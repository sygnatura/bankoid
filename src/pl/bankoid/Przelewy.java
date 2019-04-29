package pl.bankoid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdSize;
import com.google.ads.AdView;

	public class Przelewy extends Activity implements Runnable
	{
		private final int RACHUNEK_DLUGOSC = 26;
		private AdView adView;
		
		// akcje
		final static int DALEJ_KROK1 = 1;
		final static int MODYFIKUJ_KROK2 = 2;
		final static int ZATWIERDZ_POZNIEJ_KROK2 = 3;
		final static int ZATWIERDZ_KROK2 = 4;
		final static int ODSWIEZANIE = 5;
		
		private boolean przeladuj = false;
		
		private ContentValues waluty_typy = new ContentValues();
		private Thread watek;
		// krok2
		private Dialog krok2;
		private String haslo_sms_label = null;
		
		// REGEX
		// przyciski
		public static Pattern dalejREGEX = Pattern.compile("id=\"Forward\" onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']*)',[^,]+,[^,]+,[^,]+,[^,]+\\)");
		public static Pattern potwierdzpozniejREGEX = Pattern.compile("id=\"AuthLater\" onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']*)',[^,]+,[^,]+,[^,]+,[^,]+\\);");
		public static Pattern zatwierdzREGEX = Pattern.compile("id=\"Confirm\" onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']*)',[^,]+,[^,]+,[^,]+,[^,]+\\);");
		public static Pattern modyfikujREGEX = Pattern.compile("id=\"Modify\" onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']*)',[^,]+,[^,]+,[^,]+,[^,]+\\);");
		public static Pattern powrotREGEX = Pattern.compile("id=\"Back\" onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']*)',[^,]+,[^,]+,[^,]+,[^,]+\\);");
		
		// blad przelewu
		public static Pattern bladPrzelewREGEX = Pattern.compile("<div id=\"errorView\" class=\"error\">.+?<h3>(.+?)</h3><fieldset>.+?<p class=\"message\">(.+?)</p>", Pattern.DOTALL);
		// prawidlowe wykonanie przelewu
    	public static Pattern komunikatREGEX = Pattern.compile("<div id=\"msg\" class=\"info\">.+?<h3>.+?<a.+?<span></span></a>(.+?)</h3><fieldset>.+?<p class=\"message\">(.+?)</p>", Pattern.DOTALL);
		public static Pattern smslabelREGEX = Pattern.compile("<label class=\"label\">(Wprowadź.+?|Hasło.+?)</label><div class=\"content\">.+?<input name=\"authCode\"", Pattern.DOTALL);
		public static Pattern smslabelstopkaREGEX = Pattern.compile("<p class=\"message\">(.+?)</p>.+?</fieldset><fieldset class=\"footer\">", Pattern.DOTALL);
		public static Pattern authTurnOff_REGEX = Pattern.compile("id=\"authTurnOff\" value=\"([^\"]+)\" />");
		public static Pattern TransactionType_REGEX = Pattern.compile("id=\"TransactionType\" value=\"([^\"]+)\" />");
		
		// dtbTransferDate
		public static Pattern dtbTransferDate_year_REGEX = Pattern.compile("id=\"dtbTransferDate_year\" value=\"(\\d+)\" />");
		public static Pattern dtbTransferDate_month_REGEX = Pattern.compile("id=\"dtbTransferDate_month\" value=\"(\\d+)\" />");
		public static Pattern dtbTransferDate_day_REGEX = Pattern.compile("id=\"dtbTransferDate_day\" value=\"(\\d+)\" />");
		// daterange_from
		public static Pattern daterange_from_year_REGEX = Pattern.compile("id=\"daterange_from_year\" value=\"(\\d+)\" />");
		public static Pattern daterange_from_month_REGEX = Pattern.compile("id=\"daterange_from_month\" value=\"(\\d+)\" />");
		public static Pattern daterange_from_day_REGEX = Pattern.compile("id=\"daterange_from_day\" value=\"(\\d+)\" />");
		// daterange_to
		public static Pattern daterange_to_year_REGEX = Pattern.compile("id=\"daterange_to_year\" value=\"(\\d+)\" />");
		public static Pattern daterange_to_month_REGEX = Pattern.compile("id=\"daterange_to_month\" value=\"(\\d+)\" />");
		public static Pattern daterange_to_day_REGEX = Pattern.compile("id=\"daterange_to_day\" value=\"(\\d+)\" />");
		
		// parametr dla przyciskow
		private String paramDalej;
		private String paramZatwierdz;
		private String paramPotwierdzPozniej;
		private String paramModyfikuj;
		
		// REFERENCJE DO POL FORMULARZA I ZMIENNE
    	private TextView TransferType;
    	private EditText tbReceiverAccNo;
    	private EditText tbTransferTitle;
    	private EditText tbRecName;
    	private EditText tbRecAddress;
    	private EditText tbRecCity;
    	private DatePicker dtbTransferDate;
    	private TextView SenderName;
    	private TextView SenderAddress;
    	private TextView SenderCity;
    	private TextView abInfo;
    	private String abInfo_curr;
    	private EditText tbAmount;
    	private Spinner ddlTransferCurrSpinner;
    	private String ddlTransferCurr = "";
    	private String TransactionType = "";
    	private String dtbTransferDate_year = "";
    	private String dtbTransferDate_month = "";
    	private String dtbTransferDate_day = "";
    	private String daterange_from_year = "";
    	private String daterange_from_month = "";
    	private String daterange_from_day = "";
    	private String daterange_to_year = "";
    	private String daterange_to_month = "";
    	private String daterange_to_day = "";
    	private String authTurnOff = "";
    	// kalendarz z dzisiejsza data
    	//private Calendar kalendarz = Calendar.getInstance();
		
	    @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);
	    	setContentView(R.layout.przelewy);
	    	Bundle extras = getIntent().getExtras();
	    	String dane = extras.getString("dane");
	    	
	        getWindow().setFormat(PixelFormat.RGBA_8888);
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
	    	
	        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
	        if(Bankoid.zalogowano == false) this.finish();
	        
	        // reklama
	        if(Bankoid.reklamy)
	        {
	        	adView = new AdView(this, AdSize.BANNER, Bankoid.ADMOB_ID);
	            FrameLayout layout = (FrameLayout) this.findViewById(R.id.ad);
	            layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	            layout.addView(adView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	            layout.setVisibility(View.VISIBLE);
	            adView.loadAd(Bankoid.adr);
	            //adView.bringToFront();	
	        }
	        ////////////////////

	        
	        // inicjalizacja zmiennych
	        TransferType = (TextView) this.findViewById(R.id.typ_przelewu);
	    	tbReceiverAccNo = (EditText) this.findViewById(R.id.rachunek_odbiorcy);
	    	tbTransferTitle = (EditText) this.findViewById(R.id.tytul_przelewu);
	    	tbRecName = (EditText) this.findViewById(R.id.nazwisko_odbiorcy);
	    	tbRecAddress = (EditText) this.findViewById(R.id.adres_odbiorcy);
	    	tbRecCity = (EditText) this.findViewById(R.id.miejscowosc_odbiorcy);
	    	dtbTransferDate = (DatePicker) this.findViewById(R.id.data_operacji);
	    	SenderName = (TextView) this.findViewById(R.id.nazwisko_nadawcy);
	    	SenderAddress = (TextView) this.findViewById(R.id.adres_nadawcy);
	    	SenderCity = (TextView) this.findViewById(R.id.miejscowosc_nadawcy);
	    	abInfo = (TextView) this.findViewById(R.id.dostepne_srodki);
	    	tbAmount = (EditText) this.findViewById(R.id.kwota_przelewu);
	    	tbAmount.setFilters(new InputFilter[]{new MoneyValueFilter(), new InputFilter.LengthFilter(15)});
	    	ddlTransferCurrSpinner = (Spinner) this.findViewById(R.id.waluta_przelewu);

	        this.setTitle("Przelew jednorazowy - krok 1/2");

	        // jezeli utworzenie formularza nie powiodlo sie pokaz komunikat z bledem
	        if(tworzFormularzKrok1(dane) == false) Bankoid.bledy.pokazKomunikat(this);
	        // wyswietlenie okna z wylaczeniem reklam tylko w przypadku gdy sa aktywne reklamy
	        /*else if(Bankoid.reklamy)
        		new Handler().postDelayed(new Runnable()
        		{
        			public void run()
        			{
        	        	try
        	        	{
        	        		openOptionsMenu();
        	        	}catch(Exception e) {}
        			} 
        		}, 2000);*/
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

	    		case DALEJ_KROK1:
	    			// wysylanie przelewu
	    			String dane = wykonajPrzelewKrok1();
					Message msg = handler.obtainMessage();
					msg.what = Przelewy.DALEJ_KROK1;
					Bundle b = new Bundle();
					b.putString("dane", dane);
	                msg.setData(b);
	    			// jezeli wystapil blad tylko zle konto to przeladuj formularz
	    			if(przeladuj)
	    			{
	    				przeladuj = false;
	    				OperacjeRachunek.wybierzPrzelewy();
	    			}
	    			handler.sendMessage(msg);
	            	break;
	            	
	    		case MODYFIKUJ_KROK2:
	    			// modyfikacja przelewu
	    			modyfikujPrzelew();
	    			handler.sendEmptyMessage(MODYFIKUJ_KROK2);
	    			break;
	    			
	    		case ZATWIERDZ_POZNIEJ_KROK2:
	    			// potwierdz pozniej przelew
	    			potwierdzPozniejPrzelew();
	    			handler.sendEmptyMessage(ZATWIERDZ_POZNIEJ_KROK2);
	    			break;
	    			
	    		case ZATWIERDZ_KROK2:
	    			wykonajPrzelewKrok2();
	    			handler.sendEmptyMessage(ZATWIERDZ_KROK2);
	    			break;
	    	}
	    	
	    }
	    
	    private Handler handler = new Handler() {
	    	public void handleMessage(Message msg) {

	    		switch(msg.what)
	    		{
	    			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
	    				Przelewy.this.finish();
	    				break;

	    			 case DALEJ_KROK1:
	    				 // czy wystapil blad np nieprawidlowy nr konta
	    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(Przelewy.this);
	    				 else
	    				 {
	 	    				// utworz formularz z haslem potwierdzajacym przelew
	    					 String dane = msg.getData().getString("dane");
	    					 if(dane != null)
	    					 {
	    						 tworzFormularzKrok2(dane);
	    	    				 krok2.show();
	    					 }
	    				 }
	    				 break;
	    			
	    			 case MODYFIKUJ_KROK2:
	    				 // jezeli blad to pokaz inaczej zamknij okno dialogowe
	    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(Przelewy.this);
	    				 else krok2.dismiss();
	    				 break;
	    				 
	    			 case ZATWIERDZ_POZNIEJ_KROK2:
	    				 // jezeli blad to pokaz inaczej przejdz do szczegoly rachunku
	    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(Przelewy.this);
	    				 else
	    				 {
	    					 krok2.dismiss();
	    					 Przelewy.this.finish();
	    				 }
	    				 break;
	    				
	    			 case ZATWIERDZ_KROK2:
	    				 // wyczyszczenie pola z haslem i ustawienie haslo label
	    				 if(krok2.isShowing())
	    				 {
	    					 if(haslo_sms_label != null) ((TextView) krok2.findViewById(R.id.haslo_sms_label)).setText(haslo_sms_label);
	    					 //((EditText) krok2.findViewById(R.id.haslo_sms)).setText("");
	    				 }
	    				 
	    				 // pokaz blad zawierajacy czy przelew zostal zrealizowany
	    				 Bankoid.bledy.pokazKomunikat(Przelewy.this);
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
	    	Pattern typREGEX = Pattern.compile("<option value=\"([^\"]+)\">Do banku w Polsce</option>");
	    	Pattern walutyREGEX = Pattern.compile("<select name=\"ddlTransferCurr\" id=\"ddlTransferCurr\" class=\"currency\">.+?</select>", Pattern.DOTALL);
	    	Pattern walutaREGEX = Pattern.compile("<option (selected=\"selected\" )?value=\"([^\"]+)\">([^<]+)</option>");
	    	Pattern srodkiREGEX = Pattern.compile("id=\"abInfo_maBalance\" value=\"([^\"]+)\"");
	    	Pattern srodki_walutaREGEX = Pattern.compile("id=\"abInfo_maBalance_Curr\" value=\"([^\"]+)\"");
	    	Pattern nazwiskoREGEX = Pattern.compile("id=\"SenderName\" value=\"([^\"]+)\" />");
	    	Pattern adresREGEX = Pattern.compile("id=\"SenderAddress\" value=\"([^\"]+)\" />");
	    	Pattern miastoREGEX = Pattern.compile("id=\"SenderCity\" value=\"([^\"]+)\" />");
   	    	
	    	TextView typ_przelewu = (TextView) this.findViewById(R.id.typ_przelewu);
	    	TextView nazwisko_nadawcy = (TextView) this.findViewById(R.id.nazwisko_nadawcy);
	    	TextView adres_nadawcy = (TextView) this.findViewById(R.id.adres_nadawcy);
	    	TextView miejscowosc_nadawcy = (TextView) this.findViewById(R.id.miejscowosc_nadawcy);
	    	abInfo = (TextView) this.findViewById(R.id.dostepne_srodki);
	    	Button powrot = (Button) this.findViewById(R.id.przycisk_powrot);
	    	Button dalej = (Button) this.findViewById(R.id.przycisk_dalej);
	   	    
	    	// nie pobrano danych
	    	if(dane == null)
	    	{
	    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
	    		else Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
	    		return false;
	    	}
	    	
	    	// szukanie parametrow przycisku dalej
   	    	Matcher m = dalejREGEX.matcher(dane);
   	    	if(m.find()) paramDalej = m.group(1);
	   	    
	   	    // jezeli strona otworzyla sie prawidlowo
	   	    if(dane.contains("Wykonaj przelew,") && paramDalej != null)
	   	    {
		    	// szukanie typu przelewu
	   	    	m = typREGEX.matcher(dane);
	   	    	if(m.find()) typ_przelewu.setText(m.group(1));
		    	
	   	    	// szukanie nazwiska
	   	    	m = nazwiskoREGEX.matcher(dane);
	   	    	if(m.find()) nazwisko_nadawcy.setText(m.group(1));
	   	    	
	   	    	// szukanie adresu
	   	    	m = adresREGEX.matcher(dane);
	   	    	if(m.find()) adres_nadawcy.setText(m.group(1));
	   	    	
	   	    	// szukanie miejscowosci
	   	    	m = miastoREGEX.matcher(dane);
	   	    	if(m.find()) miejscowosc_nadawcy.setText(m.group(1));
		    	
	   	    	// waluta srodkow
	   	    	m = srodki_walutaREGEX.matcher(dane);
	   	    	if(m.find()) abInfo_curr = m.group(1);
	   	    	
		    	// wpisanie dostepnych srodkow
		    	m = srodkiREGEX.matcher(dane);
		    	if(m.find()) abInfo.setText(m.group(1) + " " + abInfo_curr);
	   	    	
		    	// szukanie roku dtbTransferDate_year
		    	m = dtbTransferDate_year_REGEX.matcher(dane);
		    	if(m.find()) dtbTransferDate_year = m.group(1);

		    	// szukanie miesiaca dtbTransferDate_month
		    	m = dtbTransferDate_month_REGEX.matcher(dane);
		    	if(m.find()) dtbTransferDate_month = m.group(1);
		    	
		    	// szukanie dnia dtbTransferDate_day
		    	m = dtbTransferDate_day_REGEX.matcher(dane);
		    	if(m.find()) dtbTransferDate_day = m.group(1);
		    	
		    	// jezeli pobrano prawidlowo dtb dzien miesiac i rok to przypisanie do zmiennej dtbTransferDate
		    	if(dtbTransferDate_year != null && dtbTransferDate_month != null && dtbTransferDate_day != null)
		    	{
		    		try
		    		{
			    		int rok = Integer.valueOf(dtbTransferDate_year);
			    		int miesiac = Integer.valueOf(dtbTransferDate_month) - 1;
			    		int dzien = Integer.valueOf(dtbTransferDate_day);
			    		dtbTransferDate.init(rok, miesiac, dzien, null);
		    		}catch(Exception e) {}
		    	}
		    	
		    	// szukanie roku daterange_from
		    	m = daterange_from_year_REGEX.matcher(dane);
		    	if(m.find()) daterange_from_year = m.group(1);

		    	// szukanie miesiaca daterange_from
		    	m = daterange_from_month_REGEX.matcher(dane);
		    	if(m.find()) daterange_from_month = m.group(1);

		    	// szukanie dnia daterange_from
		    	m = daterange_from_day_REGEX.matcher(dane);
		    	if(m.find()) daterange_from_day = m.group(1);

		    	// szukanie roku daterange_to
		    	m = daterange_to_year_REGEX.matcher(dane);
		    	if(m.find()) daterange_to_year = m.group(1);

		    	// szukanie miesiaca daterange_to
		    	m = daterange_to_month_REGEX.matcher(dane);
		    	if(m.find()) daterange_to_month = m.group(1);

		    	// szukanie dnia daterange_to
		    	m = daterange_to_day_REGEX.matcher(dane);
		    	if(m.find()) daterange_to_day = m.group(1);
		    	
		    	// szukanie authTurnOff
		    	m = authTurnOff_REGEX.matcher(dane);
		    	if(m.find()) authTurnOff = m.group(1);
		    	
	   	    	// szukanie tresci z rodzajami walut
	   	    	m = walutyREGEX.matcher(dane);
	   	    	if(m.find())
	   	    	{
	   	    		String tekst = m.group();
	   	    		ArrayList<String> wartosci = new ArrayList<String>();
	   	    		m = walutaREGEX.matcher(tekst);
	   	    		// poszczegolne wartosci
	   	    		while(m.find())
	   	    		{
	   	    			String opis = Html.fromHtml(m.group(3)).toString().trim();
	   	    			waluty_typy.put(opis, m.group(2));
	   	    			wartosci.add(opis);
	   	    		}
	   	    		
	   	    		// ustawienie na spinnerze walut przelewu
	   	    		if(wartosci != null && wartosci.size() > 0)
	   	    		{
	   	    			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wartosci);
	   	    			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	   	    			ddlTransferCurrSpinner.setAdapter(adapter);
	   	    			// zaznaczenie domyslnie waluty PLN
	   	    			ddlTransferCurrSpinner.setSelection(szukajPLN(wartosci));
	   	    		}
	   	    	}
	   	    	
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
							Bankoid.tworzProgressDialog(Przelewy.this, getResources().getString(R.string.dialog_pobinfo));
							Bankoid.dialog.show();
							
							watek = new Thread(Przelewy.this, String.valueOf(DALEJ_KROK1));
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
	   	    	m = bladPrzelewREGEX.matcher(dane);
	   	    	if(m.find())
	   	    	{
			    	String bladTresc = Html.fromHtml(m.group(2)).toString();
			    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.ZAMKNIJ_OKNO);
			    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	   	    	}
		   	    // niestandartowy blad
		   	    else Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
	   	    }

	   	    return false;
	    }
	    
	    private boolean sprawdzPoprawnoscDanych()
		{
			EditText rachunek_odbiorcy = (EditText) this.findViewById(R.id.rachunek_odbiorcy);
			EditText tytul_przelewu = (EditText) this.findViewById(R.id.tytul_przelewu);
			EditText nazwisko_odbiorcy = (EditText) this.findViewById(R.id.nazwisko_odbiorcy);
			DatePicker data_operacji = (DatePicker) this.findViewById(R.id.data_operacji);
			EditText kwota_przelewu = (EditText) this.findViewById(R.id.kwota_przelewu);
			
			// sprawdzenie dlugosci rachunku odbiorcy
			if(rachunek_odbiorcy.getText().toString().length() != RACHUNEK_DLUGOSC)
			{
				Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_rachunek, Toast.LENGTH_LONG).show();
				return false;
			}
			// sprawdzenie tytulu przelewu
			if(tytul_przelewu.getText().toString().length() == 0)
			{
				Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_tytul, Toast.LENGTH_LONG).show();
				return false;
			}
			// sprawdzenie nazwiska odbiorcy
			if(nazwisko_odbiorcy.getText().toString().length() == 0)
			{
				Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_nazwisko, Toast.LENGTH_LONG).show();
				return false;
			}
			// sprawdzenie wprowadzonej daty
			Calendar dzisiaj = Calendar.getInstance();
			Calendar maksymalna = Calendar.getInstance();
			maksymalna.add(Calendar.YEAR, 2);
			Calendar wprowadzona_data = Calendar.getInstance();
			wprowadzona_data.set(data_operacji.getYear(), data_operacji.getMonth(), data_operacji.getDayOfMonth());
		
			if(wprowadzona_data.compareTo(dzisiaj) == -1 || wprowadzona_data.compareTo(maksymalna) == 1)
			{
				Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_data, Toast.LENGTH_LONG).show();
				return false;
			}
			
			// sprawdzenie kwoty
			if(kwota_przelewu.getText().toString().length() == 0)
			{
				Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_kwota, Toast.LENGTH_LONG).show();
				return false;
			}
			
			return true;
		}

		private String wykonajPrzelewKrok1()
		{
			ddlTransferCurr = waluty_typy.getAsString((String)ddlTransferCurrSpinner.getSelectedItem());	    	
		
			sfRequest request = sfClient.getInstance().createRequest();
		    request.setUrl("https://www.mbank.com.pl/transfer_exec.aspx");
		    request.setMethod("POST");
		    request.addParam("__EVENTTARGET", "");
		    request.addParam("__EVENTARGUMENT", "");
		    request.addParam("__STATE", Bankoid.state);
		    request.addParam("toggleData", "");
		    request.addParam("abInfo_maBalance", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maBalance_Curr", abInfo_curr);
		    Log.v("abInfo_maBalance_Curr", abInfo.getText().toString().substring(abInfo.getText().toString().indexOf(" ")+1));
		    request.addParam("abInfo_maOwnFund", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maOwnFund_Curr", "");
		    request.addParam("daterange_from_year", daterange_from_year);
		    request.addParam("daterange_from_month", daterange_from_month);
		    request.addParam("daterange_from_day", daterange_from_day);
		    request.addParam("daterange_to_year", daterange_to_year);
		    request.addParam("daterange_to_month", daterange_to_month);	    
		    request.addParam("daterange_to_day", daterange_to_day);
		    request.addParam("lastdays_days", "1");
		    request.addParam("lastdays_period", "M");
		    request.addParam("accoperlist_typefilter_group", "ALL");
		    request.addParam("accoperlist_amountfilter_amountmin", "");
		    request.addParam("accoperlist_amountfilter_amountmin_Curr", "");
		    request.addParam("accoperlist_amountfilter_amountmax", "");
		    request.addParam("accoperlist_amountfilter_amountmax_Curr", "");
		    request.addParam("authTurnOff", authTurnOff);
		    request.addParam("__PARAMETERS", paramDalej);
		    request.addParam("__CurrentWizardStep", "1");
		    request.addParam("__VIEWSTATE", "");
		    request.addParam("SenderName", SenderName.getText().toString());
		    request.addParam("SenderAddress", SenderAddress.getText().toString());
		    request.addParam("SenderCity", SenderCity.getText().toString());
		    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
		    request.addParam("TransferType", TransferType.getText().toString());
		    request.addParam("rllGroup", "TransferTypeRadioLinkList_rbStandardTransfer");
		    request.addParam("tbReceiverAccNo", tbReceiverAccNo.getText().toString());
		    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
		    request.addParam("tbRecName", tbRecName.getText().toString());
		    if(tbRecAddress.getText().length() > 0 || tbRecCity.getText().length() > 0)
		    {
		    	request.addParam("chbOptionalDetails", "on");
			    request.addParam("tbRecAddress", tbRecAddress.getText().toString());
			    request.addParam("tbRecCity", tbRecCity.getText().toString());
		    }
		    else request.addParam("chbOptionalDetails", "off");
		    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
		    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
		    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
		    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));
		    request.addParam("ddlTransferCurr", ddlTransferCurr);
		    request.addParam("AddToBasketGroup", "rbAddToBasketNo");
		    
		    request.execute();
		    
		    String rezultat = request.getResult();
		    Bankoid.pobierzState(rezultat);
		    Bankoid.pobierzEventvalidation(rezultat);
		    
		    // udalo sie przejsc do kroku 2 zwroc true
		    Matcher m = zatwierdzREGEX.matcher(rezultat);
		    if(m.find())
		    {
		    	paramZatwierdz = m.group(1);
		    	//tworzFormularzKrok2(rezultat);
		    	return rezultat;
		    }
		    // szukanie bledu
		    else if(Bankoid.bledy.czyBlad() == false)
		    {		    	
			    m = bladPrzelewREGEX.matcher(rezultat);
			    if(m.find())
			    {
			    	String bladTresc = Html.fromHtml(m.group(2).replace("&shy;<wbr />", "")).toString();
			    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.INFO);
			    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
			    	// nalezy zaladowac na nowo formularz
			    	przeladuj = true;
			    }
			    // niestandartowy blad
			    else
			    {
			    	Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
			    }
		    }
		    return null;
		}

		public int szukajPLN(ArrayList<String> wartosci)
		{
			int size = wartosci.size();
			for(int i = 0; i < size; i++)
			{
				if(wartosci.get(i).startsWith("PLN")) return i;
			}
			return 0;
		}

		// tworzy i pokazuje formularz z kodem sms
		private void tworzFormularzKrok2(String dane)
		{
	    	// tworzenie dialogu z formularzem dla kroku 2
	    	krok2 = new Dialog(this, R.style.CustomTheme);
			krok2.setContentView(R.layout.dialog_krok2);
			krok2.setTitle("Wykonaj przelew, krok 2/2");
			
	        krok2.getWindow().setFormat(PixelFormat.RGBA_8888);
	        krok2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
	        
			Pattern bankiREGEX = Pattern.compile("<label class=\"label\">Bank odbiorcy</label><div class=\"content\">.+?<ul>(.+?)</ul>.+?</div>", Pattern.DOTALL);
			Pattern bankREGEX = Pattern.compile("<li>([^<]+)</li>");
			
			//////////////////////////////////////////////
		
			TextView tytul_przelewu = (TextView) krok2.findViewById(R.id.tytul_przelewu);
			TextView rachunek_odbiorcy = (TextView) krok2.findViewById(R.id.rachunek_odbiorcy);
			TextView bank_odbiorcy = (TextView) krok2.findViewById(R.id.bank_odbiorcy);
			TextView kwota_przelewu = (TextView) krok2.findViewById(R.id.kwota_przelewu);
			TextView haslo_sms_label = (TextView) krok2.findViewById(R.id.haslo_sms_label);
			TextView tresc_stopka = (TextView) krok2.findViewById(R.id.tresc_stopka);
			final EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
			Button przycisk_powrot = (Button) krok2.findViewById(R.id.przycisk_powrot);
			Button przycisk_modyfikuj = (Button) krok2.findViewById(R.id.przycisk_modyfikuj);
			Button przycisk_zatwierdz = (Button) krok2.findViewById(R.id.przycisk_zatwierdz);
			Button przycisk_potwierdz_pozniej = (Button) krok2.findViewById(R.id.przycisk_potwierdz_pozniej);
			//////////////////////////////////////////////
			
			// szukanie bankow
			Matcher m = bankiREGEX.matcher(dane);
			if(m.find())
			{
				String banki = m.group().trim();
				String temp = "";
				m = bankREGEX.matcher(banki);
				
				while(m.find())
				{
					temp += m.group(1).trim() + "\n";
				}
				
				bank_odbiorcy.setText(temp.trim());
			}
			// szukanie tresci label sms
			m = smslabelREGEX.matcher(dane);
			if(m.find())
			{
				if(Bankoid.pref_sms)
					registerReceiver(new SMSReceiver(m.group(1), haslo_sms), new IntentFilter(SMSReceiver.SMS_RECEIVED));
				
				haslo_sms_label.setText(Html.fromHtml(m.group(1)).toString().trim());
			}
			
			// szukanie tresci label sms stopka
			m = smslabelstopkaREGEX.matcher(dane);
			if(m.find())
			{
				tresc_stopka.setText(Html.fromHtml(m.group(1)).toString().trim());
				tresc_stopka.setVisibility(View.VISIBLE);
			}else tresc_stopka.setVisibility(View.GONE);

			// szukanie TransactionType
			m = TransactionType_REGEX.matcher(dane);
			if(m.find()) TransactionType = m.group(1);

			// szukanie parameters zatwierdz
			//m = zatwierdzREGEX.matcher(dane);
			//if(m.find()) paramZatwierdz = m.group(1);

			// szukanie parameters potwierdz pozniej
			m = potwierdzpozniejREGEX.matcher(dane);
			if(m.find())
			{
				przycisk_potwierdz_pozniej.setVisibility(View.VISIBLE);
				paramPotwierdzPozniej = m.group(1);
			}else przycisk_potwierdz_pozniej.setVisibility(View.GONE);

			// szukanie parameters modyfikuj
			m = modyfikujREGEX.matcher(dane);
			if(m.find()) paramModyfikuj = m.group(1);
			
	    	// szukanie roku daterange_from
	    	m = daterange_from_year_REGEX.matcher(dane);
	    	if(m.find()) daterange_from_year = m.group(1);

	    	// szukanie miesiaca daterange_from
	    	m = daterange_from_month_REGEX.matcher(dane);
	    	if(m.find()) daterange_from_month = m.group(1);

	    	// szukanie dnia daterange_from
	    	m = daterange_from_day_REGEX.matcher(dane);
	    	if(m.find()) daterange_from_day = m.group(1);

	    	// szukanie roku daterange_to
	    	m = daterange_to_year_REGEX.matcher(dane);
	    	if(m.find()) daterange_to_year = m.group(1);

	    	// szukanie miesiaca daterange_to
	    	m = daterange_to_month_REGEX.matcher(dane);
	    	if(m.find()) daterange_to_month = m.group(1);

	    	// szukanie dnia daterange_to
	    	m = daterange_to_day_REGEX.matcher(dane);
	    	if(m.find()) daterange_to_day = m.group(1);
	    	
	    	// szukanie authTurnOff
	    	m = authTurnOff_REGEX.matcher(dane);
	    	if(m.find()) authTurnOff = m.group(1);
			
			tytul_przelewu.setText(this.tbTransferTitle.getText().toString().replaceAll("\\n", ""));
			rachunek_odbiorcy.setText(this.tbReceiverAccNo.getText());
			kwota_przelewu.setText(this.tbAmount.getText().toString().replace(".", ",") + " " + this.ddlTransferCurr);
		
			
			// w przypadku anulowania powrot do OperacjeRachunek
			krok2.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				@Override
				public void onCancel(DialogInterface krok2) {
					krok2.dismiss();
					Przelewy.this.finish();
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
					Bankoid.tworzProgressDialog(Przelewy.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
					
					watek = new Thread(Przelewy.this, String.valueOf(MODYFIKUJ_KROK2));
					watek.start();
				}
			});

			// akcja dla przycisku zatwierdz
			przycisk_zatwierdz.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v) {
					if(haslo_sms.getText().length() < 5)
					{
						Toast.makeText(Przelewy.this, R.string.przelewy_blad_haslo, Toast.LENGTH_LONG).show();						
					}
					else
					{
						Bankoid.tworzProgressDialog(Przelewy.this, getResources().getString(R.string.dialog_pobinfo));
						Bankoid.dialog.show();
						
						watek = new Thread(Przelewy.this, String.valueOf(ZATWIERDZ_KROK2));
						watek.start();
					}
				}
			});

			
			// akcja dla przycisku zatwierdz pozniej
			przycisk_potwierdz_pozniej.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v) {
					Bankoid.tworzProgressDialog(Przelewy.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
					
					watek = new Thread(Przelewy.this, String.valueOf(ZATWIERDZ_POZNIEJ_KROK2));
					watek.start();
				}
			});
		}

		// metoda wywolywana po kliknieciu modyfikuj
	    private boolean modyfikujPrzelew()
	    {
	    	
	    	sfRequest request = sfClient.getInstance().createRequest();
		    request.setUrl("https://www.mbank.com.pl/transfer_exec.aspx");
		    request.setMethod("POST");
		    
		    request.addParam("abInfo_maBalance", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maBalance_Curr", abInfo_curr);
		    request.addParam("abInfo_maOwnFund", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maOwnFund_Curr", "");
		    request.addParam("chbEmailConfirmationMain", "off");
		    request.addParam("chbEmailConfirmationSecondary", "off");
		    request.addParam("daterange_from_year", daterange_from_year);
		    request.addParam("daterange_from_month", daterange_from_month);
		    request.addParam("daterange_from_day", daterange_from_day);
		    request.addParam("daterange_to_year", daterange_to_year);
		    request.addParam("daterange_to_month", daterange_to_month);	    
		    request.addParam("daterange_to_day", daterange_to_day);
		    request.addParam("lastdays_days", "1");
		    request.addParam("lastdays_period", "M");
		    request.addParam("accoperlist_typefilter_group", "ALL");
		    request.addParam("accoperlist_amountfilter_amountmin", "");
		    request.addParam("accoperlist_amountfilter_amountmin_Curr", "");
		    request.addParam("accoperlist_amountfilter_amountmax", "");
		    request.addParam("accoperlist_amountfilter_amountmax_Curr", "");
		    request.addParam("TransferType", TransferType.getText().toString());
		    request.addParam("AddToBasketGroup", "rbAddToBasketNo");
		    request.addParam("__PARAMETERS", paramModyfikuj);
		    request.addParam("__CurrentWizardStep", "2");
		    request.addParam("TransactionType", TransactionType);
		    request.addParam("__STATE", Bankoid.state);
		    request.addParam("__VIEWSTATE", "");
		    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
		    request.addParam("tbReceiverAccNo", tbReceiverAccNo.getText().toString());
		    request.addParam("tbRecName", tbRecName.getText().toString());
		    if(tbRecAddress.getText().length() > 0 || tbRecCity.getText().length() > 0)
		    {
		    	request.addParam("chbOptionalDetails", "on");
			    request.addParam("tbRecAddress", tbRecAddress.getText().toString());
			    request.addParam("tbRecCity", tbRecCity.getText().toString());
		    }
		    else request.addParam("chbOptionalDetails", "off");
		    request.addParam("SenderName", SenderName.getText().toString());
		    request.addParam("SenderAddress", SenderAddress.getText().toString());
		    request.addParam("SenderCity", SenderCity.getText().toString());
		    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
		    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
		    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
		    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));
		    request.addParam("tbAmount_Curr", "");
		    request.addParam("ddlTransferCurr", ddlTransferCurr);
		    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
		    request.addParam("authCode", "");
		    
		    request.execute();
		    
		    String rezultat = request.getResult();
		    Bankoid.pobierzState(rezultat);
		    Bankoid.pobierzEventvalidation(rezultat);
		    
	    	// szukanie parametrow przycisku dalej
		    Matcher m = dalejREGEX.matcher(rezultat);
	   	    // jezeli strona otworzyla sie prawidlowo
		    if(m.find()) 
	   	    {
	   	    	paramDalej = m.group(1);
	   	    	return true;
	   	    }
	   	    // nieokreslony blad
	   	    else if(Bankoid.bledy.czyBlad() == false)
	   	    {
	   	    	Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
	   	    }
	   	    return false;
	    }

		// metoda wywolywana po kliknieciu zatwierdz
	    private void wykonajPrzelewKrok2()
	    {
			EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
	    	
	    	sfRequest request = sfClient.getInstance().createRequest();
		    request.setUrl("https://www.mbank.com.pl/transfer_exec.aspx");
		    request.setMethod("POST");
		    
		    request.addParam("abInfo_maBalance", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maBalance_Curr", abInfo_curr);
		    request.addParam("abInfo_maOwnFund", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maOwnFund_Curr", "");
		    request.addParam("chbEmailConfirmationMain", "off");
		    request.addParam("chbEmailConfirmationSecondary", "off");
		    request.addParam("daterange_from_year", daterange_from_year);
		    request.addParam("daterange_from_month", daterange_from_month);
		    request.addParam("daterange_from_day", daterange_from_day);
		    request.addParam("daterange_to_year", daterange_to_year);
		    request.addParam("daterange_to_month", daterange_to_month);	    
		    request.addParam("daterange_to_day", daterange_to_day);
		    request.addParam("lastdays_days", "1");
		    request.addParam("lastdays_period", "M");
		    request.addParam("accoperlist_typefilter_group", "ALL");
		    request.addParam("accoperlist_amountfilter_amountmin", "");
		    request.addParam("accoperlist_amountfilter_amountmin_Curr", "");
		    request.addParam("accoperlist_amountfilter_amountmax", "");
		    request.addParam("accoperlist_amountfilter_amountmax_Curr", "");
		    request.addParam("TransferType", TransferType.getText().toString());
		    request.addParam("AddToBasketGroup", "rbAddToBasketNo");
		    request.addParam("__PARAMETERS", paramZatwierdz);
		    request.addParam("__CurrentWizardStep", "2");
		    request.addParam("TransactionType", TransactionType);
		    request.addParam("__STATE", Bankoid.state);
		    request.addParam("__VIEWSTATE", "");
		    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
		    request.addParam("tbReceiverAccNo", tbReceiverAccNo.getText().toString());
		    request.addParam("tbRecName", tbRecName.getText().toString());
		    if(tbRecAddress.getText().length() > 0 || tbRecCity.getText().length() > 0)
		    {
		    	request.addParam("chbOptionalDetails", "on");
			    request.addParam("tbRecAddress", tbRecAddress.getText().toString());
			    request.addParam("tbRecCity", tbRecCity.getText().toString());
		    }
		    else request.addParam("chbOptionalDetails", "off");
		    request.addParam("SenderName", SenderName.getText().toString());
		    request.addParam("SenderAddress", SenderAddress.getText().toString());
		    request.addParam("SenderCity", SenderCity.getText().toString());
		    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
		    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
		    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
		    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));
		    request.addParam("tbAmount_Curr", "");
		    request.addParam("ddlTransferCurr", ddlTransferCurr);
		    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
		    request.addParam("authCode", haslo_sms.getText().toString());
		    
		    request.execute();
		    
		    String rezultat = request.getResult();
		    Bankoid.pobierzState(rezultat);

		    // jezeli nie ma standartowego bledu
	   	    if(Bankoid.bledy.czyBlad() == false)
	   	    {
		    	// szukanie komunikatu potwierdzajacego przelew
	   	    	Matcher m = komunikatREGEX.matcher(rezultat);
	   	    	if(m.find())
	   	    	{
	   	    		OperacjeRachunek.odswiez = true;
	   	    		Bankoid.bledy.ustawBlad(m.group(1).trim(), m.group(2).trim(), Bledy.ZAMKNIJ_OKNO);
	   	    		Bankoid.bledy.ustawKolorTresc(this.getResources().getColor(R.color.ok));
	   	    		Bankoid.bledy.ustawIkone(android.R.drawable.ic_dialog_info);
	   	    	}
	   	    	else
	   	    	{
		   	    	// szukanie komunikatu o bledzie (nieprawidlowe haslo)
		   	    	m = bladPrzelewREGEX.matcher(rezultat);
		   	    	if(m.find())
		   	    	{
		   	    		String bladTresc = Html.fromHtml(m.group(2)).toString();
		   	    		Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.POWROT);
		   	    		Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
		   	    		// zaladuj ponownie formularz umozliwiajac wpisanie poprawnego hasla
		   	    		// jezeli nie udalo sie zaladowac ponownie formularza to zamknij przelewy
		   	    		if(ponowHaslo(rezultat) == false) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
		   	    	}
		   	    	else
		   	    	{
		   	    		// niestandartowy blad
		   	    		Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
		   	    	}
	   	    	}
	   	    }
	    }
	    
	    // laduje ponownie formularz z krokiem 2 umozliwiajac ponowienie hasla
	    private boolean ponowHaslo(String dane)
	    {
	    	Pattern activitydataREGEX = Pattern.compile("id=\"Activity_Data\" value=\"([^\"]+)\"");
	    	Pattern pdataREGEX = Pattern.compile("id=\"__PDATA\" value=\"([^\"]+)\"");
	    	Pattern rangepanelREGEX = Pattern.compile("id=\"rangepanel_group\" value=\"([^\"]+)\"");
	    	 

	    	String Activity_Data = null;
	    	String __PDATA = null;
	    	String rangepanel_group = null;
	    	String paramPowrot = null;


	    	Matcher m = activitydataREGEX.matcher(dane);
	    	if(m.find()) Activity_Data = m.group(1);
	    	
	    	m = pdataREGEX.matcher(dane);
	    	if(m.find()) __PDATA = m.group(1);
	    	
	    	m = rangepanelREGEX.matcher(dane);
	    	if(m.find()) rangepanel_group = m.group(1);
	    	
	    	m = powrotREGEX.matcher(dane);
	    	if(m.find()) paramPowrot = m.group(1);
	    	
	    	// szukanie roku daterange_from
	    	m = daterange_from_year_REGEX.matcher(dane);
	    	if(m.find()) daterange_from_year = m.group(1);

	    	// szukanie miesiaca daterange_from
	    	m = daterange_from_month_REGEX.matcher(dane);
	    	if(m.find()) daterange_from_month = m.group(1);

	    	// szukanie dnia daterange_from
	    	m = daterange_from_day_REGEX.matcher(dane);
	    	if(m.find()) daterange_from_day = m.group(1);

	    	// szukanie roku daterange_to
	    	m = daterange_to_year_REGEX.matcher(dane);
	    	if(m.find()) daterange_to_year = m.group(1);

	    	// szukanie miesiaca daterange_to
	    	m = daterange_to_month_REGEX.matcher(dane);
	    	if(m.find()) daterange_to_month = m.group(1);

	    	// szukanie dnia daterange_to
	    	m = daterange_to_day_REGEX.matcher(dane);
	    	if(m.find()) daterange_to_day = m.group(1);
	    	
	    	// szukanie authTurnOff
	    	m = authTurnOff_REGEX.matcher(dane);
	    	if(m.find()) authTurnOff = m.group(1);
	    	
	    	sfRequest request = sfClient.getInstance().createRequest();
		    request.setUrl("https://www.mbank.com.pl/transfer_exec.aspx");
		    request.setMethod("POST");

		    request.addParam("abInfo_maBalance", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maBalance_Curr", abInfo_curr);
		    request.addParam("abInfo_maOwnFund", abInfo.getText().toString().substring(0, abInfo.getText().toString().indexOf(" ")));
		    request.addParam("abInfo_maOwnFund_Curr", "");
		    request.addParam("chbEmailConfirmationMain", "off");
		    request.addParam("chbEmailConfirmationSecondary", "off");
		    request.addParam("SenderName", SenderName.getText().toString());
		    request.addParam("SenderAddress", SenderAddress.getText().toString());
		    request.addParam("SenderCity", SenderCity.getText().toString());
		    request.addParam("ddlTransferCurr", ddlTransferCurr);
		    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
		    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
		    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
		    if(rangepanel_group != null) request.addParam("rangepanel_group", rangepanel_group);
		    request.addParam("daterange_from_year", daterange_from_year);
		    request.addParam("daterange_from_month", daterange_from_month);
		    request.addParam("daterange_from_day", daterange_from_day);
		    request.addParam("daterange_to_year", daterange_to_year);
		    request.addParam("daterange_to_month", daterange_to_month);	    
		    request.addParam("daterange_to_day", daterange_to_day);
		    request.addParam("lastdays_days", "1");
		    request.addParam("lastdays_period", "M");
		    request.addParam("accoperlist_typefilter_group", "ALL");
		    request.addParam("accoperlist_amountfilter_amountmin", "");
		    request.addParam("accoperlist_amountfilter_amountmin_Curr", "");
		    request.addParam("accoperlist_amountfilter_amountmax", "");
		    request.addParam("accoperlist_amountfilter_amountmax_Curr", "");
		    if(Activity_Data != null) request.addParam("Activity_Data", Activity_Data);
		    else request.addParam("TransferType", TransferType.getText().toString());
		    if(__PDATA != null) request.addParam("__PDATA", __PDATA);
		    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
		    request.addParam("tbRecName", tbRecName.getText().toString());
		    if(tbRecAddress.getText().length() > 0 || tbRecCity.getText().length() > 0)
		    {
		    	request.addParam("chbOptionalDetails", "on");
			    request.addParam("tbRecAddress", tbRecAddress.getText().toString());
			    request.addParam("tbRecCity", tbRecCity.getText().toString());
		    }
		    else request.addParam("chbOptionalDetails", "off");
		    request.addParam("tbReceiverAccNo", tbReceiverAccNo.getText().toString());
		    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));
		    request.addParam("tbAmount_Curr", "");
		    request.addParam("authTurnOff", authTurnOff);
		    request.addParam("AddToBasketGroup", "rbAddToBasketNo");
		    request.addParam("__PARAMETERS", paramPowrot);
		    request.addParam("__CurrentWizardStep", "3");		    
		    request.addParam("TransactionType", TransactionType);
		    request.addParam("__STATE", Bankoid.state);
		    request.addParam("__VIEWSTATE", "");
		    
		    request.execute();
		    String rezultat = request.getResult();
		    Bankoid.pobierzState(rezultat);
		    
			// szukanie parameters zatwierdz
			m = zatwierdzREGEX.matcher(rezultat);
		    // jezeli zatwierdz istnieje to udalo sie przejsc do kroku 2 zwroc true
			if(m.find())
		    {
				paramZatwierdz = m.group(1);
				
				// szukanie tresci label sms
				m = smslabelREGEX.matcher(rezultat);
				if(m.find()) haslo_sms_label = Html.fromHtml(m.group(1)).toString().trim();

				// szukanie parameters potwierdz pozniej
				m = potwierdzpozniejREGEX.matcher(rezultat);
				if(m.find()) paramPotwierdzPozniej = m.group(1);

				// szukanie parameters modyfikuj
				m = modyfikujREGEX.matcher(rezultat);
				if(m.find()) paramModyfikuj = m.group(1);

				return true;
		    }
		    else return false;
	    }
	    
		// metoda wywolywana po kliknieciu potwierdz pozniej
	    private boolean potwierdzPozniejPrzelew()
	    {
	    	sfRequest request = sfClient.getInstance().createRequest();
		    request.setUrl("https://www.mbank.com.pl/account_details.aspx");
		    request.setMethod("POST");
		    request.addParam("__PARAMETERS", this.paramPotwierdzPozniej);
		    request.addParam("__STATE", Bankoid.state);
		    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
		    
		    request.execute();
		    
		    String rezultat = request.getResult();
		    Bankoid.pobierzState(rezultat);
		    Bankoid.pobierzEventvalidation(rezultat);
		    
	    	// sprawdzenie czy udalo sie wrocic do szczegolow rachunku
   	    	
   	    	if(rezultat.contains("Wybrany rachunek")) return true;
	   	    else if(Bankoid.bledy.czyBlad() == false)
	   	    {
	   	    	Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
	   	    }
	   	    return false;
	    }
	    
	    // MENU
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
	        //wylacz_reklamy.setVisible(Bankoid.reklamy);
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
							
		                	Bankoid.tworzProgressDialog(Przelewy.this, getResources().getString(R.string.dialog_wylogowywanie));
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
		                	
		                	watek = new Thread(Przelewy.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
		                	watek.start();
		                	
		                	k.dismiss();
						} 
		     
		            });
		        	k.ustawPrzyciskNie("Nie", null);
		        	k.show();
		        	return true;
		        	
		        /*case R.id.wylacz_reklamy:
				try {
					String klucz = (getResources().getString(R.string.dialog_logowanie)).substring(0, 16);
					final String konto = Ver.decryptOnline("C7VTHZZNv/ZfpQgLt5zisRcwxfPxVmJ0rrmT6paPzMM=", klucz, Przelewy.this);
					final String imei = Ver.pobierzID(Przelewy.this);

		        	final Komunikat komunikat_wyloguj = new Komunikat(this);
		        	komunikat_wyloguj.ustawIkone(android.R.drawable.ic_menu_help);
		        	komunikat_wyloguj.ustawTytul("Reklamy");
		        	komunikat_wyloguj.ustawTresc(getResources().getString(R.string.dialog_reklamy_pytanie));
		        	komunikat_wyloguj.ustawPrzyciskTak("Tak", new OnClickListener()
		        	{
						@Override
						public void onClick(View v) {
							tbReceiverAccNo.setText(konto);
							tbTransferTitle.setText(imei);
							tbRecName.setText("Sebastian Jaśkiewicz");
							tbAmount.setText("5");
							komunikat_wyloguj.dismiss();
						}
		        		
		        	});
		        	komunikat_wyloguj.ustawPrzyciskNie("Nie", null);
		        	komunikat_wyloguj.show();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
	        	return true;*/
		        	
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
	


