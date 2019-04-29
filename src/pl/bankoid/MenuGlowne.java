package pl.bankoid;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class MenuGlowne extends ListActivity implements Runnable {

	public static Rachunek wybrany = null;
	public static boolean odswiez = false;
	private ArrayList<Rachunek> rachunki;
	private AdapterRachunki m_adapter = null;
	private String noweDane = null;
	private AdView adView;
	
	// regex
   	Pattern kontaREGEX = Pattern.compile("<p class=\"Account\"><a .+?'([^']{100,})'[^>]+>([^<]+)</a></p><p class=\"Amount\"><a [^>]+>([^<]+)</a></p><p class=\"Amount\"><span [^>]+>([^<]+)");
	Pattern data_logowaniaREGEX = Pattern.compile("<p id=\"logonInfo\"><span>([^<]+)</span><span>([^<]+)</span></p>");

	private Thread watek;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_glowne);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");
        
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        // REKLAMA
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

      
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();
        
        this.setTitle("Menu główne");
        
	    // jezeli pobieranie rachunkow nie powiodlo sie
        if(pobierzRachunki(dane) == false) Bankoid.bledy.pokazKomunikat(MenuGlowne.this);
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
			Bankoid.tworzProgressDialog(MenuGlowne.this, getResources().getString(R.string.dialog_pobinfo));
			Bankoid.dialog.show();
			
			watek = new Thread(MenuGlowne.this, String.valueOf(Bankoid.ODSWIEZANIE));
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
            	
    		case Bankoid.ACTIVITY_POBIERANIE_INFORMACJI:
    			String dane = wybierzRachunek();
    			Intent intent = new Intent(MenuGlowne.this, OperacjeRachunek.class);
    			intent.putExtra("dane", dane);
    			startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_POBIERANIE_INFORMACJI);
            	break;
            	
    		case Bankoid.ACTIVITY_POTWIERDZENIA:
    			dane = wybierzPotwierdzenia();
    			intent = new Intent(MenuGlowne.this, ListaPotwierdzenia.class);
    			intent.putExtra("dane", dane);
    			startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_POTWIERDZENIA);
            	break;
    		
    		case Bankoid.ACTIVITY_KARTY:
    			dane = wybierzKarty();
    			intent = new Intent(MenuGlowne.this, Karty.class);
    			intent.putExtra("dane", dane);
       			intent.putExtra("typ", "ekarty");
    			startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTY);
            	break;
            	            	
    		case Bankoid.ODSWIEZANIE:
    			noweDane = Bankoid.wybierzRachunki();
            	handler.sendEmptyMessage(Bankoid.ODSWIEZANIE);
            	break;
    	}
    	
    }
    
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch(msg.what)
    		{
    			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
    				MenuGlowne.this.finish();
    				break;
    				
    			case Bankoid.ODSWIEZANIE:
    				pobierzRachunki(noweDane);
    				noweDane = null;
    				break;
    		}

    		try
    		{
    			Bankoid.dialog.dismiss();
    			
    		}catch(Exception e){}

    	}
    };
    
    @Override 
    protected void onStart() {  
    	super.onStart();
    	getPrefs();
    }

    // funkcja wywolywana po wyjsciu z ustawien
    private void getPrefs()
    {
    	SharedPreferences prefs = getSharedPreferences(Ustawienia.PREFS_NAME, 0);
        
        // czy automatycznie wstawiac haslo sms
        Bankoid.pref_sms = prefs.getBoolean("pref_sms", false);
        
    }
        
    // pobiera liste rachunkow
    public boolean pobierzRachunki(String dane)
    {
    	// jezeli nie udalo sie wybrac rachunkow to zwroc odrazu false
    	if(dane == null) return false;
    	
    	TextView udane_logowanie = (TextView) findViewById(R.id.udane_logowanie);
    	TextView nieudane_logowanie = (TextView) findViewById(R.id.nieudane_logowanie);
	    
	    // szukanie dat zalogowania
	    Matcher m = data_logowaniaREGEX.matcher(dane);
	    if(m.find())
	    {
	    	udane_logowanie.setText(m.group(1));
	    	nieudane_logowanie.setText(m.group(2));
	    }
	    
	    rachunki = new ArrayList<Rachunek>();
	    m = kontaREGEX.matcher(dane);
	    while(m.find())
	    {
	    	Rachunek r = new Rachunek();
	    	r.ustawParameters(m.group(1));
	    	r.ustawNazwe(m.group(2));
	    	r.ustawSaldo(m.group(3));
	    	r.ustawSrodki(m.group(4));
	    	
	    	rachunki.add(r);
	    }
	    // jezeli udalo sie pobrac rachunki to je wyswietl
	    if(rachunki.size() > 0)
	    {
		    //Log.v("ILOSC_KONT",""+rachunki.size());
		    this.m_adapter = new AdapterRachunki(MenuGlowne.this, R.layout.wiersz_rachunek, rachunki);
		    this.setListAdapter(m_adapter);
		    
            ListView lv = getListView();    		
    		// jezeli rachunkow jest wiecej niz 2 i sa wlaczone reklamy to ustaw wage
    		//if(lv.getCount() > 3) ustawWage(lv);
    		//else if(lv.getCount() > 2 && Bankoid.reklamy) ustawWage(lv);
    		//this.registerForContextMenu(lv);
    		
    		lv.setOnItemClickListener(new OnItemClickListener() {
    		    public void onItemClick(AdapterView<?> parent, View view,
    		        int position, long id) {
    		    	
    		    	wybrany = rachunki.get(position);
    		    	//Log.v("nazwa", wybrany.pobierzNazwe());
    		    	//Log.v("param", wybrany.pobierzParameters());
    		    	if(wybrany != null)
    		    	{
                    	Bankoid.tworzProgressDialog(MenuGlowne.this, getResources().getString(R.string.dialog_pobinfo));
                    	Bankoid.dialog.show();
                    	
                    	watek = new Thread(MenuGlowne.this, String.valueOf(Bankoid.ACTIVITY_POBIERANIE_INFORMACJI));
                    	watek.start();
    		    	}
    		    	
    		    }
    		  });
    		
    		// ustawienie sluchacza dla przycisku operacje do potwierdzenia
    		Button przycisk_do_potwierdzenia = (Button) findViewById(R.id.przycisk_do_potwierdzenia);
    		przycisk_do_potwierdzenia.setOnClickListener(new OnClickListener()
    		{

				@Override
				public void onClick(View v) {
                	Bankoid.tworzProgressDialog(MenuGlowne.this, getResources().getString(R.string.dialog_pobinfo));
                	Bankoid.dialog.show();
                	
                	watek = new Thread(MenuGlowne.this, String.valueOf(Bankoid.ACTIVITY_POTWIERDZENIA));
                	watek.start();
				}
    			
    		});
    		
    		// ustawienie sluchacza dla przycisku karty
    		Button przycisk_karty = (Button) findViewById(R.id.przycisk_karty);
    		przycisk_karty.setOnClickListener(new OnClickListener()
    		{

				@Override
				public void onClick(View v) {
                	Bankoid.tworzProgressDialog(MenuGlowne.this, getResources().getString(R.string.dialog_pobinfo));
                	Bankoid.dialog.show();
                	
                	watek = new Thread(MenuGlowne.this, String.valueOf(Bankoid.ACTIVITY_KARTY));
                	watek.start();
				}
    			
    		});
		    
		    return true;
	    }
	    // szukanie bledu
	    else if(Bankoid.bledy.czyBlad() == false)
    	{
    		Bankoid.bledy.ustawBlad(R.string.rachunki_blad, Bledy.WYLOGUJ);
    	}
	    return false;
    }

    public static String wybierzRachunek()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/account_details.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", wybrany.pobierzParameters());
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    
	    return rezultat; 
    }
    
    public static String wybierzPotwierdzenia()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/suspended_transaction_list.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", "");
	    request.addParam("__STATE", Bankoid.state);
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    
	    return rezultat; 
    }
    
    public static String wybierzKarty()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/cards_list.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", "");
	    request.addParam("__STATE", Bankoid.state);
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    
	    return rezultat; 
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        //Handle the back button
        if(keyCode == KeyEvent.KEYCODE_BACK) {
        	final Komunikat k = new Komunikat(this);
        	k.ustawIkone(android.R.drawable.ic_menu_help);
        	k.ustawTytul("Zamykanie");
        	k.ustawTresc(R.string.pytanie_wyloguj);
        	k.ustawPrzyciskTak("Tak", new View.OnClickListener() { 
     
				@Override
				public void onClick(View v) {
					
                	Bankoid.tworzProgressDialog(MenuGlowne.this, getResources().getString(R.string.dialog_wylogowywanie));
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
                	
                	watek = new Thread(MenuGlowne.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
                	watek.start();
                	
                	k.dismiss();
				} 
     
            });
        	k.ustawPrzyciskNie("Nie", null);
        	k.show();

        	return true;
        }
        else {
            return super.onKeyDown(keyCode, event);
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
        MenuItem ustawienia = menu.findItem(R.id.ustawienia);
        //wylacz_reklamy.setVisible(Bankoid.reklamy);
        ustawienia.setVisible(true);
        
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
						
	                	Bankoid.tworzProgressDialog(MenuGlowne.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(MenuGlowne.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
	                	watek.start();
	                	
	                	k.dismiss();
					} 
	     
	            });
	        	k.ustawPrzyciskNie("Nie", null);
	        	k.show();
	        	return true;
	        	
	        case R.id.oprogramie:
	        	Bankoid.pokazDialogOprogramie(MenuGlowne.this);
	        	return true;

	        /*case R.id.wylacz_reklamy:
	        	Bankoid.pokazDialogWylaczReklamy(MenuGlowne.this);
        	return true;*/
        	
	        case R.id.ustawienia:
	        	Intent ustawienia = new Intent(MenuGlowne.this, Ustawienia.class);
	        	ustawienia.putExtra("reklamy", Bankoid.REKLAMA);
	        	startActivity(ustawienia);
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
    
	private class AdapterRachunki extends ArrayAdapter<Rachunek> {

		private ArrayList<Rachunek> rachunki;
		
		public AdapterRachunki(Context context, int textViewResourceId, ArrayList<Rachunek> rachunki)
		{
	        super(context, textViewResourceId, rachunki);
	        this.rachunki = rachunki;
		}

		
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	            View v = convertView;
	            if (v == null) {
	                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                v = vi.inflate(R.layout.wiersz_rachunek, null);
	            }
	            Rachunek r = rachunki.get(position);
	            
	            if (r != null) {
	                    TextView nr_rachunku = (TextView) v.findViewById(R.id.numer_rachunku);
	                    TextView saldo = (TextView) v.findViewById(R.id.saldo);
	                    TextView dostepne_srodki = (TextView) v.findViewById(R.id.dostepne_srodki);
	                    
	                    if(r.pobierzNazwe() != null) nr_rachunku.setText(r.pobierzNazwe());
	                    if(r.pobierzSaldo() != null) saldo.setText(r.pobierzSaldo());
	                    if(r.pobierzSrodki() != null) dostepne_srodki.setText(r.pobierzSrodki());
	            }
	            return v;
	    }
	}
	
/*	 public void ustawWage(ListView listView) {

		 LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 2f);
         listView.setLayoutParams(params);
     }*/
}
