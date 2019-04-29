package pl.bankoid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class ListaHistoriaKarty extends ListActivity implements Runnable {

	private ArrayList<Historia> m_historia = new ArrayList<Historia>();
	private AdapterHistoria m_adapter = null;
	private Dialog parametry;
	private AdView adView;

	// wybrany radiobox
	private int radio = 0;
	private Thread watek;
	
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
					 ListaHistoriaKarty.this.finish();
					 break;
    		
    			case Bankoid.ACTIVITY_POBIERANIE_HISTORII:
    	    		// jezeli wystabil blad podczas pobierania historii to go pokaz
    	    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ListaHistoriaKarty.this);
    	        	// inaczej dodaj historie wynikow i ja pokaz
    	        	else
    	        	{
    	        	    m_adapter = new AdapterHistoria(getApplicationContext(), R.layout.wiersz_historia, m_historia);
    	        	    setListAdapter(m_adapter);

    	        		// zamkniecie okna dialogowego z parametrami historii
    	    			parametry.dismiss();


    	        	}
    				break;
    		}
    	}
    };
    
	// tworzy i pokazuje dialog z parametrami historii
    public boolean tworzParamHistoria(String dane)
    {
	    parametry = new Dialog(this, R.style.CustomTheme);
   	    parametry.setContentView(R.layout.dialog_historia_karta);
   	    parametry.getWindow().setFormat(PixelFormat.RGBA_8888);
   	    parametry.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
   	    
   	    // REGEX
    	Pattern tytulREGEX = Pattern.compile("<div id=\"rangepanel\" class=\"general\">.+?<h3>.+?</a>(.+?)</h3>", Pattern.DOTALL);

    	// ZMIENNE
    	Calendar kalendarz_od = Calendar.getInstance();
    	kalendarz_od.add(Calendar.MONTH, -1);
    	Calendar kalendarz_do = Calendar.getInstance();
    	final RadioButton radio_zakres_dat = (RadioButton) parametry.findViewById(R.id.radio_zakres_dat);
    	final RadioButton radio_ostatnie_dni = (RadioButton) parametry.findViewById(R.id.radio_ostatnie_dni);
    	final RadioButton radio_ostatnie_miesiace = (RadioButton) parametry.findViewById(R.id.radio_ostatnie_miesiace);
    	EditText miesiace_wartosc = (EditText) parametry.findViewById(R.id.miesiace_wartosc);
    	TextView miesiace_label = (TextView) parametry.findViewById(R.id.miesiecy_label);
    	DatePicker data_od = (DatePicker) parametry.findViewById(R.id.data_od);
    	DatePicker data_do = (DatePicker) parametry.findViewById(R.id.data_do);
    	Button przycisk_zatwierdz = (Button) parametry.findViewById(R.id.przycisk_zatwierdz);
   	
    	data_od.init(kalendarz_od.get(Calendar.YEAR), kalendarz_od.get(Calendar.MONTH), kalendarz_od.getActualMinimum(Calendar.DAY_OF_MONTH), null);
    	data_do.init(kalendarz_do.get(Calendar.YEAR), kalendarz_do.get(Calendar.MONTH), kalendarz_do.get(Calendar.DAY_OF_MONTH), null);

    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.historia_blad, Bledy.WYLOGUJ);
    		return false;
    	}
    	
    	Matcher m = tytulREGEX.matcher(dane);
    	
   	    // udalo sie otworzyc okno z parametrami historii
   	    if(m.find())
   	    {
   	    	parametry.setTitle(m.group(1).trim());
   	    	this.setTitle(m.group(1).trim());
   	    	
   	    	// pokaz dodatkowe pole z wyborem miesiaca
   	    	if(dane.contains("lastmonth_months"))
   	    	{
   	    		radio_ostatnie_miesiace.setVisibility(View.VISIBLE);
   	    		miesiace_wartosc.setVisibility(View.VISIBLE);
   	    		miesiace_label.setVisibility(View.VISIBLE);
   	    	}
   	    	
   	    	// w przypadku anulowania parametrow historii powrot do szczegolow karty
   	    	parametry.setOnCancelListener(new DialogInterface.OnCancelListener()
   	    	{
				@Override
				public void onCancel(DialogInterface oknoparam) {
					oknoparam.dismiss();
					ListaHistoriaKarty.this.finish();
				}
   	    		
   	    	});
   	    	
   	    	// odznaczanie poszczegolnych radio
   	    	radio_zakres_dat.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v) {
   					radio = 0;
   					radio_ostatnie_dni.setChecked(false);
   					radio_ostatnie_miesiace.setChecked(false);
   				}
   	    	});

   	    	radio_ostatnie_dni.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v) {
   					radio = 1;
   					radio_zakres_dat.setChecked(false);
   					radio_ostatnie_miesiace.setChecked(false);
   				}
   	    	});

   	    	radio_ostatnie_miesiace.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View v) {
   					radio = 2;
   					radio_zakres_dat.setChecked(false);
   					radio_ostatnie_dni.setChecked(false);
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
   	   					Bankoid.tworzProgressDialog(ListaHistoriaKarty.this, getResources().getString(R.string.dialog_pobinfo));
   	   					Bankoid.dialog.show();
   	   							
   	   					watek = new Thread(ListaHistoriaKarty.this, String.valueOf(Bankoid.ACTIVITY_POBIERANIE_HISTORII));
   	   					watek.start();
   					}
   				}
   	    		
   	    	});

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
    		String lastmonth_months)
    {
    	
    	//Log.v("pobieranie", "historia");
    	Pattern historia_ekartyREGEX = Pattern.compile("<li( class=\"alternate\")?><p class=\"Date\"><a [^>]+>([^<]+)</a></p><p class=\"Date\"><span id=[^>]+>([^<]+)</span></p><p class=\"OperationType\"><span id=[^>]+>([^<]+)</span></p><p class=\"OperationDescription\"><span id=[^>]+>([^<]+)</span></p><p class=\"Amount\"><span id=[^>]+>([^<]+)</span></p><p class=\"Amount\"><span id=[^>]+>([^<]+)</span>");
    	Pattern historia_debetREGEX = Pattern.compile("<li( class=\"alternate\")?><p class=\"CardNumber\"><span id=[^>]+>([^<]+)</span></p><p class=\"Date\"><span id=[^>]+>([^<]+)<br[^>]*>([^<]+)</span></p><p class=\"OperationType\"><a [^>]+>([^<]+)</a></p><p class=\"Merchant\"><span id=[^>]+>([^<]*)</span></p><p class=\"Amount\"><span id=[^>]+>([^<]+)</span></p><p class=\"Amount\"><span id=[^>]+>([^<]+)</span>");
    	
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	if(rozszerzenie.equals("cd")) request.setUrl("https://www.mbank.com.pl/cd_operations_list.aspx");
    	else if(rozszerzenie.equals("cv")) request.setUrl("https://www.mbank.com.pl/cv_card_operation_list.aspx");
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
   	    }
   	    else if(rangepanel_group.equals("lastmonth_radio"))
   	    {
   	    	request.addParam("lastmonth_months", lastmonth_months);
   	    }
   	    request.execute();

   	    String rezultat = request.getResult();

    	if(rozszerzenie.equals("cd"))
    	{
    		Matcher m = historia_debetREGEX.matcher(rezultat);
       	    while(m.find())
       	    {
       	    	Historia h = new Historia();
       	    	h.ustawRachunekNadawcy(m.group(2));
       	    	h.ustawDateOperacji(m.group(3));
       	    	h.ustawDateKsiegowania(m.group(4));
       	    	h.ustawRodzajPrzelewu(m.group(5));	
       	    	h.ustawTytulPrzelewu(m.group(6));
       	      	h.ustawKwoteOperacji(m.group(7));
       	    	h.ustawSaldoPoOperacji(m.group(8));
       	    	
       	    	m_historia.add(h);
       	    }
    	}
    	else if(rozszerzenie.equals("cv"))
    	{
       	    Matcher m = historia_ekartyREGEX.matcher(rezultat);
       	    while(m.find())
       	    {
       	    	Historia h = new Historia();
       	    	h.ustawDateOperacji(m.group(2));
       	    	h.ustawDateKsiegowania(m.group(3));
       	    	h.ustawTytulPrzelewu(m.group(4));
       	    	h.ustawRodzajPrzelewu(m.group(5));
       	    	h.ustawKwoteOperacji(m.group(6));
       	    	h.ustawSaldoPoOperacji(m.group(7));
       	    	
       	    	m_historia.add(h);
       	    }
    	}

    	    
   	    Bankoid.pobierzState(rezultat);
   	    Bankoid.pobierzEventvalidation(rezultat);
    }
    
    // metoda pobiera dante z formularza i wysyla do metody pobierzHistorie()
    private void pobieranieHistorii()
    {
    	// referencje do obiektow formularza
    	DatePicker data_od = (DatePicker) parametry.findViewById(R.id.data_od);
    	DatePicker data_do = (DatePicker) parametry.findViewById(R.id.data_do);
    	EditText lastdays_days = (EditText) parametry.findViewById(R.id.dni_wartosc);
    	EditText lastmonth_months = (EditText) parametry.findViewById(R.id.miesiace_wartosc);

    	// parametry wysylki
		String rangepanel_group;
		String daterange_from_day;
		String daterange_from_month;
		String daterange_from_year;
		String daterange_to_day;
		String daterange_to_month;
		String daterange_to_year;
    	
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

				pobierzHistorie(
						rangepanel_group,
						daterange_from_day,
						daterange_from_month,
						daterange_from_year,
						daterange_to_day,
						daterange_to_month,
						daterange_to_year,
						null,
						null
						);
				break;
			// okreslona ilosc dni
			case 1:
				rangepanel_group = "lastdays_radio";
	
				pobierzHistorie(
							rangepanel_group,
							null,
							null,
							null,
							null,
							null,
							null,
							lastdays_days.getText().toString(),
							null
							);

				break;
			// ostatnich miesiecy
			case 2:

				rangepanel_group = "lastmonth_radio";
	
				pobierzHistorie(
						rangepanel_group,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						lastmonth_months.getText().toString()
						);
				break;
		}
    }
    
    private boolean sprawdzPoprawnoscDanych(int radio)
	{
    	DatePicker data_od = (DatePicker) parametry.findViewById(R.id.data_od);
    	DatePicker data_do = (DatePicker) parametry.findViewById(R.id.data_do);
   		EditText dni_wartosc = (EditText) parametry.findViewById(R.id.dni_wartosc);
   		EditText miesiace_wartosc = (EditText) parametry.findViewById(R.id.miesiace_wartosc);
		
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
					Toast.makeText(ListaHistoriaKarty.this, R.string.przelewy_blad_data, Toast.LENGTH_LONG).show();
					return false;
				}
				if(wprowadzona_od.compareTo(wprowadzona_do) == 1)
				{
					Toast.makeText(ListaHistoriaKarty.this, R.string.historia_data_blad, Toast.LENGTH_LONG).show();
					return false;
				}
				break;
				// okres
			case 1:
				// sprawdzenie czy podana wartosc okresu
				if(dni_wartosc.getText().toString().length() == 0)
				{
					Toast.makeText(ListaHistoriaKarty.this.getApplicationContext(), R.string.historia_brak_wartosci, Toast.LENGTH_LONG).show();
					return false;
				}
				break;
			case 2:
				// sprawdzenie czy podana wartosc okresu
				if(miesiace_wartosc.getText().toString().length() == 0)
				{
					Toast.makeText(ListaHistoriaKarty.this.getApplicationContext(), R.string.historia_brak_wartosci, Toast.LENGTH_LONG).show();
					return false;
				}
				break;
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
	            String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
	            
	            if (h != null)
	            {
	            	TextView data = (TextView) v.findViewById(R.id.historia_data);
	            	TextView kwota = (TextView) v.findViewById(R.id.historia_kwota);
	                TextView rodzaj = (TextView) v.findViewById(R.id.historia_rodzaj);
	                TextView dane = (TextView) v.findViewById(R.id.historia_dane);
	                TextView rachunek = (TextView) v.findViewById(R.id.historia_rachunek);
	                TextView tytul = (TextView) v.findViewById(R.id.historia_tytul);
                	dane.setVisibility(View.GONE);
                	
            		data.setText(h.pobierzDateOperacji());
            		kwota.setText(h.pobierzKwoteOperacji()+ " / " + h.pobierzSaldoPoOperacji());
                    if(kwota.getText().toString().startsWith("-"))
	                {
	                	kwota.setBackgroundResource(R.drawable.kwota_ujemna);
	                	kwota.setTextColor(ListaHistoriaKarty.this.getResources().getColor(R.color.wartosc_ujemna));
	                }
	                else
	                {
	                	kwota.setTextColor(ListaHistoriaKarty.this.getResources().getColor(R.color.wartosc_dodatnia));
	                	kwota.setBackgroundResource(R.drawable.kwota_dodatnia);
	                }
                    rodzaj.setText(h.pobierzRodzajPrzelewu());
                	if(h.pobierzTytulPrzelewu() != null)
                	{
                		tytul.setText(h.pobierzTytulPrzelewu());
                		tytul.setVisibility(View.VISIBLE);
               		}else tytul.setVisibility(View.GONE);
	                
	            	if(rozszerzenie.equals("cd"))
	            	{
	            		rachunek.setText("Numer karty: " + h.pobierzRachunekNadawcy());
	            		rachunek.setVisibility(View.VISIBLE);
	            	}
		            else if(rozszerzenie.equals("cv")) rachunek.setVisibility(View.GONE);

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
						
	                	Bankoid.tworzProgressDialog(ListaHistoriaKarty.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(ListaHistoriaKarty.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
