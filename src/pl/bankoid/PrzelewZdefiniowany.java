package pl.bankoid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PrzelewZdefiniowany extends Activity implements Runnable {

	private ContentValues waluty_typy = new ContentValues();
	private Thread watek;
	// krok2
	private Dialog krok2;
	private String haslo_sms_label = null;
	
	// REFERENCJE DO POL FORMULARZA I ZMIENNE
	private TextView lblTransferName;
	//private TextView ddlTransferType;
	private EditText tbTransferTitle;
	private DatePicker dtbTransferDate;
	private TextView SenderName;
	private TextView SenderAddress;
	private TextView SenderCity;
	private EditText tbAmount;
	private Spinner ddlTransferCurrSpinner;
	private String ddlTransferCurr = "";
	private TextView lblAvailBalance;
	private String lblAvailBalance_Curr = "";
	private String dtbTransferDate_year = "";
	private String dtbTransferDate_month = "";
	private String dtbTransferDate_day = "";
	private String authTurnOff = "";
	private String anRecAccount = "";
	private String lblRecName = "";
	private String lblRecAddress = "";
	private String lblRecCity = "";
	private String TransactionType = "";
	private String AddToBasketGroup = null;
	
	// parametr dla przyciskow
	private String paramDalej;
	private String paramZatwierdz;
	private String paramPotwierdzPozniej;
	private String paramModyfikuj;
	private String paramPowrot;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.przelew_zdefiniowany);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");

        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();

        this.setTitle("Przelew zdefiniowany, krok 1/2");
        
        // inicjalizacja zmiennych
        lblTransferName = (TextView) this.findViewById(R.id.nazwa_odbiorcy);
        //ddlTransferType = (TextView) this.findViewById(R.id.typ_przelewu);
    	tbTransferTitle = (EditText) this.findViewById(R.id.tytul_przelewu);
    	dtbTransferDate = (DatePicker) this.findViewById(R.id.data_operacji);
    	SenderName = (TextView) this.findViewById(R.id.nazwisko_nadawcy);
    	SenderAddress = (TextView) this.findViewById(R.id.adres_nadawcy);
    	SenderCity = (TextView) this.findViewById(R.id.miejscowosc_nadawcy);
    	tbAmount = (EditText) this.findViewById(R.id.kwota_przelewu);
    	tbAmount.setFilters(new InputFilter[]{new MoneyValueFilter(), new InputFilter.LengthFilter(15)});
    	ddlTransferCurrSpinner = (Spinner) this.findViewById(R.id.waluta_przelewu);
    	lblAvailBalance = (TextView) this.findViewById(R.id.dostepne_srodki);
        
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
					 PrzelewZdefiniowany.this.finish();
					 break;
    		
    			 case Przelewy.DALEJ_KROK1:
    				 // czy wystapil blad np nieprawidlowy nr konta
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(PrzelewZdefiniowany.this);
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
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(PrzelewZdefiniowany.this);
    				 else krok2.dismiss();
    				 break;
    				 
    			 case Przelewy.ZATWIERDZ_POZNIEJ_KROK2:
    				 // jezeli blad to pokaz inaczej przejdz do szczegoly rachunku
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(PrzelewZdefiniowany.this);
    				 else
    				 {
    					 krok2.dismiss();
    					 PrzelewZdefiniowany.this.finish();
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
    				 Bankoid.bledy.pokazKomunikat(PrzelewZdefiniowany.this);
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
    	Pattern rachunekREGEX = Pattern.compile("id=\"anRecAccount\" value=\"([^\"]+)\"");
    	Pattern nadawca_imieREGEX = Pattern.compile("id=\"lblRecName\" value=\"([^\"]+)\"");
    	Pattern nadawca_adresREGEX = Pattern.compile("id=\"lblRecAddress\" value=\"([^\"]+)\"");
    	Pattern nadawca_miastoREGEX = Pattern.compile("id=\"lblRecCity\" value=\"([^\"]+)\"");
    	//Pattern typREGEX = Pattern.compile("<option value=\"([^\"]+)\">Przelew jednorazowy</option>");
    	Pattern nazwa_odbiorcyREGEX = Pattern.compile("id=\"lblTransferName\" value=\"([^\"]+)\"");
    	Pattern tytulREGEX = Pattern.compile("id=\"tbTransferTitle\" class=\"[^\"]+\">(.+?)</textarea>");
    	Pattern walutyREGEX = Pattern.compile("<select name=\"ddlTransferCurr\" id=\"ddlTransferCurr\" class=\"currency\">.+?</select>", Pattern.DOTALL);
    	Pattern walutaREGEX = Pattern.compile("<option (selected=\"selected\" )?value=\"([^\"]+)\">([^<]+)</option>");
    	Pattern srodkiREGEX = Pattern.compile("id=\"lblAvailBalance_maBalance\" value=\"([^\"]+)\"");
    	Pattern srodki_walutaREGEX = Pattern.compile("id=\"lblAvailBalance_maBalance_Curr\" value=\"([^\"]+)\"");
    	Pattern nazwiskoREGEX = Pattern.compile("id=\"SenderName\" value=\"([^\"]+)\" />");
    	Pattern adresREGEX = Pattern.compile("id=\"SenderAddress\" value=\"([^\"]+)\" />");
    	Pattern miastoREGEX = Pattern.compile("id=\"SenderCity\" value=\"([^\"]+)\" />");
    	Pattern kwotaREGEX = Pattern.compile("<input name=\"tbAmount\" type=\"text\" value=\"([^\"]+)\"");
	    	
    	Button powrot = (Button) this.findViewById(R.id.przycisk_powrot);
    	Button dalej = (Button) this.findViewById(R.id.przycisk_dalej);
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
    		return false;
    	}

    	// szukanie parametru dal przycisku dalej
    	Matcher m = Przelewy.dalejREGEX.matcher(dane);
    	if(m.find()) 
    	{
    		paramDalej = m.group(1);
    		
        	// szukanie rachunku odbiorcy
        	m = rachunekREGEX.matcher(dane);
        	if(m.find()) anRecAccount = m.group(1);
        	
        	// szukanie imienia nadawcy
        	m = nadawca_imieREGEX.matcher(dane);
        	if(m.find()) lblRecName = m.group(1);
        	
        	// szukanie adresu nadawcy
        	m = nadawca_adresREGEX.matcher(dane);
        	if(m.find()) lblRecAddress = m.group(1);

        	// szukanie miasta nadawcy
        	m = nadawca_miastoREGEX.matcher(dane);
        	if(m.find()) lblRecCity = m.group(1);
        	
        	// szukanie nazwy odbiorcy
        	m = nazwa_odbiorcyREGEX.matcher(dane);
        	if(m.find()) lblTransferName.setText(m.group(1));
        	
        	// szukanie typu przelewu
    	    /*m = typREGEX.matcher(dane);
    	    if(m.find()) ddlTransferType.setText(m.group(1));*/
    	    
    	    // szukanie tytulu przelewu
    	    m = tytulREGEX.matcher(dane);
    	    if(m.find()) tbTransferTitle.setText(m.group(1));
        	
    	    // szukanie nazwiska
    	    m = nazwiskoREGEX.matcher(dane);
    	    if(m.find()) SenderName.setText(m.group(1));
    	    	
    	    // szukanie adresu
    	    m = adresREGEX.matcher(dane);
    	    if(m.find()) SenderAddress.setText(m.group(1));
    	    	
    	    // szukanie miejscowosci
    	    m = miastoREGEX.matcher(dane);
    	    if(m.find()) SenderCity.setText(m.group(1));
        	
    	    // szukanie kwoty przelewu
    	    m = kwotaREGEX.matcher(dane);
    	    if(m.find()) tbAmount.setText(m.group(1).replace(",", "."));
    	    
    	    // szukanie waluty w jakiej podano dostepne srodki
    	    m = srodki_walutaREGEX.matcher(dane);
    	    if(m.find()) lblAvailBalance_Curr = m.group(1);
    	    
        	// wpisanie dostepnych srodkow + waluta
        	m = srodkiREGEX.matcher(dane);
        	if(m.find()) lblAvailBalance.setText(m.group(1) + " " + lblAvailBalance_Curr);
    	    	
        	// szukanie roku dtbTransferDate_year
        	m = Przelewy.dtbTransferDate_year_REGEX.matcher(dane);
        	if(m.find()) dtbTransferDate_year = m.group(1);

        	// szukanie miesiaca dtbTransferDate_month
        	m = Przelewy.dtbTransferDate_month_REGEX.matcher(dane);
        	if(m.find()) dtbTransferDate_month = m.group(1);
        	
        	// szukanie dnia dtbTransferDate_day
        	m = Przelewy.dtbTransferDate_day_REGEX.matcher(dane);
        	if(m.find()) dtbTransferDate_day = m.group(1);
        	
        	// szukanie AddToBasketGroup
        	if(dane.contains("AddToBasketGroup")) AddToBasketGroup = "rbAddToBasketNo";
        	else AddToBasketGroup = null;
         	
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
        	
        	// szukanie authTurnOff
        	m = Przelewy.authTurnOff_REGEX.matcher(dane);
        	if(m.find()) authTurnOff = m.group(1);
        	
        	// szukanie tresci z rodzajami walut
        	m = walutyREGEX.matcher(dane);
        	if(m.find())
    	    {
        		int index = 0;
        		int waluta_id = 0;
        		String tekst = m.group();
        		ArrayList<String> wartosci = new ArrayList<String>();
        		m = walutaREGEX.matcher(tekst);
        		// poszczegolne wartosci
        		while(m.find())
        		{
        			String opis = Html.fromHtml(m.group(3)).toString().trim();
        			waluty_typy.put(opis, m.group(2));
        			wartosci.add(opis);
        			if(m.group(1) != null) waluta_id = index;
        			index++;
        		}
    	    		
        		// ustawienie na spinnerze walut przelewu
        		if(wartosci != null && wartosci.size() > 0)
        		{
        			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wartosci);
        			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        			ddlTransferCurrSpinner.setAdapter(adapter);
        			// zaznaczenie waluty ktora byla domyslnie zaznaczona
        			ddlTransferCurrSpinner.setSelection(waluta_id);
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
    					Bankoid.tworzProgressDialog(PrzelewZdefiniowany.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(PrzelewZdefiniowany.this, String.valueOf(Przelewy.DALEJ_KROK1));
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
	   	    else Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
   	    }

   	    return false;
    }
    
    private boolean sprawdzPoprawnoscDanych()
	{

		// sprawdzenie tytulu przelewu
		if(tbTransferTitle.getText().toString().length() == 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_tytul, Toast.LENGTH_LONG).show();
			return false;
		}
		// sprawdzenie wprowadzonej daty
		Calendar dzisiaj = Calendar.getInstance();
		Calendar maksymalna = Calendar.getInstance();
		maksymalna.add(Calendar.YEAR, 2);
		Calendar wprowadzona_data = Calendar.getInstance();
		wprowadzona_data.set(dtbTransferDate.getYear(), dtbTransferDate.getMonth(), dtbTransferDate.getDayOfMonth());
	
		if(wprowadzona_data.compareTo(dzisiaj) == -1 || wprowadzona_data.compareTo(maksymalna) == 1)
		{
			Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_data, Toast.LENGTH_LONG).show();
			return false;
		}
		
		// sprawdzenie kwoty
		if(tbAmount.getText().toString().length() == 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.przelewy_blad_kwota, Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}

    // zwraca false jezeli nalezy przeladowac
	private String wykonajPrzelewKrok1()
	{
		ddlTransferCurr = waluty_typy.getAsString((String)ddlTransferCurrSpinner.getSelectedItem());	    	
	
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/defined_transfer_exec.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("toggleData", "");
	    request.addParam("anRecAccount", anRecAccount);
	    request.addParam("lblRecName", lblRecName);
	    request.addParam("lblRecAddress", lblRecAddress);
	    request.addParam("lblRecCity", lblRecCity);
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramDalej);	    
	    request.addParam("__CurrentWizardStep", "1");
	    if(TransactionType != null) request.addParam("TransactionType", TransactionType);
	    request.addParam("__STATE", Bankoid.state);	    
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("lblTransferName", lblTransferName.getText().toString());
	    request.addParam("SenderName", SenderName.getText().toString());
	    request.addParam("SenderAddress", SenderAddress.getText().toString());
	    request.addParam("SenderCity", SenderCity.getText().toString());
	    request.addParam("lblAvailBalance_maBalance", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maBalance_Curr", lblAvailBalance_Curr);
	    request.addParam("lblAvailBalance_maOwnFund", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maOwnFund_Curr", "");
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("rllGroup", "TransferTypeRadioLinkList_rbStandardTransfer");
	    //request.addParam("ddlTransferType", ddlTransferType.getText().toString());	    
	    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
	    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
	    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
	    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
	    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));	    
	    request.addParam("ddlTransferCurr", ddlTransferCurr);
	    if(AddToBasketGroup != null) request.addParam("AddToBasketGroup", AddToBasketGroup);
	    
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
		    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.ZAMKNIJ_OKNO);

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
		krok2.setTitle("Przelew zdefiniowany, krok 2/2");

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
		m = Przelewy.smslabelREGEX.matcher(dane);
		if(m.find())
		{
			// jezeli wersja bez reklam
			if(Bankoid.pref_sms)
				registerReceiver(new SMSReceiver(m.group(1), haslo_sms), new IntentFilter(SMSReceiver.SMS_RECEIVED));

			haslo_sms_label.setText(Html.fromHtml(m.group(1)).toString().trim());
			
			haslo_sms_label.setVisibility(View.VISIBLE);
			haslo_sms.setVisibility(View.VISIBLE);
			tresc_stopka.setVisibility(View.VISIBLE);
		}
		// jezeli nie znaleziono tzn ze nie potrzeba autoryzacji na kod sms
		else
		{
			haslo_sms_label.setVisibility(View.GONE);
			haslo_sms.setVisibility(View.GONE);
			tresc_stopka.setVisibility(View.GONE);
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

    	// szukanie authTurnOff
    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
    	if(m.find()) authTurnOff = m.group(1);
    	
		// szukanie parameters modyfikuj
		m = Przelewy.modyfikujREGEX.matcher(dane);
		if(m.find()) paramModyfikuj = m.group(1);
		
    	// szukanie AddToBasketGroup
    	if(dane.contains("AddToBasketGroup")) AddToBasketGroup = "rbAddToBasketNo";
    	else AddToBasketGroup = null;
		
		// szukanie parameters potwierdz pozniej
		m = Przelewy.potwierdzpozniejREGEX.matcher(dane);
		if(m.find())
		{
			paramPotwierdzPozniej = m.group(1);
			przycisk_potwierdz_pozniej.setVisibility(View.VISIBLE);
		}
		else przycisk_potwierdz_pozniej.setVisibility(View.GONE);
		
		tytul_przelewu.setText(this.tbTransferTitle.getText().toString().replaceAll("\\n", ""));
		rachunek_odbiorcy.setText(anRecAccount);
		kwota_przelewu.setText(this.tbAmount.getText().toString().replace(".", ",") + " " + this.ddlTransferCurr);
	
		// w przypadku anulowania powrot do OdbiorcyZdefiniowani
		krok2.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface krok2) {
				krok2.dismiss();
				PrzelewZdefiniowany.this.finish();
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
				Bankoid.tworzProgressDialog(PrzelewZdefiniowany.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(PrzelewZdefiniowany.this, String.valueOf(Przelewy.MODYFIKUJ_KROK2));
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
					Toast.makeText(PrzelewZdefiniowany.this, R.string.przelewy_blad_haslo, Toast.LENGTH_LONG).show();						
				}
				else
				{
					Bankoid.tworzProgressDialog(PrzelewZdefiniowany.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
					
					watek = new Thread(PrzelewZdefiniowany.this, String.valueOf(Przelewy.ZATWIERDZ_KROK2));
					watek.start();
				}
			}
		});

		// akcja dla przycisku zatwierdz pozniej
		przycisk_potwierdz_pozniej.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				Bankoid.tworzProgressDialog(PrzelewZdefiniowany.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(PrzelewZdefiniowany.this, String.valueOf(Przelewy.ZATWIERDZ_POZNIEJ_KROK2));
				watek.start();
			}
		});
	}
	
	// metoda wywolywana po kliknieciu modyfikuj
    private boolean modyfikujPrzelew()
    {
    	EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
    	
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/defined_transfer_exec.aspx");
	    request.setMethod("POST");
	    
	    //request.addParam("ddlTransferType", ddlTransferType.getText().toString());	  
	    request.addParam("lblAvailBalance_maBalance", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maBalance_Curr", lblAvailBalance_Curr);
	    request.addParam("lblAvailBalance_maOwnFund", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maOwnFund_Curr", "");
	    request.addParam("chbEmailConfirmationMain", "off");
	    request.addParam("chbEmailConfirmationSecondary", "off");
	    request.addParam("__PARAMETERS", paramModyfikuj);
	    request.addParam("__CurrentWizardStep", "2");
	    request.addParam("TransactionType", TransactionType);
	    request.addParam("__STATE", Bankoid.state);	    
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("lblTransferName", lblTransferName.getText().toString());
	    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
	    request.addParam("anRecAccount", anRecAccount);
	    request.addParam("lblRecName", lblRecName);
	    request.addParam("lblRecAddress", lblRecAddress);
	    request.addParam("lblRecCity", lblRecCity);
	    request.addParam("SenderName", SenderName.getText().toString());
	    request.addParam("SenderAddress", SenderAddress.getText().toString());
	    request.addParam("SenderCity", SenderCity.getText().toString());
	    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));	  
	    request.addParam("tbAmount_Curr", "");
	    request.addParam("ddlTransferCurr", ddlTransferCurr);
	    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
	    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
	    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    // przelew z potwierdzeniem sms
	    if(haslo_sms.isShown()) request.addParam("authCode", "");
	    else request.addParam("authTurnOff", authTurnOff);
	    if(AddToBasketGroup != null) request.addParam("AddToBasketGroup", AddToBasketGroup);
	    
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
	    request.setUrl("https://www.mbank.com.pl/defined_transfers_list.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", this.paramPotwierdzPozniej);
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
    	// sprawdzenie czy udalo sie wrocic do szczegolow rachunku	
	    if(rezultat.contains("Twoja nazwa odbiorcy")) return true;
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
	    request.setUrl("https://www.mbank.com.pl/defined_transfer_exec.aspx");
	    request.setMethod("POST");
	    
	    //request.addParam("ddlTransferType", ddlTransferType.getText().toString());	  
	    request.addParam("lblAvailBalance_maBalance", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maBalance_Curr", lblAvailBalance_Curr);
	    request.addParam("lblAvailBalance_maOwnFund", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maOwnFund_Curr", "");
	    request.addParam("chbEmailConfirmationMain", "off");
	    request.addParam("chbEmailConfirmationSecondary", "off");
	    request.addParam("__PARAMETERS", paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "2");
	    request.addParam("TransactionType", TransactionType);
	    request.addParam("__STATE", Bankoid.state);	    
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("lblTransferName", lblTransferName.getText().toString());
	    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
	    request.addParam("anRecAccount", anRecAccount);
	    request.addParam("lblRecName", lblRecName);
	    request.addParam("lblRecAddress", lblRecAddress);
	    request.addParam("lblRecCity", lblRecCity);
	    request.addParam("SenderName", SenderName.getText().toString());
	    request.addParam("SenderAddress", SenderAddress.getText().toString());
	    request.addParam("SenderCity", SenderCity.getText().toString());
	    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));	  
	    request.addParam("tbAmount_Curr", "");
	    request.addParam("ddlTransferCurr", ddlTransferCurr);
	    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
	    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
	    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    // przelew z potwierdzeniem sms
	    if(haslo_sms.isShown()) request.addParam("authCode", haslo_sms.getText().toString());
	    else request.addParam("authTurnOff", authTurnOff);
	    if(AddToBasketGroup != null) request.addParam("AddToBasketGroup", AddToBasketGroup);
	    
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
    	Pattern activitydataREGEX = Pattern.compile("id=\"Activity_Data\" value=\"([^\"]+)\"");
    	Pattern pdataREGEX = Pattern.compile("id=\"__PDATA\" value=\"([^\"]+)\"");
    	Pattern rangepanelREGEX = Pattern.compile("id=\"rangepanel_group\" value=\"([^\"]+)\"");
    	 

    	String Activity_Data = null;
    	String __PDATA = null;
    	String rangepanel_group = null;
    	//String paramPowrot = null;


    	Matcher m = activitydataREGEX.matcher(dane);
    	if(m.find()) Activity_Data = m.group(1);
    	
    	m = pdataREGEX.matcher(dane);
    	if(m.find()) __PDATA = m.group(1);
    	
    	m = rangepanelREGEX.matcher(dane);
    	if(m.find()) rangepanel_group = m.group(1);
    	
    	m = Przelewy.powrotREGEX.matcher(dane);
    	if(m.find()) paramPowrot = m.group(1);
    	
    	// szukanie authTurnOff
    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
    	if(m.find()) authTurnOff = m.group(1);
    	
       	// szukanie AddToBasketGroup
       	if(dane.contains("AddToBasketGroup")) AddToBasketGroup = "rbAddToBasketNo";
       	else AddToBasketGroup = null;
    	
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/defined_transfer_exec.aspx");
	    request.setMethod("POST");

	    if(rangepanel_group != null) request.addParam("rangepanel_group", rangepanel_group);

	    if(__PDATA != null) request.addParam("__PDATA", __PDATA);
	      
	    request.addParam("tbTransferTitle", tbTransferTitle.getText().toString().replaceAll("\\n", ""));
	    request.addParam("dtbTransferDate_day", String.valueOf(dtbTransferDate.getDayOfMonth()));
	    request.addParam("dtbTransferDate_month", String.valueOf(dtbTransferDate.getMonth()+1));
	    request.addParam("dtbTransferDate_year", String.valueOf(dtbTransferDate.getYear()));
	    request.addParam("tbAmount", tbAmount.getText().toString().replace(".", ","));	  
	    request.addParam("tbAmount_Curr", "");
	    request.addParam("ddlTransferCurr", ddlTransferCurr);
	    if(Activity_Data != null) request.addParam("Activity_Data", Activity_Data);
	    //else request.addParam("ddlTransferType", ddlTransferType.getText().toString());
	    request.addParam("lblTransferName", lblTransferName.getText().toString());
	    request.addParam("lblAvailBalance_maBalance", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maBalance_Curr", lblAvailBalance_Curr);
	    request.addParam("lblAvailBalance_maOwnFund", lblAvailBalance.getText().toString().substring(0, lblAvailBalance.getText().toString().indexOf(" ")));
	    request.addParam("lblAvailBalance_maOwnFund_Curr", "");
	    request.addParam("chbEmailConfirmationMain", "off");
	    request.addParam("chbEmailConfirmationSecondary", "off");
	    request.addParam("anRecAccount", anRecAccount);
	    request.addParam("lblRecName", lblRecName);
	    request.addParam("lblRecAddress", lblRecAddress);
	    request.addParam("lblRecCity", lblRecCity);
	    request.addParam("SenderName", SenderName.getText().toString());
	    request.addParam("SenderAddress", SenderAddress.getText().toString());
	    request.addParam("SenderCity", SenderCity.getText().toString());
	    request.addParam("authTurnOff", authTurnOff);
	    if(AddToBasketGroup != null) request.addParam("AddToBasketGroup", AddToBasketGroup);
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
						
	                	Bankoid.tworzProgressDialog(PrzelewZdefiniowany.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(PrzelewZdefiniowany.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
