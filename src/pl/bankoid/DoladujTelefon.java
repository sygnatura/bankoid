package pl.bankoid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout.LayoutParams;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class DoladujTelefon extends Activity implements Runnable {

	// STALE
	private final int NUMER_DLUGOSC = 9;
	
	private ContentValues operatorzy = new ContentValues();
  	private ArrayList<String> operator = new ArrayList<String>();
	private ContentValues kwoty = new ContentValues();
	private Thread watek;
	private AdView adView;
	
	private boolean przeladuj = false;
	
	// krok2
	private Dialog krok2;
	private String haslo_sms_label = null;
	
	Pattern ddlAmountREGEX = Pattern.compile("<select name=\"ddlAmount\"[^>]+>(.+?)</select>", Pattern.DOTALL);
	Pattern operatorREGEX = Pattern.compile("<select name=\"ddlOperatorList\" [^>]+>(.+?)</select>", Pattern.DOTALL);
	Pattern wartosciREGEX = Pattern.compile("<option (selected=\"selected\" )?value=\"([^\"]+)\">([^<]+)</option>");
	Pattern uwaga1REGEX = Pattern.compile("id=\"tbTransferDescr\" />.+?</div><div class=\"content2\">.+?<span>(.+?)</span>", Pattern.DOTALL);
	Pattern uwaga2REGEX = Pattern.compile("id=\"maTransferAmount\".+?/>.+?</div><div class=\"content2\">.+?<span>(.+?)</span>", Pattern.DOTALL);
	Pattern srodkiREGEX = Pattern.compile("<label class=\"label\">Dostępne środki</label><div class=\"content\">.+?<span>([^<]+)</span>", Pattern.DOTALL);
	Pattern limityREGEX = Pattern.compile("CheckPrepaidAmount\\(theform.maTransferAmount, '[^']+', '[^']+', '[^']+', '[^']+', '([^']+)', '([^']+)'\\)\\)");

	// parametr dla przyciskow
	private String paramDalej;
	private String paramZatwierdz;
	private String paramPotwierdzPozniej;
	private String paramModyfikuj;
	private String paramPowrot;
	
	// limity dla kwot
	int limit1 = 1;
	int limit2 = 500;
	
	// wybrana pozycja operatora
	int wybrany_operator = -1;
	
	// REFERENCJE DO POL FORMULARZA I ZMIENNE
	private Spinner ddlOperatorList;
	private Spinner ddlAmount;
	private EditText maTransferAmount;
	private TextView numer_telefonu_label;
	private EditText tbTransferDescr;
	private TextView nazwa_konta_label;
	private EditText nazwa_konta;
	private TextView uwaga1;
	private TextView uwaga2;
	private TextView dostepne_srodki;
	private Button przycisk_dalej;
	boolean AddNumberGroup = false;
	private String TransactionType = "";
	private String dtbTransferDate_year = "";
	private String dtbTransferDate_month = "";
	private String dtbTransferDate_day = "";
	private String authTurnOff = "";

	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.doladuj_telefon);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");
    	
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    	
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();
        
        // INICJALIZACJA ZMIENNYCH
    	ddlOperatorList = (Spinner) this.findViewById(R.id.operator_konta);
    	ddlAmount = (Spinner) this.findViewById(R.id.kwota_doladowania_spinner);
    	maTransferAmount = (EditText) this.findViewById(R.id.kwota_doladowania);
    	maTransferAmount.setFilters(new InputFilter[]{new MoneyValueFilter(), new InputFilter.LengthFilter(15)});
    	numer_telefonu_label = (TextView) this.findViewById(R.id.numer_telefonu_label);
    	tbTransferDescr = (EditText) this.findViewById(R.id.numer_telefonu);
    	nazwa_konta_label = (TextView) this.findViewById(R.id.nazwa_konta_label);
    	nazwa_konta = (EditText) this.findViewById(R.id.nazwa_konta);
    	uwaga1 = (TextView) this.findViewById(R.id.uwaga1);
    	uwaga2 = (TextView) this.findViewById(R.id.uwaga2);
    	dostepne_srodki = (TextView) this.findViewById(R.id.dostepne_srodki);
    	przycisk_dalej = (Button) this.findViewById(R.id.przycisk_dalej);

    	
        // reklama
        if(Bankoid.reklamy)
        {
        	adView = new AdView(this, AdSize.BANNER, Bankoid.ADMOB_ID); 
            FrameLayout layout = (FrameLayout) this.findViewById(R.id.ad);
            layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            layout.addView(adView);
            layout.setVisibility(View.VISIBLE);
            adView.loadAd(Bankoid.adr);
            //adView.bringToFront();	
        }
        
        this.setTitle("Doładowanie telefonu, krok 1/2");
        
        // jezeli otwieranie formularza nie powiodlo sie
        if(tworzFormularzKrok1(dane, false))
        {
        	SharedPreferences prefs = getSharedPreferences(Ustawienia.PREFS_NAME, 0);
        	String pref_tel = prefs.getString("pref_tel", null);
        	String pref_operator = prefs.getString("pref_operator", null);
        	
        	if(pref_tel != null) tbTransferDescr.setText(pref_tel);
        	if(pref_operator != null)
        	{
        		pref_operator = pref_operator.trim().toLowerCase();
        		int size = operator.size();
        		for(int i = 0; i < size; i++)
        		{
        			String o = operator.get(i).trim().toLowerCase();
        			if(o.equals(pref_operator))
        			{
        				ddlOperatorList.setSelection(i);
        				break;
        			}
        		}
        	}
        }
        else Bankoid.bledy.pokazKomunikat(this);
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

    		case Przelewy.ODSWIEZANIE:
    			// odswiezanie formularza
    			String dane = odswiezFormularz();
				Message msg = handler.obtainMessage();
				msg.what = Przelewy.ODSWIEZANIE;
				Bundle b = new Bundle();
				b.putString("dane", dane);
                msg.setData(b);
                
    			handler.sendMessage(msg);
            	break;

    		case Przelewy.DALEJ_KROK1:
    			// wysylanie przelewu
    			dane = wykonajPrzelewKrok1();
    			msg = handler.obtainMessage();
    			msg.what = Przelewy.DALEJ_KROK1;
				b = new Bundle();
				b.putString("dane", dane);
                msg.setData(b);
    			// jezeli wystapil blad tylko zle konto to przeladuj formularz
    			if(przeladuj)
    			{
    				przeladuj = false;
        			odswiezFormularz();
    			}
    			
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
    				 DoladujTelefon.this.finish();
    				 break;

    			 case Przelewy.ODSWIEZANIE:
   					 String dane = msg.getData().getString("dane");
   					 tworzFormularzKrok1(dane, true);
    				 // czy wystapil blad podczas odswiezania formularza
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(DoladujTelefon.this);
    				 break;
    				 
    			 case Przelewy.DALEJ_KROK1:
    				 // czy wystapil blad np nieprawidlowy nr telefonu
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(DoladujTelefon.this);
    				 else
    				 {
       					 dane = msg.getData().getString("dane");
       					 tworzFormularzKrok2(dane);
        				 krok2.show();
    				 }
    				 break;
    				 
    			 case Przelewy.MODYFIKUJ_KROK2:
    				 // jezeli blad to pokaz inaczej zamknij okno dialogowe
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(DoladujTelefon.this);
    				 else krok2.dismiss();
    				 break;

    			 case Przelewy.ZATWIERDZ_POZNIEJ_KROK2:
    				 // jezeli blad to pokaz inaczej przejdz do szczegoly rachunku
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(DoladujTelefon.this);
    				 else
    				 {
    					 krok2.dismiss();
    					 DoladujTelefon.this.finish();
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
    				 Bankoid.bledy.pokazKomunikat(DoladujTelefon.this);
    				 break;

    		}
    		
    		// zakmniecie dialogu
    		try
    		{
    			Bankoid.dialog.dismiss();
    		}catch(Exception e){}
    	}
    };
	
    public boolean tworzFormularzKrok1(String dane, boolean odswiezanie)
    {
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
    		return false;
    	}
    	
    	// szukanie parametrow przycisku dalej
	    Matcher m = Przelewy.dalejREGEX.matcher(dane);
	    if(m.find()) paramDalej = m.group(1);
   	    
   	    // jezeli strona otworzyla sie prawidlowo
   	    if(dane.contains("Doładowanie telefonu") && paramDalej != null)
   	    {
   	    	// sprawdzenie czy doladowanie nr tel czy konta voip
   	    	if(dane.contains("<label class=\"label\">Nazwa konta</label>"))
   	    	{
   	    		numer_telefonu_label.setVisibility(View.GONE);
   	    		tbTransferDescr.setVisibility(View.GONE);
   	    		nazwa_konta_label.setVisibility(View.VISIBLE);
   	    		nazwa_konta.setVisibility(View.VISIBLE);
   	    	}
   	    	else
   	    	{
   	    		numer_telefonu_label.setVisibility(View.VISIBLE);
   	    		tbTransferDescr.setVisibility(View.VISIBLE);
   	    		nazwa_konta_label.setVisibility(View.GONE);
   	    		nazwa_konta.setVisibility(View.GONE);
   	    	}
   	    	
   	    	// sprawdzanie rodzaju pola kwoty
   	    	m = ddlAmountREGEX.matcher(dane);
   	    	// zdefiniowana kwota
   	    	if(m.find())
   	    	{
   	    		maTransferAmount.setVisibility(View.GONE);
   	    		ddlAmount.setVisibility(View.VISIBLE);
   	    		
   	    		ArrayList<String> kwota = new ArrayList<String>();
   	    		
   	    		m = wartosciREGEX.matcher(m.group(1));
   	    		while(m.find())
   	    		{
   	    			// dodaj wszystkie wartosci z wyjatkiem --- Wybierz ---
   	    			if(! m.group(2).equals("0"))
   	    			{
   	    				kwota.add(m.group(3));
   	    				kwoty.put(m.group(3), m.group(2));
   	    			}
   	    		}
   	    		
   	    		// dodanie kwot do spinnera
   	    		if(kwota != null && kwota.size() > 0)
   	    		{
   	    			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, kwota);
   	    			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
   	    			ddlAmount.setAdapter(adapter);
   	    		}
   	    	}
   	    	// pokaz zwykle pole tekstowe
   	    	else
   	    	{
   	    		maTransferAmount.setVisibility(View.VISIBLE);
   	    		ddlAmount.setVisibility(View.GONE);
   	    	}
   	    	

   	    	// szukanie uwaga1
   	    	m = uwaga1REGEX.matcher(dane);
   	    	if(m.find()) uwaga1.setText(Html.fromHtml(m.group(1)));

   	    	// szukanie uwaga2
   	    	m = uwaga2REGEX.matcher(dane);
   	    	if(m.find()) uwaga2.setText(Html.fromHtml(m.group(1)));

   	    	// dostepne srodki
   	    	m = srodkiREGEX.matcher(dane);
   	    	if(m.find()) dostepne_srodki.setText(m.group(1));

   	    	// szukanie roku dtbTransferDate_year
   	    	m = Przelewy.dtbTransferDate_year_REGEX.matcher(dane);
   	    	if(m.find()) dtbTransferDate_year = m.group(1);

   	    	// szukanie miesiaca dtbTransferDate_month
   	    	m = Przelewy.dtbTransferDate_month_REGEX.matcher(dane);
   	    	if(m.find()) dtbTransferDate_month = m.group(1);
   	    	
   	    	// szukanie dnia dtbTransferDate_day
   	    	m = Przelewy.dtbTransferDate_day_REGEX.matcher(dane);
   	    	if(m.find()) dtbTransferDate_day = m.group(1);

   	    	// szukanie authTurnOff
   	    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
   	    	if(m.find()) authTurnOff = m.group(1);
   	    	
   	    	// szukanie limitow kwoty
   	    	m = limityREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
   	    		try
   	    		{
   	    			limit1 = Integer.parseInt(m.group(1));
   	    			limit2 = Integer.parseInt(m.group(2));
   	    		}catch(NumberFormatException e) {}
   	    	}
   	    	
   	    	// szukanie czy jest dostepna zmienna AddNumberGroup
   	    	if(dane.contains("<span class=\"radio\" disabled=\"disabled\">")) AddNumberGroup = false;
   	    	else AddNumberGroup = true;
   	    	
   	    	
   	    	// jezeli to 1 uruchomienie
   	    	if(odswiezanie == false)
   	    	{
   	   	    	// szukanie operatorow
   	   	    	m = operatorREGEX.matcher(dane);
   	   	    	if(m.find())
   	   	    	{
   	   	    		m = wartosciREGEX.matcher(m.group(1));
   	   	    		// szukanie poszczegolnych wartosci
   	   	    		while(m.find())
   	   	    		{
   	   		    		operator.add(m.group(3));
   	   		    		operatorzy.put(m.group(3), m.group(2));
   	   	    		}
   	   	    		
   	   	    		// dodanie operatorow do spinnera
   	   	    		if(operator != null && operator.size() > 0)
   	   	    		{
   	   	    			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, operator);
   	   	    			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
   	   	    			ddlOperatorList.setAdapter(adapter);
   	   	    		}
   	   	    	}
   	   	    	
   	   	    	// ustawienie sluchaczy na przyciski
   	  	    	przycisk_dalej.setOnClickListener(new OnClickListener()
   	   	    	{
   	   				@Override
   	   				public void onClick(View arg0) {
   	   					// jezeli dane zostaly wprowadzone poprawnie przejdz do krok2
   	   					if(sprawdzPoprawnoscDanych())
   	   					{
   	   						Bankoid.tworzProgressDialog(DoladujTelefon.this, getResources().getString(R.string.dialog_pobinfo));
   	   						Bankoid.dialog.show();
   	   						
   	   						watek = new Thread(DoladujTelefon.this, String.valueOf(Przelewy.DALEJ_KROK1));
   	   						watek.start();
   	   					}
   	   				}
   	   	    	});
   	   	    	
   	   	    	// dokonano zmiany operatora odswiez formularz
   	   	    	ddlOperatorList.setOnItemSelectedListener(new OnItemSelectedListener()
   	   	    	{

   	   				@Override
   	   				public void onItemSelected(AdapterView<?> arg0, View arg1,
   	   						int arg2, long arg3) {
   	   					if(wybrany_operator != -1)
   	   					{
   	   	    				Bankoid.tworzProgressDialog(DoladujTelefon.this, getResources().getString(R.string.dialog_pobinfo));
   	   	    				Bankoid.dialog.show();
   	   	    				
   	   	    				watek = new Thread(DoladujTelefon.this, String.valueOf(Przelewy.ODSWIEZANIE));
   	   	    				watek.start();    					
   	   					}
   	   					wybrany_operator = arg2;
   	   				}

   	   				@Override
   	   				public void onNothingSelected(AdapterView<?> arg0) {
   	   					
   	   				}
   	   	    	});  	   	    	
   	    	}
 
   	    	return true;
   	    }
   	    // czy wystapil standartowy blad
   	    if(Bankoid.bledy.czyBlad())
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
	   	    else Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
    public String odswiezFormularz()
    {
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/prepaid_handling.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
    	Spinner ddlOperatorList = (Spinner) this.findViewById(R.id.operator_konta);
    	request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
    	request.addParam("ddlOperatorList", operatorzy.getAsString((String) ddlOperatorList.getSelectedItem()));
    	request.addParam("AddNumberGroup", "rbAddNumberYes");

   	    request.execute();
    	    
   	    String rezultat = request.getResult();
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    private boolean sprawdzPoprawnoscDanych()
	{
    	EditText numer_telefonu = (EditText) this.findViewById(R.id.numer_telefonu);
    	EditText nazwa_konta = (EditText) this.findViewById(R.id.nazwa_konta);
    	EditText kwota_doladowania = (EditText) this.findViewById(R.id.kwota_doladowania);
    	// aktualnie wpisana kwota
    	BigDecimal kwota = null;
    	BigDecimal limit1_big = null;
    	BigDecimal limit2_big = null;
    	
		// sprawdzenie nr telefonu jezeli pole jest widoczne
		if(numer_telefonu.isShown() && numer_telefonu.getText().toString().length() != NUMER_DLUGOSC)
		{
			Toast.makeText(this.getApplicationContext(), R.string.doladuj_blad_nrtel, Toast.LENGTH_LONG).show();
			return false;
		}
		// sprawdzenie tytulu przelewu
		if(nazwa_konta.isShown() && nazwa_konta.getText().toString().length() == 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.doladuj_blad_konto, Toast.LENGTH_LONG).show();
			return false;
		}
		// sprawdzenie czy kwota zostala podana z dopuszczalnego zbioru
		if(kwota_doladowania.isShown())
		{
	    	try
			{
				kwota = new BigDecimal(kwota_doladowania.getText().toString());
				limit1_big = new BigDecimal(limit1);
				limit2_big = new BigDecimal(limit2);
			}catch(NumberFormatException e) {
				String bladFormatted = String.format(getResources().getString(R.string.doladuj_blad_kwota), limit1, limit2);
				Toast.makeText(this.getApplicationContext(), Html.fromHtml(bladFormatted), Toast.LENGTH_LONG).show();
				return false;
			}

			if(kwota.compareTo(limit1_big) == -1 || kwota.compareTo(limit2_big) == 1)
			{
				String bladFormatted = String.format(getResources().getString(R.string.doladuj_blad_kwota), limit1, limit2);
				Toast.makeText(this.getApplicationContext(), Html.fromHtml(bladFormatted), Toast.LENGTH_LONG).show();
				return false;
			}
		}
		// nie ma bledow
		return true;
	}
    
    // zwraca false jezeli nalezy przeladowac
	private String wykonajPrzelewKrok1()
	{	
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/prepaid_handling.aspx");
	    request.setMethod("POST");
	    request.addParam("dtbTransferDate_year", dtbTransferDate_year);
	    request.addParam("dtbTransferDate_month", dtbTransferDate_month);
	    request.addParam("dtbTransferDate_day", dtbTransferDate_day);
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", this.paramDalej);
	    request.addParam("__CurrentWizardStep", "1");
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("ddlOperatorList", operatorzy.getAsString((String) ddlOperatorList.getSelectedItem()));
	    request.addParam("tbTransferDescr", tbTransferDescr.getText().toString());
	    // kwota wpisana w formularz
	    if(maTransferAmount.isShown())
	    {
	    	request.addParam("maTransferAmount", maTransferAmount.getText().toString().replace(".", ","));
	    }
	    // kwota wybrana z listy
	    else
	    {
	    	request.addParam("maTransferAmount", "");
	    	request.addParam("maTransferAmount_Curr", "");
	    	request.addParam("ddlAmount", kwoty.getAsString((String) ddlAmount.getSelectedItem()));
	    }
	    if(AddNumberGroup)
	    {
	    	request.addParam("AddNumberGroup", "rbAddNumberYes");
	    }
	    
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
		    m = Przelewy.bladPrzelewREGEX.matcher(rezultat);
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

	// tworzy i pokazuje formularz z kodem sms
	private void tworzFormularzKrok2(String dane)
	{
    	// tworzenie dialogu z formularzem dla kroku 2
    	krok2 = new Dialog(this, R.style.CustomTheme);
		krok2.setContentView(R.layout.dialog_krok2);
		krok2.setTitle("Doładowanie telefonu, krok 2/2");

        krok2.getWindow().setFormat(PixelFormat.RGBA_8888);
        krok2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        
		Pattern bankiREGEX = Pattern.compile("<label class=\"label\">Bank odbiorcy</label><div class=\"content\">(.+?)</div>", Pattern.DOTALL);
		Pattern bankREGEX = Pattern.compile("<span>([^<]+)</span>");
		Pattern tytulREGEX = Pattern.compile("<label class=\"label\">Tytuł</label><div class=\"content\">.+?<span>(.+?)</span>", Pattern.DOTALL);
		Pattern rachunekREGEX = Pattern.compile("<label class=\"label\">Rachunek odbiorcy</label><div class=\"content\">.+?<span>(.+?)</span>", Pattern.DOTALL);
	
		//////////////////////////////////////////////
	
		TextView operator_konta_label = (TextView) krok2.findViewById(R.id.operator_konta_label);
		TextView operator_konta = (TextView) krok2.findViewById(R.id.operator_konta);
		TextView numer_telefonu_label = (TextView) krok2.findViewById(R.id.numer_telefonu_label);
		TextView numer_telefonu = (TextView) krok2.findViewById(R.id.numer_telefonu);
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
		
		operator_konta_label.setVisibility(View.VISIBLE);
		operator_konta.setVisibility(View.VISIBLE);
		numer_telefonu_label.setVisibility(View.VISIBLE);
		numer_telefonu.setVisibility(View.VISIBLE);
		
		// ustawianie tekstu dla nr tel i operatora
		operator_konta.setText((String) ddlOperatorList.getSelectedItem());
		numer_telefonu.setText(tbTransferDescr.getText().toString());
		
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
		m = Przelewy.smslabelREGEX.matcher(dane);
		if(m.find())
		{
			// jezeli opcja pobierania sms aktywna
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

		// szukanie parameters zatwierdz
		//m = Przelewy.zatwierdzREGEX.matcher(dane);
		//if(m.find()) paramZatwierdz = m.group(1);

		// szukanie parameters modyfikuj
		m = Przelewy.modyfikujREGEX.matcher(dane);
		if(m.find()) paramModyfikuj = m.group(1);

		// szukanie parameters potwierdz pozniej
		m = Przelewy.potwierdzpozniejREGEX.matcher(dane);
		if(m.find())
		{
			przycisk_potwierdz_pozniej.setVisibility(View.VISIBLE);
			paramPotwierdzPozniej = m.group(1);
		}else przycisk_potwierdz_pozniej.setVisibility(View.GONE);

		// szukanie tytulu przelewu
		m = tytulREGEX.matcher(dane);
		if(m.find()) tytul_przelewu.setText(m.group(1));

		// szukanie rachunku odbiorcy
		m = rachunekREGEX.matcher(dane);
		if(m.find()) rachunek_odbiorcy.setText(m.group(1));

    	// szukanie roku dtbTransferDate_year
    	m = Przelewy.dtbTransferDate_year_REGEX.matcher(dane);
    	if(m.find()) dtbTransferDate_year = m.group(1);

    	// szukanie miesiaca dtbTransferDate_month
    	m = Przelewy.dtbTransferDate_month_REGEX.matcher(dane);
    	if(m.find()) dtbTransferDate_month = m.group(1);
    	
    	// szukanie dnia dtbTransferDate_day
    	m = Przelewy.dtbTransferDate_day_REGEX.matcher(dane);
    	if(m.find()) dtbTransferDate_day = m.group(1);

    	// szukanie authTurnOff
    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
    	if(m.find()) authTurnOff = m.group(1);

		
		// kwota przelewu
	    if(maTransferAmount.isShown())
	    {
	    	kwota_przelewu.setText(maTransferAmount.getText().toString().replace(".", ",") + " PLN");
	    }
	    // kwota wybrana z listy
	    else
	    {
	    	kwota_przelewu.setText(((String) ddlAmount.getSelectedItem()) + " PLN");
	    }
		
		// w przypadku anulowania powrot do OperacjeRachunek
		krok2.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface krok2) {
				krok2.dismiss();
				DoladujTelefon.this.finish();
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
				Bankoid.tworzProgressDialog(DoladujTelefon.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(DoladujTelefon.this, String.valueOf(Przelewy.MODYFIKUJ_KROK2));
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
					Toast.makeText(DoladujTelefon.this, R.string.przelewy_blad_haslo, Toast.LENGTH_LONG).show();						
				}
				else
				{
					Bankoid.tworzProgressDialog(DoladujTelefon.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
					
					watek = new Thread(DoladujTelefon.this, String.valueOf(Przelewy.ZATWIERDZ_KROK2));
					watek.start();
				}
			}
		});

		
		// akcja dla przycisku zatwierdz pozniej
		przycisk_potwierdz_pozniej.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				Bankoid.tworzProgressDialog(DoladujTelefon.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(DoladujTelefon.this, String.valueOf(Przelewy.ZATWIERDZ_POZNIEJ_KROK2));
				watek.start();
			}
		});
	}
	
	// metoda wywolywana po kliknieciu modyfikuj
    private boolean modyfikujPrzelew()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/prepaid_handling.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__PARAMETERS", paramModyfikuj);
	    request.addParam("__CurrentWizardStep", "2");
	    request.addParam("TransactionType", TransactionType);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("ddlOperatorList", operatorzy.getAsString((String) ddlOperatorList.getSelectedItem()));
	    request.addParam("tbTransferDescr", tbTransferDescr.getText().toString());
	    request.addParam("dtbTransferDate_year", dtbTransferDate_year);
	    request.addParam("dtbTransferDate_month", dtbTransferDate_month);	    
	    request.addParam("dtbTransferDate_day", dtbTransferDate_day);
	    request.addParam("maTransferAmount", maTransferAmount.getText().toString().replace(".", ","));
	    request.addParam("maTransferAmount_Curr", "");
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
   	    	Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }

	// metoda wywolywana po kliknieciu potwierdz pozniej
    private boolean potwierdzPozniejPrzelew()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/account_details.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", paramPotwierdzPozniej);
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
    
	// metoda wywolywana po kliknieciu zatwierdz
    private void wykonajPrzelewKrok2()
    {

    	
		EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
    	
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/prepaid_handling.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__PARAMETERS", paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "2");
	    request.addParam("TransactionType", TransactionType);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("ddlOperatorList", operatorzy.getAsString((String) ddlOperatorList.getSelectedItem()));
	    request.addParam("tbTransferDescr", tbTransferDescr.getText().toString());
	    request.addParam("dtbTransferDate_year", dtbTransferDate_year);
	    request.addParam("dtbTransferDate_month", dtbTransferDate_month);	    
	    request.addParam("dtbTransferDate_day", dtbTransferDate_day);
	    // kwota wpisana w formularz
	    if(maTransferAmount.isShown())
	    {
	    	request.addParam("maTransferAmount", maTransferAmount.getText().toString().replace(".", ","));
	    }
	    // kwota wybrana z listy
	    else
	    {
	    	request.addParam("maTransferAmount", "");
	    	request.addParam("ddlAmount", kwoty.getAsString((String) ddlAmount.getSelectedItem()));
	    }
	    request.addParam("maTransferAmount_Curr", "");
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
   	    		OperacjeRachunek.odswiez = true;
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
	    // szukanie parametru dla przycisku Powrot
    	Matcher m = Przelewy.powrotREGEX.matcher(dane);
    	if(m.find()) paramPowrot = m.group(1);
    	
    	// szukanie roku dtbTransferDate_year
    	m = Przelewy.dtbTransferDate_year_REGEX.matcher(dane);
    	if(m.find()) dtbTransferDate_year = m.group(1);

    	// szukanie miesiaca dtbTransferDate_month
    	m = Przelewy.dtbTransferDate_month_REGEX.matcher(dane);
    	if(m.find()) dtbTransferDate_month = m.group(1);
    	
    	// szukanie dnia dtbTransferDate_day
    	m = Przelewy.dtbTransferDate_day_REGEX.matcher(dane);
    	if(m.find()) dtbTransferDate_day = m.group(1);
    	
    	// szukanie authTurnOff
    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
    	if(m.find()) authTurnOff = m.group(1);
    	
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/prepaid_handling.aspx");
	    request.setMethod("POST");
	    request.addParam("ddlOperatorList", operatorzy.getAsString((String) ddlOperatorList.getSelectedItem()));
	    request.addParam("dtbTransferDate_year", dtbTransferDate_year);
	    request.addParam("dtbTransferDate_month", dtbTransferDate_month);	    
	    request.addParam("dtbTransferDate_day", dtbTransferDate_day);
	    // kwota wpisana w formularz
	    if(maTransferAmount.isShown())
	    {
	    	request.addParam("maTransferAmount", maTransferAmount.getText().toString().replace(".", ","));
	    }
	    // kwota wybrana z listy
	    else
	    {
	    	request.addParam("maTransferAmount", "");
	    	request.addParam("ddlAmount", kwoty.getAsString((String) ddlAmount.getSelectedItem()));
	    }
	    request.addParam("maTransferAmount_Curr", "");
	    request.addParam("tbTransferDescr", tbTransferDescr.getText().toString());
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramPowrot);
	    request.addParam("__CurrentWizardStep", "3");
	    request.addParam("TransactionType", TransactionType);
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    
	    request.execute();
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    
		// szukanie parameters zatwierdz
		m = Przelewy.zatwierdzREGEX.matcher(rezultat);
	    // udalo sie przejsc do kroku 2 zwroc true
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
						
	                	Bankoid.tworzProgressDialog(DoladujTelefon.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(DoladujTelefon.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
