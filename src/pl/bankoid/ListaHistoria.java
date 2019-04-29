package pl.bankoid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class ListaHistoria extends ListActivity implements Runnable {

	private ArrayList<Historia> m_historia = new ArrayList<Historia>();
	private AdapterHistoria m_adapter = null;
	private Dialog parametry;
	// dane do okna dialogowego
	private ContentValues okres_typy = new ContentValues();
	private ContentValues rodzaje_operacji = new ContentValues();
	// wybrany radiobox
	private int radio = 0;
	private String plikPDF;
	private Thread watek;
	private AdView adView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_historii);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");
    	
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

        // reklama
        if(Bankoid.reklamy)
        {
        	adView = new AdView(this, AdSize.BANNER, Bankoid.ADMOB_ID);
            FrameLayout layout = (FrameLayout) this.findViewById(R.id.ad);
            layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            layout.addView(adView);
            layout.setVisibility(View.VISIBLE);
            adView.loadAd(Bankoid.adr);
        }
        ////////////////////
        
        // usuwanie dividera pomiedzy kolejnymi wierszami
        getListView().setDivider(null); 
        getListView().setDividerHeight(0);
        
    	// automatycznie zamknij okno jezeli nie jest sie zalogowanym
    	if(Bankoid.zalogowano == false) this.finish();
        
    	// jezeli nie udalo sie otworzyc okno z param hisotrii
        if(tworzParamHistoria(dane) == false) Bankoid.bledy.pokazKomunikat(this);

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
	        
	        // pobieranie historii z serwera z danymi z formularza
			case Bankoid.ACTIVITY_POBIERANIE_HISTORII:
				pobieranieHistorii();
				handler.sendEmptyMessage(Bankoid.ACTIVITY_POBIERANIE_HISTORII);
				break;
    	}
    }

    
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		
    		try
    		{
    			Bankoid.dialog.dismiss();
    		}catch(Exception e){}
    		
    		switch(msg.what)
    		{
    			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
					 ListaHistoria.this.finish();
					 break;
    		
    			case Bankoid.ACTIVITY_POBIERANIE_HISTORII:
    	    		// jezeli wystabil blad podczas pobierania historii to go pokaz
    	    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ListaHistoria.this);
    	    		else if(plikPDF != null)
    	    		{
    	            	Komunikat k = new Komunikat(ListaHistoria.this);
    	            	k.ustawTresc(plikPDF);
    	            	k.ustawPrzyciskTak("Zamknij", new OnClickListener()
    	            	{

							@Override
							public void onClick(View v) {
								parametry.dismiss();
								ListaHistoria.this.finish();
							}
    	            		
    	            	});
    	            	k.show();
    	    		}
    	        	// inaczej dodaj historie wynikow i ja pokaz
    	        	else
    	        	{
    	        	    m_adapter = new AdapterHistoria(getApplicationContext(), R.layout.wiersz_historia, m_historia);
    	        	    setListAdapter(m_adapter);

    	        		// zamkniecie okna dialogowego z parametrami historii
    	    			parametry.dismiss();

    	        		// ustawienie tytulu
    	           	    ListaHistoria.this.setTitle("Lista operacji wykonanych");
    	        	}
    				break;
    		}
    	}
    };
    
	// tworzy i pokazuje dialog z parametrami historii
    public boolean tworzParamHistoria(String dane)
    {
	    parametry = new Dialog(this, R.style.CustomTheme);
   	    parametry.setContentView(R.layout.dialog_historia);
   	    parametry.getWindow().setFormat(PixelFormat.RGBA_8888);
   	    parametry.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
   	    
   	    // REGEX
    	Pattern okresyREGEX = Pattern.compile("<select name=\"lastdays_period\".+?</select>", Pattern.DOTALL);
    	Pattern rodzajeREGEX = Pattern.compile("<select name=\"accoperlist_typefilter_group\".+?</select>", Pattern.DOTALL);
    	Pattern wartosciREGEX = Pattern.compile("<option (selected=\"selected\" )?value=\"([^\"]+)\">([^<]+)</option>");
    	Pattern walutaREGEX = Pattern.compile("class=\"text amount\" />(\\w+)");
   	    
    	// ZMIENNE
    	EditText kwota_od = (EditText) parametry.findViewById(R.id.kwota_od);
    	kwota_od.setFilters(new InputFilter[]{new MoneyValueFilter(), new InputFilter.LengthFilter(15)});
    	EditText kwota_do = (EditText) parametry.findViewById(R.id.kwota_do);
    	kwota_do.setFilters(new InputFilter[]{new MoneyValueFilter(), new InputFilter.LengthFilter(15)});
    	
    	Calendar kalendarz_od = Calendar.getInstance();
    	kalendarz_od.add(Calendar.MONTH, -1);
    	Calendar kalendarz_do = Calendar.getInstance();
    	final RadioButton radio_zakres_dat = (RadioButton) parametry.findViewById(R.id.radio_zakres_dat);
    	final RadioButton radio_okres = (RadioButton) parametry.findViewById(R.id.radio_okres);
    	final RadioButton radio_ostatnie = (RadioButton) parametry.findViewById(R.id.radio_ostatnie);
    	DatePicker data_od = (DatePicker) parametry.findViewById(R.id.data_od);
    	DatePicker data_do = (DatePicker) parametry.findViewById(R.id.data_do);
    	Button przycisk_zatwierdz = (Button) parametry.findViewById(R.id.przycisk_zatwierdz);
    	Button przycisk_warunki = (Button) parametry.findViewById(R.id.przycisk_warunki);
    	
    	data_od.init(kalendarz_od.get(Calendar.YEAR), kalendarz_od.get(Calendar.MONTH), kalendarz_od.getActualMinimum(Calendar.DAY_OF_MONTH), null);
    	data_do.init(kalendarz_do.get(Calendar.YEAR), kalendarz_do.get(Calendar.MONTH), kalendarz_do.get(Calendar.DAY_OF_MONTH), null);

    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.historia_blad, Bledy.WYLOGUJ);
    		return false;
    	}
    	
   	    // udalo sie otworzyc okno z parametrami historii
   	    if(dane.contains("Operacje z okresu"))
   	    {
   	    	// w przypadku anulowania parametrow historii powrot do menuglowne
   	    	parametry.setOnCancelListener(new DialogInterface.OnCancelListener()
   	    	{
				@Override
				public void onCancel(DialogInterface oknoparam) {
					oknoparam.dismiss();
					ListaHistoria.this.finish();
				}
   	    		
   	    	});
   	    	
   	    	//odczytanie waluty rachunku
   	    	Matcher m = walutaREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
   	    		TextView label_pln = (TextView) parametry.findViewById(R.id.label_pln);
   	    		label_pln.setText(m.group(1));
   	    		label_pln = (TextView) parametry.findViewById(R.id.label_pln2);
   	    		label_pln.setText(m.group(1));
   	    	}
   	    	
   	    	
   	    	// okresy
   	    	Spinner s = (Spinner) parametry.findViewById(R.id.okres_typ);
   	    	
   	    	// szukanie okresow
   	    	m = okresyREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
   	    		String tekst = m.group();
   	    		ArrayList<String> wartosci_okresy = new ArrayList<String>();
   	    		m = wartosciREGEX.matcher(tekst);
   	    		// poszczegolne wartosci
   	    		while(m.find())
   	    		{
   	    			String opis = Html.fromHtml(m.group(3)).toString().trim();
   	    			okres_typy.put(opis, m.group(2));
   	    			wartosci_okresy.add(opis);
   	    		}
   	    		
   	    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wartosci_okresy);
   	    		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
   	    		if(adapter != null) s.setAdapter(adapter);
   	    	}
   	    	
   	    	s = (Spinner) parametry.findViewById(R.id.rodzaj_operacji);
   	    	
   	    	// szukanie rodzaje operacji
   	    	m = rodzajeREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
   	    		String tekst = m.group();
   	    		ArrayList<String> wartosci_rodzaje = new ArrayList<String>();
   	    		m = wartosciREGEX.matcher(tekst);
   	    		// poszczegolne wartosci
   	    		while(m.find())
   	    		{
   	    			String wartosc = m.group(2);
   	    			if(! wartosc.contains("-"))
   	    			{
	   	    			String opis = Html.fromHtml(m.group(3)).toString().trim();
	   	    			rodzaje_operacji.put(opis, wartosc);
	   	    			wartosci_rodzaje.add(opis);
   	    			}
	   	    	}
   	    		
   	    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wartosci_rodzaje);
   	    		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
   	    		if(adapter != null) s.setAdapter(adapter);
   	    		
   	    	}
   	    	
   	    	// odznaczanie poszczegolnych radio
   	    	radio_zakres_dat.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v) {
   					radio = 0;
   					radio_okres.setChecked(false);
   					radio_ostatnie.setChecked(false);
   				}
   	    	});

   	    	radio_okres.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v) {
   					radio = 1;
   					radio_zakres_dat.setChecked(false);
   					radio_ostatnie.setChecked(false);
   				}
   	    	});

   	    	radio_ostatnie.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v) {
   					radio = 2;
   					radio_zakres_dat.setChecked(false);
   					radio_okres.setChecked(false);
   				}
   	    	});

   	    	przycisk_zatwierdz.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v)
   				{
   					// jezeli nie ma bledow w formularzu to pobierz historie
   					if(sprawdzPoprawnoscDanych(radio))
   					{
   	   					Bankoid.tworzProgressDialog(ListaHistoria.this, getResources().getString(R.string.dialog_pobinfo));
   	   					Bankoid.dialog.show();
   						{
   	   	   					watek = new Thread(ListaHistoria.this, String.valueOf(Bankoid.ACTIVITY_POBIERANIE_HISTORII));
   	   	   					watek.start();
   						}
   					}
   				}
   	    		
   	    	});
   	    	
   	    	// pokazuje ukryte pola szczegolow historii
   	    	przycisk_warunki.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v)
   				{
   					v.setVisibility(View.GONE);
   					((RelativeLayout) parametry.findViewById(R.id.warunki_dodatkowe)).setVisibility(View.VISIBLE);
   				}
   	    		
   	    	});
   	    	
   	    	parametry.setTitle("Kryteria wyświetlania operacji wykonanych");
   	    	parametry.show();
   	    	
   	    	return true;
   	    }
   	    // zdiagnozowanie bledu
   	    else if(Bankoid.bledy.czyBlad())
   	    {
   	    	Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
   	    }
   	    // nieokreslony blad wyloguj
   	    else
   	    {
   	    	Bankoid.bledy.ustawBlad(R.string.historia_blad, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
    // pobiera historie z podanymi parametrami
    public void pobierzHistorie(
    		String rangepanel_group,
    		String daterange_from_day,
    		String daterange_from_month,
    		String daterange_from_year,
    		String daterange_to_day,
    		String daterange_to_month,
    		String daterange_to_year,
    		String lastdays_days,
    		String lastdays_period,
    		String accoperlist_typefilter_group,
    		String accoperlist_amountfilter_amountmin,
    		String accoperlist_amountfilter_amountmax,
    		String export_oper_history_format)
    {
    	
    	//Log.v("pobieranie", "historia");
    	Pattern historiaREGEX = Pattern.compile("<li( class=\"alternate\")?><p class=\"Date\"><span id=[^>]+>([^<]+)</span><span id=[^>]+>([^<]+)</span>.+?'([^']{100,})'[^>]+>([^<]+)</a><span>(.+?)</span>(<span>.+?</span>)?(<span>.+?</span>)?<span class=\"FilterType\">.*?</span></p><p class=\"Amount\"><span[^>]+>([^<]+)</span></p><p class=\"Amount\"><span[^>]+>([^<]+)</span>", Pattern.DOTALL);

    	sfRequest request = sfClient.getInstance().createRequest();
    	if(export_oper_history_format != null) request.setUrl("https://www.mbank.com.pl/printout_oper_list.aspx");
    	else request.setUrl("https://www.mbank.com.pl/account_oper_list.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.addParam("__VIEWSTATE", "");
   	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
   	    request.addParam("rangepanel_group", rangepanel_group);
   	    if(rangepanel_group.equals("daterange_radio"))
   	    {
   	    	request.addParam("daterange_from_day", daterange_from_day);
   	    	request.addParam("daterange_from_month", daterange_from_month);
   	    	request.addParam("daterange_from_year", daterange_from_year);
   	    	request.addParam("daterange_to_day", daterange_to_day);
   	    	request.addParam("daterange_to_month", daterange_to_month);
   	    	request.addParam("daterange_to_year", daterange_to_year);
   	    }
   	    else if(rangepanel_group.equals("lastdays_radio"))
   	    {
       	    request.addParam("lastdays_days", lastdays_days);
       	    request.addParam("lastdays_period", lastdays_period);
   	    }
   	    request.addParam("accoperlist_typefilter_group", accoperlist_typefilter_group);
   	    request.addParam("accoperlist_amountfilter_amountmin", accoperlist_amountfilter_amountmin);
   	    request.addParam("accoperlist_amountfilter_amountmax", accoperlist_amountfilter_amountmax);
   	    if(export_oper_history_format != null)
   	    {
   	    	request.addParam("export_oper_history_check", "on");
   	    	request.addParam("export_oper_history_format", export_oper_history_format);
   	    }
   	    else request.addParam("export_oper_history_format", "PDF");
   	    
   	    if(export_oper_history_format != null)
   	    {
   	    	request.result = null;
   	    	plikPDF = request.pobierzPlik();
   	    	if(plikPDF == null) plikPDF = getString(R.string.plik_blad);
   	    }
   	    else
   	    {
   	   	    request.execute();
   	   	    
   	  	    String rezultat = request.getResult();
    	    
   	   	    Matcher m = historiaREGEX.matcher(rezultat);
   	   	    while(m.find())
   	   	    {
   	   	    	Historia h = new Historia();
   	   	    	h.ustawDateOperacji(m.group(2));
   	   	    	h.ustawDateKsiegowania(m.group(3));
   	   	    	h.ustawRodzajPrzelewu(m.group(5));
   	   	    	h.ustawDaneNadawcy(m.group(6));
   	   	    	h.ustawRachunekNadawcy(m.group(7));
   	   	    	h.ustawTytulPrzelewu(m.group(8));
   	   	    	h.ustawKwoteOperacji(m.group(9));
   	   	    	h.ustawSaldoPoOperacji(m.group(10));
   	   	    	
   	   	    	m_historia.add(h);
   	   	    }
   	    	    
   	   	    Bankoid.pobierzState(rezultat);
   	   	    Bankoid.pobierzEventvalidation(rezultat);
   	    }

    }
    
    // metoda pobiera dante z formularza i wysyla do metody pobierzHistorie()
    private void pobieranieHistorii()
    {
    	// referencje do obiektow formularza
    	DatePicker data_od = (DatePicker) parametry.findViewById(R.id.data_od);
    	DatePicker data_do = (DatePicker) parametry.findViewById(R.id.data_do);
    	EditText okres_wartosc = (EditText) parametry.findViewById(R.id.okres_wartosc);
    	Spinner okres_typ = (Spinner) parametry.findViewById(R.id.okres_typ);
    	Spinner rodzaj_operacji = (Spinner) parametry.findViewById(R.id.rodzaj_operacji);
    	EditText kwota_od = (EditText) parametry.findViewById(R.id.kwota_od);
    	EditText kwota_do = (EditText) parametry.findViewById(R.id.kwota_do);
    	CheckBox exportPDF = (CheckBox) parametry.findViewById(R.id.exportPDF);

    	// parametry wysylki
		String rangepanel_group;
		String daterange_from_day;
		String daterange_from_month;
		String daterange_from_year;
		String daterange_to_day;
		String daterange_to_month;
		String daterange_to_year;
		String lastdays_days;
		String lastdays_period;
		String accoperlist_typefilter_group;
		String accoperlist_amountfilter_amountmin;
		String accoperlist_amountfilter_amountmax;
		String export_oper_history_format;

		if(exportPDF.isChecked()) export_oper_history_format = "PDF";
		else
		{
			export_oper_history_format = null;
			exportPDF = null;
		}
    	
		// sprawdzenie ktora opcja radio jest wybrana
		switch(radio)
		{
			// podany przedzial czasowy
			case 0:
				rangepanel_group = "daterange_radio";
				daterange_from_day = String.valueOf(data_od.getDayOfMonth());
				daterange_from_month = String.valueOf(data_od.getMonth()+1);
				daterange_from_year = String.valueOf(data_od.getYear());
				daterange_to_day = String.valueOf(data_do.getDayOfMonth());
				daterange_to_month = String.valueOf(data_do.getMonth()+1);
				daterange_to_year = String.valueOf(data_do.getYear());
				accoperlist_typefilter_group = rodzaje_operacji.getAsString((String) rodzaj_operacji.getSelectedItem());
				accoperlist_amountfilter_amountmin = kwota_od.getText().toString().replace(".", ",");
				accoperlist_amountfilter_amountmax = kwota_do.getText().toString().replace(".", ",");
				
				
				pobierzHistorie(
						rangepanel_group,
						daterange_from_day,
						daterange_from_month,
						daterange_from_year,
						daterange_to_day,
						daterange_to_month,
						daterange_to_year,
						null,
						null,
						accoperlist_typefilter_group,
						accoperlist_amountfilter_amountmin,
						accoperlist_amountfilter_amountmax,
						export_oper_history_format
						);
				break;
			// okreslona ilosc dla danego okresu
			case 1:
				rangepanel_group = "lastdays_radio";
				lastdays_days = okres_wartosc.getText().toString();
				lastdays_period = okres_typy.getAsString((String) okres_typ.getSelectedItem());
				accoperlist_typefilter_group = rodzaje_operacji.getAsString((String) rodzaj_operacji.getSelectedItem());
				accoperlist_amountfilter_amountmin = kwota_od.getText().toString().replace(".", ",");
				accoperlist_amountfilter_amountmax = kwota_do.getText().toString().replace(".", ",");
	
				pobierzHistorie(
							rangepanel_group,
							null,
							null,
							null,
							null,
							null,
							null,
							lastdays_days,
							lastdays_period,
							accoperlist_typefilter_group,
							accoperlist_amountfilter_amountmin,
							accoperlist_amountfilter_amountmax,
							export_oper_history_format
							);

				break;
			// ostatnie logowanie
			case 2:

				rangepanel_group = "lastlogin_radio";
				accoperlist_typefilter_group = rodzaje_operacji.getAsString((String) rodzaj_operacji.getSelectedItem());
				accoperlist_amountfilter_amountmin = kwota_od.getText().toString().replace(".", ",");
				accoperlist_amountfilter_amountmax = kwota_do.getText().toString().replace(".", ",");
	
				pobierzHistorie(
						rangepanel_group,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						accoperlist_typefilter_group,
						accoperlist_amountfilter_amountmin,
						accoperlist_amountfilter_amountmax,
						export_oper_history_format
						);
				break;
		}
    }
    
    private boolean sprawdzPoprawnoscDanych(int radio)
	{
    	DatePicker data_od = (DatePicker) parametry.findViewById(R.id.data_od);
    	DatePicker data_do = (DatePicker) parametry.findViewById(R.id.data_do);
   		EditText okres_wartosc = (EditText) parametry.findViewById(R.id.okres_wartosc);
   		EditText kwota_od = (EditText) parametry.findViewById(R.id.kwota_od);
   		EditText kwota_do = (EditText) parametry.findViewById(R.id.kwota_do);
   		CheckBox exportPDF = (CheckBox) parametry.findViewById(R.id.exportPDF);
   		
   		if(exportPDF.isChecked())
   		{
   			String state = Environment.getExternalStorageState();

   			if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				Toast.makeText(ListaHistoria.this, R.string.karta_odczyt, Toast.LENGTH_LONG).show();
				return false;

			}
   			else if (Environment.MEDIA_MOUNTED.equals(state) == false) {
				Toast.makeText(ListaHistoria.this, R.string.karta_brak, Toast.LENGTH_LONG).show();
				return false;
   			}
   		}
		
		switch(radio)
		{
			// data od do
			case 0:
				// sprawdzenie wprowadzonej daty
				Calendar minimalna = Calendar.getInstance();
				Calendar wprowadzona_od = Calendar.getInstance();
				Calendar wprowadzona_do = Calendar.getInstance();
				Calendar dzisiaj = Calendar.getInstance();
				wprowadzona_od.set(data_od.getYear(), data_od.getMonth(), data_od.getDayOfMonth());
				wprowadzona_do.set(data_do.getYear(), data_do.getMonth(), data_do.getDayOfMonth());
				minimalna.set(1901, 0, 1);
				
				// jezeli data od lub do jest wieksza od daty dzisiejszej lub daty sa pozniej minimalnej
				if(wprowadzona_od.compareTo(dzisiaj) == 1 || wprowadzona_do.compareTo(dzisiaj) == 1 || wprowadzona_od.compareTo(minimalna) == -1 || wprowadzona_do.compareTo(minimalna) == -1)
				{
					Toast.makeText(ListaHistoria.this, R.string.przelewy_blad_data, Toast.LENGTH_LONG).show();
					return false;
				}
				if(wprowadzona_od.compareTo(wprowadzona_do) == 1)
				{
					Toast.makeText(ListaHistoria.this, R.string.historia_data_blad, Toast.LENGTH_LONG).show();
					return false;
				}
				break;
				// okres
			case 1:
				// sprawdzenie czy podana wartosc okresu
				if(okres_wartosc.getText().toString().length() == 0)
				{
					Toast.makeText(ListaHistoria.this.getApplicationContext(), R.string.historia_brak_wartosci, Toast.LENGTH_LONG).show();
					return false;
				}
				break;
		}
		
		// sprawdzenie czy kwota do jest wieksza niz kwota od
		if(kwota_od.getText().toString().length() != 0 && kwota_do.getText().toString().length() != 0)
		{
			try
			{
				int kwotaOdInt = Integer.parseInt(kwota_od.getText().toString());
				int kwotaDoInt = Integer.parseInt(kwota_do.getText().toString());
				
				if(kwotaOdInt > kwotaDoInt)
				{
					Toast.makeText(ListaHistoria.this.getApplicationContext(), R.string.historia_kwoty, Toast.LENGTH_LONG).show();
					return false;
				}
			}catch(NumberFormatException e)
			{
				return false;
			}
		}
		
		return true;
	}

    
	private class AdapterHistoria extends ArrayAdapter<Historia> {

		private ArrayList<Historia> historia;
		
		public AdapterHistoria(Context context, int textViewResourceId, ArrayList<Historia> historia)
		{
	        super(context, textViewResourceId, historia);
	        this.historia = historia;
		}

		
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	            View v = convertView;
	            if (v == null) {
	                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                v = vi.inflate(R.layout.wiersz_historia, null);
	            }
	            Historia h = historia.get(position);
	            
	            if (h != null)
	            {
	            	TextView data = (TextView) v.findViewById(R.id.historia_data);
	            	TextView kwota = (TextView) v.findViewById(R.id.historia_kwota);
	                TextView rodzaj = (TextView) v.findViewById(R.id.historia_rodzaj);
	                TextView dane = (TextView) v.findViewById(R.id.historia_dane);
	                TextView rachunek = (TextView) v.findViewById(R.id.historia_rachunek);
	                TextView tytul = (TextView) v.findViewById(R.id.historia_tytul);
	                
	                data.setText(h.pobierzDateKsiegowania());
	                kwota.setText(h.pobierzKwoteOperacji());
	                if(kwota.getText().toString().startsWith("-"))
	                {
	                	kwota.setBackgroundResource(R.drawable.kwota_ujemna);
	                	kwota.setTextColor(ListaHistoria.this.getResources().getColor(R.color.wartosc_ujemna));
	                }
	                else
	                {
	                	kwota.setTextColor(ListaHistoria.this.getResources().getColor(R.color.wartosc_dodatnia));
	                	kwota.setBackgroundResource(R.drawable.kwota_dodatnia);
	                }
	                rodzaj.setText(h.pobierzRodzajPrzelewu());
	                if(h.pobierzDaneNadawcy() != null)
	                {
	                	dane.setText(h.pobierzDaneNadawcy());
	                	dane.setVisibility(View.VISIBLE);
	                }
	                else dane.setVisibility(View.GONE);
	                if(h.pobierzRachunekNadawcy() != null)
	                {
	                	rachunek.setText(h.pobierzRachunekNadawcy());
	                	rachunek.setVisibility(View.VISIBLE);
	                }
	                else rachunek.setVisibility(View.GONE);
	                if(h.pobierzTytulPrzelewu() != null)
	                {
	                	tytul.setText(h.pobierzTytulPrzelewu());
	                	tytul.setVisibility(View.VISIBLE);
	                }
	                else tytul.setVisibility(View.GONE);
	            }
	            return v;
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
						
	                	Bankoid.tworzProgressDialog(ListaHistoria.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(ListaHistoria.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
