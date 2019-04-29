package pl.bankoid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class OperacjeKarta extends Activity implements Runnable {

	private Thread watek;
	private String noweDane = null;
	private String tytulOkna = null;
	public static boolean odswiez = false;
	private AdView adView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.karta);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");
    	
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        // reklama
        if(Bankoid.reklamy)
        {
        	adView = new AdView(this, AdSize.BANNER, Bankoid.ADMOB_ID);
            FrameLayout layout = (FrameLayout) this.findViewById(R.id.ad);
            layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(adView);
            layout.setVisibility(View.VISIBLE);
            adView.loadAd(Bankoid.adr);
            //adView.bringToFront();     	
        }
        ///////////////////////////
        
        // automatycznie zamknij okno jezeli brak zalogowania
        if(Bankoid.zalogowano == false) this.finish();
        
        if(wybierzKarte(dane) && tytulOkna != null) this.setTitle(tytulOkna);
        // pokaz blad
        else Bankoid.bledy.pokazKomunikat(this);

    }
    
    @Override
	 protected void onResume()
	 {
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();
		super.onResume();
		if(odswiez)
		{
			odswiez = false;
			// odswiez rowniez glowne menu
			MenuGlowne.odswiez = true;
			Karty.odswiez = true;
			Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
			Bankoid.dialog.show();
			
			watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ODSWIEZANIE));
			watek.start();	
		}
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
    	 	
    		case Bankoid.ACTIVITY_HISTORIA:
    			String dane = wybierzHistorie();
	    		Intent intent = new Intent(OperacjeKarta.this, ListaHistoriaKarty.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_HISTORIA);
            	break;
            	
    		case Bankoid.ACTIVITY_KARTA_ZMIANA_SRODKOW:
    			dane = wybierzZmianaSrodkow();
	    		intent = new Intent(OperacjeKarta.this, ZmianaSrodkow.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTA_ZMIANA_SRODKOW);
            	break;
            	
    		case Bankoid.ACTIVITY_KARTA_ROZLADOWANIE:
    			dane = wybierzRozladowanie();
	    		intent = new Intent(OperacjeKarta.this, Rozladowanie.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTA_ROZLADOWANIE);
            	break;
            	
    		case Bankoid.ACTIVITY_KARTA_LIMIT_KWOTOWY:
    			dane = wybierzZmianaLimitowKwotowych();
	    		intent = new Intent(OperacjeKarta.this, ZmianaLimitowKwotowych.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTA_LIMIT_KWOTOWY);
            	break;

    		case Bankoid.ACTIVITY_KARTA_LIMIT_ILOSCIOWY:
    			dane = wybierzZmianaLimitowIlosciowych();
	    		intent = new Intent(OperacjeKarta.this, ZmianaLimitowIlosciowych.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTA_LIMIT_ILOSCIOWY);
            	break;

    		case Bankoid.ACTIVITY_KARTA_OPERACJE_BIEZACE:
    			dane = wybierzOperacjeBiezace();
	    		intent = new Intent(OperacjeKarta.this, ListaOperacjeBiezace.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTA_OPERACJE_BIEZACE);
            	break;
            	
    		case Bankoid.ACTIVITY_KARTA_SPLATA_ZADLUZENIA:
    			dane = wybierzSplataZadluzenia();
	    		intent = new Intent(OperacjeKarta.this, SplataZadluzenia.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTA_SPLATA_ZADLUZENIA);
            	break;
            	
    		case Bankoid.ODSWIEZANIE:
    			noweDane = Karty.wybierzKarte(Karty.wybrana_karta);
            	handler.sendEmptyMessage(Bankoid.ODSWIEZANIE);
            	break;
    	}
    	
    }
    
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {

    		switch(msg.what)
    		{
    			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
					 OperacjeKarta.this.finish();
					 break;

    			case Bankoid.ODSWIEZANIE:
    				wybierzKarte(noweDane);
    				noweDane = null;
    				break;
    		}

    		try
    		{
    			Bankoid.dialog.dismiss();
    		}catch(Exception e){}
    	}
    };
    
    
    // wybiera dany rachunek
    public boolean wybierzKarte(String dane)
    {
    	Pattern tytulREGEX = Pattern.compile("<div id=\"cardDetails\" class=\"details\">.+?<h3>.+?</a>(.+?)</h3>", Pattern.DOTALL);

    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.rachunek_blad, Bledy.WYLOGUJ);
    		return false;
    	}

    	Matcher m = tytulREGEX.matcher(dane);
    	// czy to szczegoly karty
	    if(m.find())
	    {
	    	tytulOkna = m.group(1).trim();
	    	
	    	odswiezOkno(dane);
	    	tworzPrzyciski(dane);
	    	return true;
	    }
    	else if(Bankoid.bledy.czyBlad())
    	{
    		// zamknij okno poniwaz nie pobrano info o karcie
    		Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    	}
    	else
    	{
    		// nieokreslony blad wyloguj
    		Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
    	}
	    return false;
    }
    
    // odswieza dane odnosnie wybranej karty
    public void odswiezOkno(String dane)
	{
		Pattern wpisyREGEX = Pattern.compile("<label class=\"label\">(.+?)</label><div class=\"content\">(.+?)</div>", Pattern.DOTALL);
		Pattern kwotaREGEX = Pattern.compile("<span[^>]+class=\"text amount\">([^<]+)</span>");
		// pola
		TextView karta_numer = (TextView) findViewById(R.id.karta_numer);
		TextView karta_rachunek = (TextView) findViewById(R.id.karta_rachunek);
		TextView karta_numer_rachunku = (TextView) findViewById(R.id.karta_numer_rachunku);
		TextView karta_numer_rachunku_label = (TextView) findViewById(R.id.karta_numer_rachunku_label);
		TextView karta_data_waznosci = (TextView) findViewById(R.id.karta_data_waznosci);
		TextView karta_nazwisko = (TextView) findViewById(R.id.karta_nazwisko);
		TextView karta_limit = (TextView) findViewById(R.id.karta_limit);
		TextView karta_limit_label = (TextView) findViewById(R.id.karta_limit_label);
		TextView karta_dostepne_srodki = (TextView) findViewById(R.id.karta_dostepne_srodki);
		TextView karta_dostepne_srodki_label = (TextView) findViewById(R.id.karta_dostepne_srodki_label);
		TextView karta_status = (TextView) findViewById(R.id.karta_status);
		
		Matcher m = wpisyREGEX.matcher(dane);
		
		while(m.find())
		{
			String nazwa = m.group(1).trim();
			String wartosc = m.group(2).trim();
			
			if(nazwa.equals("Numer karty") || nazwa.equals("Typ i numer karty"))
			{
				karta_numer.setText(wartosc);
			}
			else if(nazwa.equals("Numer rachunku powiązanego") || nazwa.equals("Wydana do rachunku"))
			{
				karta_rachunek.setText(Html.fromHtml(wartosc));
			}
			else if(nazwa.equals("Numer rachunku karty"))
			{
				karta_numer_rachunku_label.setVisibility(View.VISIBLE);
				karta_numer_rachunku.setVisibility(View.VISIBLE);
				karta_numer_rachunku.setText(Html.fromHtml(wartosc));
			}
			else if(nazwa.equals("Data ważności karty"))
			{
				karta_data_waznosci.setText(wartosc);
			}
			else if(nazwa.equals("Imię i nazwisko na karcie"))
			{
				karta_nazwisko.setText(wartosc);
			}
			else if(nazwa.equals("Limit karty"))
			{
				karta_limit_label.setVisibility(View.VISIBLE);
				karta_limit.setVisibility(View.VISIBLE);
				
				Matcher kwota = kwotaREGEX.matcher(wartosc);
				if(kwota.find()) wartosc = kwota.group(1);
				else wartosc = "Brak";
				karta_limit.setText(wartosc);
			}
			else if(nazwa.equals("Dostępne środki"))
			{
				karta_dostepne_srodki.setVisibility(View.VISIBLE);
				karta_dostepne_srodki_label.setVisibility(View.VISIBLE);
				
				Matcher kwota = kwotaREGEX.matcher(wartosc);
				if(kwota.find()) wartosc = kwota.group(1);
				else wartosc = "Brak";
				karta_dostepne_srodki.setText(wartosc);
			}
			else if(nazwa.equals("Status karty"))
			{
				karta_status.setText(wartosc);
			}
		}
	}


	public void tworzPrzyciski(String dane)
    {
    	Pattern szukajOperacje = Pattern.compile("Karty</a><ul class=\"dynamic\">.+?</ul>", Pattern.DOTALL);
    	Pattern operacjaREGEX = Pattern.compile("<li( class=\"selected\")?><a onclick[^>]+>([^<]+)</a></li>");
    	
    	// przyciski
    	Button przycisk_historia = (Button) this.findViewById(R.id.przycisk_historia);
    	Button przycisk_zmiana_srodkow = (Button) this.findViewById(R.id.przycisk_zmiana_srodkow);
    	Button przycisk_rozladowanie = (Button) this.findViewById(R.id.przycisk_rozladowanie);
    	Button przycisk_zmiana_limitow_kwotowych = (Button) this.findViewById(R.id.przycisk_zmiana_limitow_kwotowych);
       	Button przycisk_zmiana_limitow_ilosciowych = (Button) this.findViewById(R.id.przycisk_zmiana_limitow_ilosciowych);
      	Button przycisk_operacje_biezace = (Button) this.findViewById(R.id.przycisk_operacje_biezace);
      	Button przycisk_splata_zadluzenia = (Button) this.findViewById(R.id.przycisk_splata_zadluzenia);

       	// sliding
    	final SlidingDrawer sliding = (SlidingDrawer) this.findViewById(R.id.sliding);
    	
    	// aktywuje kolejne przyciski dostepne dla rachunku
    	Matcher m = szukajOperacje.matcher(dane);
    	if(m.find())
    	{
    		dane = m.group();
    		m = operacjaREGEX.matcher(dane);
    		// jezeli znaleziono operacje
    		while(m.find())
    		{
				if(m.group(2).equals("Historia operacji")) przycisk_historia.setVisibility(View.VISIBLE);
				else if(m.group(2).equals("Zmiana środków")) przycisk_zmiana_srodkow.setVisibility(View.VISIBLE);
				else if(m.group(2).equals("Rozładowanie")) przycisk_rozladowanie.setVisibility(View.VISIBLE);
				else if(m.group(2).equals("Parametry"))
				{
					przycisk_zmiana_limitow_kwotowych.setVisibility(View.VISIBLE);
					przycisk_zmiana_limitow_ilosciowych.setVisibility(View.VISIBLE);
				}
				else if(m.group(2).equals("Operacje bieżące")) przycisk_operacje_biezace.setVisibility(View.VISIBLE);
				else if(m.group(2).equals("Spłata zadłużenia")) przycisk_splata_zadluzenia.setVisibility(View.VISIBLE);
    		}
    		
    		if(przycisk_zmiana_srodkow.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku zmiana srodkow
        		przycisk_zmiana_srodkow.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_KARTA_ZMIANA_SRODKOW));
    					watek.start();	
    				}
        			
        		});
    		}

    		if(przycisk_historia.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Historia
        		przycisk_historia.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_HISTORIA));
    					watek.start();
    				}
        			
        		});
    		}

    		if(przycisk_rozladowanie.getVisibility() == View.VISIBLE)
    		{
    	   		// akcja dla przycisku Rozladowanie eKarty   		
        		przycisk_rozladowanie.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_KARTA_ROZLADOWANIE));
    					watek.start();
    				}
        			
        		});
    		}
 
    		if(przycisk_zmiana_limitow_kwotowych.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Zmiana limitow kwotowych 		
        		przycisk_zmiana_limitow_kwotowych.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_KARTA_LIMIT_KWOTOWY));
    					watek.start();
    				}
        			
        		});
    		}

    		if(przycisk_zmiana_limitow_ilosciowych.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Zmiana limitow ilosciowych 		
        		przycisk_zmiana_limitow_ilosciowych.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_KARTA_LIMIT_ILOSCIOWY));
    					watek.start();
    				}
        			
        		});
    		}

    		if(przycisk_operacje_biezace.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Operacje bieżące 		
        		przycisk_operacje_biezace.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_KARTA_OPERACJE_BIEZACE));
    					watek.start();
    				}
        			
        		});
    		}
    		
    		if(przycisk_splata_zadluzenia.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Operacje bieżące 		
    			przycisk_splata_zadluzenia.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_KARTA_SPLATA_ZADLUZENIA));
    					watek.start();
    				}
        			
        		});
    		}
    	}
    }
    
    public String wybierzZmianaSrodkow()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_load.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    public String wybierzRozladowanie()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_unload.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    public String wybierzHistorie()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	if(rozszerzenie.equals("cd")) request.setUrl("https://www.mbank.com.pl/cd_operations_list.aspx");
    	else if(rozszerzenie.equals("cv")) request.setUrl("https://www.mbank.com.pl/cv_card_operation_list.aspx");
   	    
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    // otwiera strone ze zmiana limitow dla karty
    public String wybierzZmianaLimitow()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_max_limits_list.aspx");
   	    
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }

    // otwiera operacje biezace dla karty
    public String wybierzOperacjeBiezace()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_current_operations_list.aspx");
   	    
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    // otwiera Splata zadluzenia
    public static String wybierzSplataZadluzenia()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_manual_payment.aspx");
   	    
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    public String wybierzZmianaLimitowKwotowych()
    {
    	Pattern przyciskREGEX = Pattern.compile("id=\"cardMaxAmountLimitsList\" class=\"grid\">.+?onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']*)',[^,]+,[^,]+,[^,]+,[^,]+\\);", Pattern.DOTALL);
    	String dane = wybierzZmianaLimitow();
    	
    	// jezeli udalo sie znalesc przycisk Modyfikuj
    	Matcher m = przyciskREGEX.matcher(dane);
    	if(m.find())
    	{
        	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
        	sfRequest request = sfClient.getInstance().createRequest();
        	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_change_limits.aspx");
       	    
       	    request.setMethod("POST");
       	    request.addParam("__PARAMETERS", m.group(1));
       	    request.addParam("__STATE", Bankoid.state);
       	    request.execute();
       	    
       	    String rezultat = request.getResult();
        	
       	    Bankoid.pobierzEventvalidation(rezultat);
       	    Bankoid.pobierzState(rezultat);
       	    
       	    return rezultat;
    	}
    	return null;
    }

    public String wybierzZmianaLimitowIlosciowych()
    {
    	Pattern przyciskREGEX = Pattern.compile("id=\"cardMaxNumberLimitsList\" class=\"grid\">.+?onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']*)',[^,]+,[^,]+,[^,]+,[^,]+\\);", Pattern.DOTALL);
    	String dane = wybierzZmianaLimitow();
    	
    	// jezeli udalo sie znalesc przycisk Modyfikuj
    	Matcher m = przyciskREGEX.matcher(dane);
    	if(m.find())
    	{
        	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
        	sfRequest request = sfClient.getInstance().createRequest();
        	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_change_limits.aspx");
       	    
       	    request.setMethod("POST");
       	    request.addParam("__PARAMETERS", m.group(1));
       	    request.addParam("__STATE", Bankoid.state);
       	    request.execute();
       	    
       	    String rezultat = request.getResult();
        	
       	    Bankoid.pobierzEventvalidation(rezultat);
       	    Bankoid.pobierzState(rezultat);
       	    
       	    return rezultat;
    	}
    	return null;
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
						
	                	Bankoid.tworzProgressDialog(OperacjeKarta.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(OperacjeKarta.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
    
	@Override
	protected void onDestroy() {
		if(adView != null) adView.destroy();
		super.onDestroy();
	}   

}
