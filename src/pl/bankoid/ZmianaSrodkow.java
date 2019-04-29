package pl.bankoid;

import java.util.regex.Matcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ZmianaSrodkow extends Activity implements Runnable {

	private Thread watek;
	// krok2
	private Dialog krok2;
	private String haslo_sms_label = null;
	
	// REFERENCJE DO POL FORMULARZA I ZMIENNE
	private TextView tbCardNo;
	private TextView maCardLimit;
	private TextView maAvailableFunds;
	private TextView maAccountAvailableFunds;
	private EditText maCardLimitNew;
	private String authTurnOff;
	private String TransactionType;
	private String maCardLimit_Curr;
	private String maAvailableFunds_Curr;
	private String maAccountAvailableFunds_Curr;
	
	// parametr dla przyciskow
	private String paramDalej;
	private String paramZatwierdz;
	private String paramModyfikuj;
	private String paramPotwierdzPozniej;
	private String paramPowrot;
	

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.zmiana_srodkow);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");

        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();

        this.setTitle("Ładowanie/rozładowanie eKARTY");
        
        // inicjalizacja zmiennych
        tbCardNo = (TextView) this.findViewById(R.id.numer_karty);
        maCardLimit = (TextView) this.findViewById(R.id.limit_karty);
        maAvailableFunds = (TextView) this.findViewById(R.id.dostepne_srodki);
        maAccountAvailableFunds = (TextView) this.findViewById(R.id.dostepne_srodki_rachunek);
        maCardLimitNew = (EditText) this.findViewById(R.id.kwota);
        maCardLimitNew.setFilters(new InputFilter[]{new MoneyValueFilter(), new InputFilter.LengthFilter(15)});

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

    		case Przelewy.DALEJ_KROK1:
    			// wysylanie przelewu
    			String dane = wykonajPrzelewKrok1();
				Message msg = handler.obtainMessage();
				msg.what = Przelewy.DALEJ_KROK1;
				Bundle b = new Bundle();
				b.putString("dane", dane);
                msg.setData(b);
                handler.sendMessage(msg);
            	break;
            			
    		case Przelewy.MODYFIKUJ_KROK2:
    			// modyfikacja przelewu
    			modyfikujPrzelew();
    			handler.sendEmptyMessage(Przelewy.MODYFIKUJ_KROK2);
    			break;
    			
    		case Przelewy.ZATWIERDZ_POZNIEJ_KROK2:
    			// potwierdz pozniej przelew
    			potwierdzPozniejPrzelew();
    			handler.sendEmptyMessage(Przelewy.ZATWIERDZ_POZNIEJ_KROK2);
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
					 ZmianaSrodkow.this.finish();
					 break;
    		
    			 case Przelewy.DALEJ_KROK1:
    				 // czy wystapil blad np nieprawidlowy nr konta
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ZmianaSrodkow.this);
    				// otworz formularz z haslem potwierdzajacym przelew
    				 else
    				 {
    					 String dane = msg.getData().getString("dane");
    					 if(dane != null)
    					 {
    						 tworzFormularzKrok2(dane);
    						 krok2.show();
    					 }
    					 
    				 }
    				 break;
    			
    			 case Przelewy.MODYFIKUJ_KROK2:
    				 // jezeli blad to pokaz inaczej zamknij okno dialogowe
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ZmianaSrodkow.this);
    				 else krok2.dismiss();
    				 break;
    				 
    			 case Przelewy.ZATWIERDZ_POZNIEJ_KROK2:
    				 // jezeli blad to pokaz inaczej przejdz do szczegoly rachunku
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ZmianaSrodkow.this);
    				 else
    				 {
    					 krok2.dismiss();
    					 ZmianaSrodkow.this.finish();
    				 }
    				 break;
    				
    			 case Przelewy.ZATWIERDZ_KROK2:
    				 // wyczyszczenie pola z haslem i ustawienie haslo label
    				 if(krok2.isShowing())
    				 {
    					 if(haslo_sms_label != null) ((TextView) krok2.findViewById(R.id.haslo_sms_label)).setText(haslo_sms_label);
    					 //((EditText) krok2.findViewById(R.id.haslo_sms)).setText("");
    				 }
    				 
    				 // pokaz blad zawierajacy czy przelew zostal zrealizowany
    				 Bankoid.bledy.pokazKomunikat(ZmianaSrodkow.this);
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
  	  	
    	Button powrot = (Button) this.findViewById(R.id.przycisk_powrot);
    	Button dalej = (Button) this.findViewById(R.id.przycisk_dalej);
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
    		return false;
    	}

    	// szukanie parametru dal przycisku dalej
    	Matcher m = Przelewy.dalejREGEX.matcher(dane);
    	if(m.find()) 
    	{
    		paramDalej = m.group(1);
    		
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
        	
    	    // szukanie srodkow na rachunku waluta
    	    m = Karty.maAccountAvailableFunds_Curr_REGEX.matcher(dane);
    	    if(m.find()) maAccountAvailableFunds_Curr = m.group(1);
    	    
        	// szukanie srodkow na rachunku
    	    m = Karty.maAccountAvailableFunds_REGEX.matcher(dane);
    	    if(m.find()) maAccountAvailableFunds.setText(m.group(1) + " " + maAccountAvailableFunds_Curr);
        	
        	// szukanie authTurnOff
        	m = Przelewy.authTurnOff_REGEX.matcher(dane);
        	if(m.find()) authTurnOff = m.group(1);
        	
        	// szukanie TransactionType
        	m = Przelewy.TransactionType_REGEX.matcher(dane);
        	if(m.find()) TransactionType = m.group(1);
        	
        	// dodanie sluchaczy na przyciski dalej i powrot
        	powrot.setOnClickListener(new OnClickListener()
        	{
    			@Override
    			public void onClick(View arg0) {
    				finish();
    				
    			}
        	});
        	
        	dalej.setOnClickListener(new OnClickListener()
        	{
    			@Override
    			public void onClick(View arg0) {
    				// jezeli dane zostaly wprowadzone poprawnie wykonaj przelew
    				if(sprawdzPoprawnoscDanych())
    				{
    					Bankoid.tworzProgressDialog(ZmianaSrodkow.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(ZmianaSrodkow.this, String.valueOf(Przelewy.DALEJ_KROK1));
    					watek.start();
    				}
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
    
    private boolean sprawdzPoprawnoscDanych()
	{

    	String nowaKwotaString = maCardLimitNew.getText().toString();
		// sprawdzenie tytulu przelewu
		if(nowaKwotaString.length() == 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.karty_blad_kwota, Toast.LENGTH_LONG).show();
			return false;
		}
		else
		{
			float nowaKwota = 0;
			float kartaLimit = 0;
			float kartaSrodki = 0;
			float kontoSrodki = 0;
			try
			{
				nowaKwota = Float.valueOf(nowaKwotaString);
				kartaLimit = Float.valueOf(maCardLimit.getText().toString().replace(" "+maCardLimit_Curr, "").replace(",", "."));
				kartaSrodki = Float.valueOf(maAvailableFunds.getText().toString().replace(" "+maAvailableFunds_Curr, "").replace(",", "."));
				kontoSrodki = Float.valueOf(maAccountAvailableFunds.getText().toString().replace(" "+maAccountAvailableFunds_Curr, "").replace(",", "."));
			}catch(NumberFormatException e) {}
			
			/*Log.v("nowa", ""+nowaKwota);
			Log.v("kartaLimit", ""+kartaLimit);
			Log.v("kartaSrodki", ""+kartaSrodki);
			Log.v("kontoSrodki", ""+kontoSrodki);*/
			if(nowaKwota > kartaLimit)
			{
				Toast.makeText(this.getApplicationContext(), R.string.karty_blad_limit, Toast.LENGTH_LONG).show();
				return false;
			}
			if(nowaKwota == kartaSrodki)
			{
				Toast.makeText(this.getApplicationContext(), R.string.karty_blad_kwota_dostepna, Toast.LENGTH_LONG).show();
				return false;
			}
			if(nowaKwota > kontoSrodki)
			{
				Toast.makeText(this.getApplicationContext(), R.string.karty_blad_brak_srodkow, Toast.LENGTH_LONG).show();
				return false;
			}
		}

		
		return true;
	}

    // zwraca false jezeli nalezy przeladowac
	private String wykonajPrzelewKrok1()
	{
		String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_load.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramDalej);
	    request.addParam("__CurrentWizardStep", "1");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    
	    request.addParam("tbCardNo", tbCardNo.getText().toString());
	    request.addParam("maCardLimit", maCardLimit.getText().toString().replace(" "+maCardLimit_Curr, ""));
	    request.addParam("maCardLimit_Curr", maCardLimit_Curr);
	    request.addParam("maAvailableFunds", maAvailableFunds.getText().toString().replace(" "+maAvailableFunds_Curr, ""));
	    request.addParam("maAvailableFunds_Curr", maAvailableFunds_Curr);
	    request.addParam("maAccountAvailableFunds", maAccountAvailableFunds.getText().toString().replace(" "+maAccountAvailableFunds_Curr, ""));
	    request.addParam("maAccountAvailableFunds_Curr", maAccountAvailableFunds_Curr);
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("maCardLimitNew", maCardLimitNew.getText().toString().replace(".", ","));
	    
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
	    // udalo sie przejsc do kroku 2 zwroc true
	    Matcher m = Przelewy.zatwierdzREGEX.matcher(rezultat);
	    if(m.find())
	    {
	    	paramZatwierdz = m.group(1);
	    	//tworzFormularzKrok2(rezultat);
	    	return rezultat;
	    }
	    // szukanie bledu
	    else if(Bankoid.bledy.czyBlad() == false)
	    {
	    	// jezeli odczytano blad to zamknij formularz
		    m = Przelewy.bladPrzelewREGEX.matcher(rezultat);
		    if(m.find())
		    {
		    	String bladTresc = Html.fromHtml(m.group(2).replace("&shy;<wbr />", "")).toString();
		    	Bankoid.bledy.ustawBlad(m.group(1).replace("&shy;<wbr />", "").trim(), bladTresc, Bledy.ZAMKNIJ_OKNO);

		    }
		    // niestandartowy blad
		    else
		    {
		    	Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
		    }
	    }
	    return null;
	}

	// tworzy i pokazuje formularz z kodem sms
	private void tworzFormularzKrok2(String dane)
	{
    	// tworzenie dialogu z formularzem dla kroku 2
    	krok2 = new Dialog(this, R.style.CustomTheme);
		krok2.setContentView(R.layout.dialog_krok2_karta);
		krok2.setTitle("Ładowanie/rozładowanie eKARTY");

        krok2.getWindow().setFormat(PixelFormat.RGBA_8888);
        krok2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

		//////////////////////////////////////////////
	
		TextView nr_karty = (TextView) krok2.findViewById(R.id.nr_karty);
		TextView limit_karty = (TextView) krok2.findViewById(R.id.limit_karty);
		TextView dostepne_srodki = (TextView) krok2.findViewById(R.id.dostepne_srodki);
		TextView nowa_kwota = (TextView) krok2.findViewById(R.id.nowa_kwota);
		TextView haslo_sms_label = (TextView) krok2.findViewById(R.id.haslo_sms_label);
		TextView tresc_stopka = (TextView) krok2.findViewById(R.id.tresc_stopka);
		final EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
		Button przycisk_powrot = (Button) krok2.findViewById(R.id.przycisk_powrot);
		Button przycisk_modyfikuj = (Button) krok2.findViewById(R.id.przycisk_modyfikuj);
		Button przycisk_zatwierdz = (Button) krok2.findViewById(R.id.przycisk_zatwierdz);
		Button przycisk_potwierdz_pozniej = (Button) krok2.findViewById(R.id.przycisk_potwierdz_pozniej);
		//////////////////////////////////////////////
		
		// szukanie tresci label sms
		Matcher m = Przelewy.smslabelREGEX.matcher(dane);
		if(m.find())
		{
			// jezeli wersja bez reklam
			if(Bankoid.pref_sms)
				registerReceiver(new SMSReceiver(m.group(1), haslo_sms), new IntentFilter(SMSReceiver.SMS_RECEIVED));

			haslo_sms_label.setText(Html.fromHtml(m.group(1)).toString().trim());
		}
		
		// szukanie tresci label sms stopka
		m = Przelewy.smslabelstopkaREGEX.matcher(dane);
		if(m.find())
		{
			tresc_stopka.setText(Html.fromHtml(m.group(1)).toString().trim());
			tresc_stopka.setVisibility(View.VISIBLE);
		}else tresc_stopka.setVisibility(View.GONE);

		// szukanie TransactionType
		m = Przelewy.TransactionType_REGEX.matcher(dane);
		if(m.find()) TransactionType = m.group(1);

		// szukanie parameters modyfikuj
		m = Przelewy.modyfikujREGEX.matcher(dane);
		if(m.find()) paramModyfikuj = m.group(1);
		
		// szukanie parameters potwierdz pozniej
		m = Przelewy.potwierdzpozniejREGEX.matcher(dane);
		if(m.find())
		{
			paramPotwierdzPozniej = m.group(1);
			przycisk_potwierdz_pozniej.setVisibility(View.VISIBLE);
			haslo_sms_label.setVisibility(View.VISIBLE);
			haslo_sms.setVisibility(View.VISIBLE);
			tresc_stopka.setVisibility(View.VISIBLE);
		}
		// jezeli nie znaleziono tzn ze nie potrzeba autoryzacji na kod sms
		else
		{
			przycisk_potwierdz_pozniej.setVisibility(View.GONE);
			haslo_sms_label.setVisibility(View.GONE);
			haslo_sms.setVisibility(View.GONE);
			tresc_stopka.setVisibility(View.GONE);
		}
		
		nr_karty.setText(tbCardNo.getText());
		limit_karty.setText(maCardLimit.getText());
		dostepne_srodki.setText(maAvailableFunds.getText());
		nowa_kwota.setText(maCardLimitNew.getText().toString().replace(".", ","));
	
		// w przypadku anulowania powrot do OdbiorcyZdefiniowani
		krok2.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface krok2) {
				krok2.dismiss();
				ZmianaSrodkow.this.finish();
			}
			
		});
		
		// akcja dla przycisku powrot
		przycisk_powrot.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				krok2.cancel();
			}
		});
	
		// akcja dla przycisku modyfikuj
		przycisk_modyfikuj.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				Bankoid.tworzProgressDialog(ZmianaSrodkow.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(ZmianaSrodkow.this, String.valueOf(Przelewy.MODYFIKUJ_KROK2));
				watek.start();
			}
		});

		// akcja dla przycisku zatwierdz
		przycisk_zatwierdz.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				if(haslo_sms.isShown() && haslo_sms.getText().length() < 5)
				{
					Toast.makeText(ZmianaSrodkow.this, R.string.przelewy_blad_haslo, Toast.LENGTH_LONG).show();						
				}
				else
				{
					Bankoid.tworzProgressDialog(ZmianaSrodkow.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
					
					watek = new Thread(ZmianaSrodkow.this, String.valueOf(Przelewy.ZATWIERDZ_KROK2));
					watek.start();
				}
			}
		});

		// akcja dla przycisku zatwierdz pozniej
		przycisk_potwierdz_pozniej.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				Bankoid.tworzProgressDialog(ZmianaSrodkow.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(ZmianaSrodkow.this, String.valueOf(Przelewy.ZATWIERDZ_POZNIEJ_KROK2));
				watek.start();
			}
		});
	}
	
	// metoda wywolywana po kliknieciu modyfikuj
    private boolean modyfikujPrzelew()
    {    	
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_load.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("maAccountAvailableFunds", maAccountAvailableFunds.getText().toString().replace(" "+maAccountAvailableFunds_Curr, ""));
	    request.addParam("maAccountAvailableFunds_Curr", maAccountAvailableFunds_Curr);
	    request.addParam("__PARAMETERS", paramModyfikuj);
	    request.addParam("__CurrentWizardStep", "2");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("tbCardNo", tbCardNo.getText().toString());
	    request.addParam("maCardLimit", maCardLimit.getText().toString().replace(" "+maCardLimit_Curr, ""));
	    request.addParam("maCardLimit_Curr", maCardLimit_Curr);
	    request.addParam("maAvailableFunds", maAvailableFunds.getText().toString().replace(" "+maAvailableFunds_Curr, ""));
	    request.addParam("maAvailableFunds_Curr", maAvailableFunds_Curr);
	    request.addParam("maCardLimitNew", maCardLimitNew.getText().toString().replace(".", ","));
	    request.addParam("maCardLimitNew_Curr", "");
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("authCode", "");
	    
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
    	// szukanie parametrow przycisku dalej
	    Matcher m = Przelewy.dalejREGEX.matcher(rezultat);
   	    // jezeli strona otworzyla sie prawidlowo
	    if(m.find()) 
   	    {
   	    	paramDalej = m.group(1);
   	    	return true;
   	    }
   	    // nieokreslony blad
   	    else if(Bankoid.bledy.czyBlad() == false)
   	    {
   	    	Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
	// metoda wywolywana po kliknieciu potwierdz pozniej
    private boolean potwierdzPozniejPrzelew()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_details.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", this.paramPotwierdzPozniej);
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
    	// sprawdzenie czy udalo sie wrocic do szczegolow karty	
	    if(rezultat.contains("Status karty")) return true;
   	    else if(Bankoid.bledy.czyBlad() == false)
   	    {
   	    	Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
	// metoda wywolywana po kliknieciu zatwierdz
    private void wykonajPrzelewKrok2()
    {
		EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
    	
		String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_load.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("maAccountAvailableFunds", maAccountAvailableFunds.getText().toString().replace(" "+maAccountAvailableFunds_Curr, ""));
	    request.addParam("maAccountAvailableFunds_Curr", maAccountAvailableFunds_Curr);
	    request.addParam("__PARAMETERS", paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "2");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("tbCardNo", tbCardNo.getText().toString());
	    request.addParam("maCardLimit", maCardLimit.getText().toString().replace(" "+maCardLimit_Curr, ""));
	    request.addParam("maCardLimit_Curr", maCardLimit_Curr);
	    request.addParam("maAvailableFunds", maAvailableFunds.getText().toString().replace(" "+maAvailableFunds_Curr, ""));
	    request.addParam("maAvailableFunds_Curr", maAvailableFunds_Curr);
	    request.addParam("maCardLimitNew", maCardLimitNew.getText().toString().replace(".", ","));
	    request.addParam("maCardLimitNew_Curr", "");
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("authCode", haslo_sms.getText().toString());
	    
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
   	    		Bankoid.bledy.ustawBlad(m.group(1).replace("&shy;<wbr />", "").trim(), m.group(2).trim(), Bledy.ZAMKNIJ_OKNO);
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
	   	    		Bankoid.bledy.ustawBlad(m.group(1).replace("&shy;<wbr />", "").trim(), bladTresc, Bledy.POWROT);
	   	    		Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	   	    		// zaladuj ponownie formularz umozliwiajac wpisanie poprawnego hasla
	   	    		// jezeli nie udalo sie zaladowac ponownie formularza to zamknij przelewy
	   	    		if(ponowHaslo(rezultat) == false) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
	   	    	}
	   	    	else
	   	    	{
	   	    		// niestandartowy blad
	   	    		Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
	   	    	}
   	    	}
   	    }
    }
    
    // laduje ponownie formularz z krokiem 2 umozliwiajac ponowienie hasla
    private boolean ponowHaslo(String dane)
    {
    	
    	Matcher m = Przelewy.powrotREGEX.matcher(dane);
    	if(m.find()) paramPowrot = m.group(1);
    	
    	// szukanie authTurnOff
    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
    	if(m.find()) authTurnOff = m.group(1);
    	
		String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_load.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("tbCardNo", tbCardNo.getText().toString());
	    request.addParam("maCardLimit", maCardLimit.getText().toString().replace(" "+maCardLimit_Curr, ""));
	    request.addParam("maCardLimit_Curr", maCardLimit_Curr);
	    request.addParam("maCardLimitNew", maCardLimitNew.getText().toString().replace(".", ","));
	    request.addParam("maCardLimitNew_Curr", "");
	    request.addParam("maAvailableFunds", maAvailableFunds.getText().toString().replace(" "+maAvailableFunds_Curr, ""));
	    request.addParam("maAvailableFunds_Curr", maAvailableFunds_Curr);
	    request.addParam("maAccountAvailableFunds", maAccountAvailableFunds.getText().toString().replace(" "+maAccountAvailableFunds_Curr, ""));
	    request.addParam("maAccountAvailableFunds_Curr", maAccountAvailableFunds_Curr);
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramPowrot);
	    request.addParam("__CurrentWizardStep", "3");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    
	    request.execute();

	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    
		// szukanie parameters zatwierdz
		m = Przelewy.zatwierdzREGEX.matcher(rezultat);
	    // jezeli zatwierdz istnieje to udalo sie przejsc do kroku 2 zwroc true
		if(m.find())
	    {
			paramZatwierdz = m.group(1);
			
			// szukanie tresci label sms
			m = Przelewy.smslabelREGEX.matcher(rezultat);
			if(m.find()) haslo_sms_label = Html.fromHtml(m.group(1)).toString().trim();

			// szukanie parameters potwierdz pozniej
			m = Przelewy.potwierdzpozniejREGEX.matcher(rezultat);
			if(m.find()) paramPotwierdzPozniej = m.group(1);

			// szukanie parameters modyfikuj
			m = Przelewy.modyfikujREGEX.matcher(rezultat);
			if(m.find()) paramModyfikuj = m.group(1);

			return true;
	    }
	    else return false;
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
						
	                	Bankoid.tworzProgressDialog(ZmianaSrodkow.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(ZmianaSrodkow.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
