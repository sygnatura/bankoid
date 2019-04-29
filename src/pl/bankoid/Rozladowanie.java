package pl.bankoid;

import java.util.regex.Matcher;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class Rozladowanie extends Activity implements Runnable {

	private Thread watek;
	
	// REFERENCJE DO POL FORMULARZA I ZMIENNE
	private TextView tbCardNo;
	private TextView maCardLimit;
	private TextView maAvailableFunds;
	private String maCardLimit_Curr;
	private String maAvailableFunds_Curr;
	private String authTurnOff;
	
	// parametr dla przyciskow
	private String paramZatwierdz;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.rozladowanie);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");

        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();

        this.setTitle("Szybkie rozładowanie eKARTY");
        
        // inicjalizacja zmiennych
        tbCardNo = (TextView) this.findViewById(R.id.numer_karty);
        maCardLimit = (TextView) this.findViewById(R.id.limit_karty);
        maAvailableFunds = (TextView) this.findViewById(R.id.dostepne_srodki);
 
        // jezeli otwieranie formularza z przelewem nie powiodlo sie
        if(tworzFormularzKrok1(dane) == false) Bankoid.bledy.pokazKomunikat(this);
    }

    @Override
	protected void onResume()
	{
    	// automatycznie zamknij okno jezeli nie jest sie zalogowanym
    	if(Bankoid.zalogowano == false) this.finish();
		super.onResume();
	}
    
	@Override 
    public void run()
    {
    	switch(Integer.valueOf(watek.getName()))
    	{
			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
				Bankoid.wyloguj();
	        	handler.sendEmptyMessage(Bankoid.ACTIVITY_WYLOGOWYWANIE);
	        	break;

 
    		case Przelewy.ZATWIERDZ_KROK2:
    			wykonajPrzelewKrok2();
    			handler.sendEmptyMessage(Przelewy.ZATWIERDZ_KROK2);
    			break;
    	}
    	
    }
    
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {

    		switch(msg.what)
    		{
				 case Bankoid.ACTIVITY_WYLOGOWYWANIE:
					 Rozladowanie.this.finish();
					 break;
    		
    			 case Przelewy.ZATWIERDZ_KROK2:
    				 // pokaz blad zawierajacy czy przelew zostal zrealizowany
    				 Bankoid.bledy.pokazKomunikat(Rozladowanie.this);
    				 break;
    		}
    		
    		// zakmniecie dialogu
    		try
    		{
    			Bankoid.dialog.dismiss();
    		}catch(Exception e){}
    	}
    };
    
    public boolean tworzFormularzKrok1(String dane)
    {	
    	Button przycisk_powrot = (Button) this.findViewById(R.id.przycisk_powrot);
    	Button przycisk_zatwierdz = (Button) this.findViewById(R.id.przycisk_zatwierdz);
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
    		return false;
    	}

    	// szukanie parametru dal przycisku dalej
    	Matcher m = Przelewy.zatwierdzREGEX.matcher(dane);
    	if(m.find()) 
    	{
    		paramZatwierdz = m.group(1);
    		
        	// szukanie nr karty
        	m = Karty.tbCardNo_REGEX.matcher(dane);
        	if(m.find()) tbCardNo.setText(m.group(1));
        		
        	// szukanie limitu karty waluta
        	m = Karty.maCardLimit_Curr_REGEX.matcher(dane);
        	if(m.find()) maCardLimit_Curr = m.group(1);
        	
        	// szukanie limitu karty
        	m = Karty.maCardLimit_REGEX.matcher(dane);
        	if(m.find()) maCardLimit.setText(m.group(1)  + " " + maCardLimit_Curr); 	
        	
        	// szukanie waluty srodkow
        	m = Karty.maAvailableFunds_Curr_REGEX.matcher(dane);
        	if(m.find()) maAvailableFunds_Curr = m.group(1);
        	
        	// szukanie srodkow na karcie
        	m = Karty.maAvailableFunds_REGEX.matcher(dane);
        	if(m.find()) maAvailableFunds.setText(m.group(1) + " " + maAvailableFunds_Curr);
        	      	
        	// szukanie authTurnOff
        	m = Przelewy.authTurnOff_REGEX.matcher(dane);
        	if(m.find()) authTurnOff = m.group(1);
    		
        	// dodanie sluchaczy na przyciski dalej i powrot
        	przycisk_powrot.setOnClickListener(new OnClickListener()
        	{
    			@Override
    			public void onClick(View arg0) {
    				finish();
    				
    			}
        	});
        	
        	przycisk_zatwierdz.setOnClickListener(new OnClickListener()
        	{
    			@Override
    			public void onClick(View arg0) {
    				Bankoid.tworzProgressDialog(Rozladowanie.this, getResources().getString(R.string.dialog_pobinfo));
    				Bankoid.dialog.show();
    					
    				watek = new Thread(Rozladowanie.this, String.valueOf(Przelewy.ZATWIERDZ_KROK2));
    				watek.start();
    			}
        	});
        	return true;
    	}
   	    // czy wystapil standartowy blad
   	    else if(Bankoid.bledy.czyBlad())
   	    {
   	    	Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
   	    }
   	    else
   	    {
   	    	// czy jest komunikat z bledem
   	    	m = Przelewy.bladPrzelewREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
		    	String bladTresc = Html.fromHtml(m.group(2)).toString();
		    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.ZAMKNIJ_OKNO);
		    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
   	    	}
	   	    // niestandartowy blad
	   	    else Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
   	    }

   	    return false;
    }
    
	// metoda wywolywana po kliknieciu zatwierdz
    private void wykonajPrzelewKrok2()
    {
    	
		String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_unload.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("maCardLimitNew", "");
	    request.addParam("maCardLimitNew_Curr", "");
	    request.addParam("maAccountAvailableFunds", "");
	    request.addParam("maAccountAvailableFunds_Curr", "");
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "1");
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("tbCardNo", tbCardNo.getText().toString());
	    request.addParam("maCardLimit", maCardLimit.getText().toString().replace(" "+maCardLimit_Curr, ""));
	    request.addParam("maCardLimit_Curr", maCardLimit_Curr);
	    request.addParam("maAvailableFunds", maAvailableFunds.getText().toString().replace(" "+maAvailableFunds_Curr, ""));
	    request.addParam("maAvailableFunds_Curr", maAvailableFunds_Curr);
	    
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);

	    // jezeli nie ma standartowego bledu
   	    if(Bankoid.bledy.czyBlad() == false)
   	    {
	    	// szukanie komunikatu potwierdzajacego przelew
   	    	Matcher m = Przelewy.komunikatREGEX.matcher(rezultat);
   	    	if(m.find())
   	    	{
   	    		OperacjeKarta.odswiez = true;
   	    		Bankoid.bledy.ustawBlad(m.group(1).trim(), m.group(2).trim(), Bledy.ZAMKNIJ_OKNO);
   	    		Bankoid.bledy.ustawKolorTresc(this.getResources().getColor(R.color.ok));
   	    		Bankoid.bledy.ustawIkone(android.R.drawable.ic_dialog_info);
   	    	}
   	    	else
   	    	{
	   	    	// szukanie komunikatu o bledzie (nieprawidlowe haslo)
	   	    	m = Przelewy.bladPrzelewREGEX.matcher(rezultat);
	   	    	if(m.find())
	   	    	{
	   	    		String bladTresc = Html.fromHtml(m.group(2)).toString();
	   	    		Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.POWROT);
	   	    		Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	   	    	}
	   	    	else
	   	    	{
	   	    		// niestandartowy blad
	   	    		Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
	   	    	}
   	    	}
   	    }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	// wylaczenie opcji reklamy jezeli sa wylaczone
        //MenuItem wylacz_reklamy = menu.findItem(R.id.wylacz_reklamy);
        MenuItem oprogramie = menu.findItem(R.id.oprogramie);
        //wylacz_reklamy.setVisible(false);
        oprogramie.setVisible(false);
        
        return super.onPrepareOptionsMenu(menu);
    }
    
    // wybrano opcje z glownego menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
	        case R.id.wyloguj:
	        	final Komunikat k = new Komunikat(this);
	        	k.ustawIkone(android.R.drawable.ic_menu_help);
	        	k.ustawTytul("Zamykanie");
	        	k.ustawTresc(R.string.pytanie_wyloguj);
	        	k.ustawPrzyciskTak("Tak", new View.OnClickListener() { 
	     
					@Override
					public void onClick(View v) {
						
	                	Bankoid.tworzProgressDialog(Rozladowanie.this, getResources().getString(R.string.dialog_wylogowywanie));
	                    // w przypadku anulowania wylogowania zamknij program
	                	Bankoid.dialog.setOnCancelListener(new OnCancelListener()
	                    {
	            			@Override
	            			public void onCancel(DialogInterface dialog) {
	            				finish();
	            			}
	                    });
	                	Bankoid.dialog.setCancelable(true);
	                	Bankoid.dialog.show();
	                	
	                	watek = new Thread(Rozladowanie.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
	                	watek.start();
	                	
	                	k.dismiss();
					} 
	     
	            });
	        	k.ustawPrzyciskNie("Nie", null);
	        	k.show();
	        	return true;
	        	
	        default:
	            return super.onOptionsItemSelected(item);
       	}
    }
}
