package pl.bankoid;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class Bankoid extends Activity implements Runnable {
    
	public final int IDENTYFIKATOR_DLUGOSC = 8;
	public static String ADMOB_ID = "a14d61838cacedb"; 
	private AdView adView;
	
	public static AdRequest adr;
	public static boolean zalogowano = false;
	public static String state = "";
	public static String eventvalidation = "";
	public static Bledy bledy;
	public static boolean reklamy = false;
	//public static boolean hosts = false;
	public static boolean pref_sms = false;
	// dialo informujacy o nowej wersji
	private Komunikat nowa_wersja;
	private Thread watek;
	
	// Context
	public static Context context;

	// zmienne formularza
	private CheckBox zapamietaj_haslo;
	private EditText pole_haslo;
	private EditText pole_identyfikator;
	
	
	// REGEX
	private static Pattern stateREGEX = Pattern.compile("id=\"__STATE\" value=\"([^\"]+)\"");
	private static Pattern eventvalidationREGEX = Pattern.compile("id=\"__EVENTVALIDATION\" value=\"([^\"]+)\"");
	
	// progressdialog
	public static ProgressDialog dialog;

	// activity
	public final static int ACTIVITY_WYLOGOWYWANIE = 0;
	public final static int ACTIVITY_POBIERANIE_INFORMACJI = 1;
	public final static int ACTIVITY_HISTORIA = 3;
	public final static int ACTIVITY_PRZELEWY = 4;
	public final static int ACTIVITY_DOLADUJ_TELEFON = 5;
	public final static int ACTIVITY_ODBIORCY_ZDEFINIOWANI = 6;
	public final static int ACTIVITY_PRZELEW_ZDEFINIOWANY = 7;
	public final static int ACTIVITY_LOGOWANIE = 8;
	public final static int ACTIVITY_POTWIERDZENIA = 9;
	public final static int ACTIVITY_POBIERANIE_HISTORII = 10;
	public final static int ACTIVITY_KARTY = 11;
	public final static int ACTIVITY_KARTA = 12;
	public final static int ACTIVITY_KARTA_ZMIANA_SRODKOW = 13;
	public final static int ACTIVITY_KARTA_ROZLADOWANIE = 14;
	public final static int ACTIVITY_KARTA_LIMIT_KWOTOWY = 15;
	public final static int ACTIVITY_KARTA_LIMIT_ILOSCIOWY = 16;
	public final static int ACTIVITY_KARTA_OPERACJE_BIEZACE = 17;
	public final static int ACTIVITY_KARTA_SPLATA_ZADLUZENIA = 18;
	public final static int ACTIVITY_BLOKADY = 19;
	public final static int ACTIVITY_NOWA_WERSJA = 20;
	public final static int REKLAMA = 100;
	public final static int ODSWIEZANIE = 101;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DITHER, WindowManager.LayoutParams.FLAG_DITHER);

        // inicjalizacja zmiennych
    	zapamietaj_haslo = (CheckBox) findViewById(R.id.zapamietaj_haslo);
    	pole_haslo = (EditText) findViewById(R.id.pole_haslo);
    	pole_identyfikator = (EditText)findViewById(R.id.pole_identyfikator);
        context = this.getApplicationContext();
        bledy = new Bledy();
        nowa_wersja = new Komunikat(Bankoid.this);
        // id admob
		try {
			String klucz = (getString(R.string.dialog_logowanie)).substring(0, 16);
			ADMOB_ID = Ver.decryptOnline("7RIpNHtFeMw7m4q7G4j3ww==", klucz, Bankoid.this);
		} catch (Exception e) {
			
		}

        
        // sprawdzanie czy nalezy wyswietlac reklamy na serwerze
		watek = new Thread(Bankoid.this, String.valueOf(REKLAMA));
		watek.start();
		
        ((Button)findViewById(R.id.przycisk_loguj)).setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) {
				if(sprawdzPoprawnoscDanych())
				{
					tworzProgressDialog(Bankoid.this, getResources().getString(R.string.dialog_logowanie));
			        dialog.show();
			        
					watek = new Thread(Bankoid.this, String.valueOf(ACTIVITY_LOGOWANIE));
					watek.start();
				}

			}
        });

        
        // sprawdzenie czy uzytkownik wyraza zgode na zapisanie hasla w telefonie
        ((CheckBox)findViewById(R.id.zapamietaj_haslo)).setOnCheckedChangeListener(new OnCheckedChangeListener()
        {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked)
				{
					final Komunikat pytanie = new Komunikat(Bankoid.this);
					pytanie.ustawTytul("Pytanie");
					pytanie.ustawIkone(android.R.drawable.ic_menu_help);
					pytanie.ustawTresc(R.string.dialog_haslo);
					pytanie.ustawPrzyciskTak("Tak", null);
					pytanie.ustawPrzyciskNie("Nie", new OnClickListener()
					{

						@Override
						public void onClick(View v) {
							((CheckBox)findViewById(R.id.zapamietaj_haslo)).setChecked(false);
							pytanie.dismiss();
						}
						
					});
					pytanie.show();
				}
			}
        	
        });
        
        pole_identyfikator.addTextChangedListener(new TextWatcher()
        {

			public void afterTextChanged(Editable s) {
				if(s.toString().length() == IDENTYFIKATOR_DLUGOSC)
				{
					String haslo = odczytajHaslo(s.toString());
					if(haslo != null) pole_haslo.setText(haslo);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}
        	
        });
    }

    @Override 
    protected void onStart() {  
    	super.onStart();
    	getPrefs();
    }
    
    @Override 
    public void run()
    {
    	switch(Integer.valueOf(watek.getName()))
    	{
    		case ACTIVITY_LOGOWANIE:
    	    	zalogowano = zaloguj(); 
    	    	if(zalogowano)
    	    	{
    	    		String dane = wybierzRachunki();
    	    		Intent intent = new Intent(this, MenuGlowne.class);
    	    		intent.putExtra("dane", dane);
    	    		startActivity(intent);
    	    	}
    	    	handler.sendEmptyMessage(ACTIVITY_LOGOWANIE);
    			break;

    			// sprawdzenie czy pokazywac reklamy na serwerze
    		case REKLAMA:
    			reklamy = Ver.pokazReklamy(context);
    			handler.sendEmptyMessage(REKLAMA);
    			break;
    			
    		case ACTIVITY_NOWA_WERSJA:
    			try {
    			    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=pl.bankoid")));
    			} catch (android.content.ActivityNotFoundException anfe) {
    			    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=pl.bankoid")));
    			}
    	    	handler.sendEmptyMessage(ACTIVITY_NOWA_WERSJA);
    			break;
    	}
    }
   
    
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		try
    		{
    			dialog.dismiss();
    			
    		}catch(Exception e){}
    		

    		switch(msg.what)
    		{
    			case ACTIVITY_NOWA_WERSJA:
    				Bankoid.this.finish();
    				break;
				
    			case ACTIVITY_LOGOWANIE:
    	    		// czyszczenie pola z loginem i haslem
    				if(zalogowano)
    				{
    			  		// jezeli zaznacozne pole zapisz haslo
    		    		if(zapamietaj_haslo.isChecked())
    		    		{
    		    			zapiszHashID(pole_identyfikator.getText().toString());
    		    			zapiszHaslo(pole_haslo.getText().toString(), pole_identyfikator.getText().toString()); 
    		    		}

    					pole_identyfikator.setText("");
    					pole_haslo.setText("");
    				}
    				// jezeli logowanie nie powiodlo sie pokaz komunikat z bledem
    				else bledy.pokazKomunikat(Bankoid.this);

    				break;
    				
    				// pokaz reklamy jezeli trzeba
    			case REKLAMA:
		            FrameLayout layout = (FrameLayout) Bankoid.this.findViewById(R.id.ad);
		            layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));

    		        if(reklamy)
    		        {
    		        	adView = new AdView(Bankoid.this, AdSize.BANNER, ADMOB_ID); 
    		            layout.addView(adView);
    		            layout.setVisibility(View.VISIBLE);
    		            adr = new AdRequest();
    		            adView.loadAd(adr);
    		            
    		            /*hosts = sprawdzHosts();
    		            if(hosts)
    		            {
    		            	final Komunikat k = new Komunikat(Bankoid.this);
    		            	k.ustawIkone(android.R.drawable.ic_dialog_info);
    		            	k.ustawTresc(R.string.reklamy_info);
    		            	k.ustawTytul("Informacja");
    		            	k.setOnCancelListener(new OnCancelListener()
    		            	{
								@Override
								public void onCancel(DialogInterface dialog) {
									Bankoid.this.finish();
								}
    		            		
    		            	});
    				        k.ustawPrzyciskTak("Zamknij", new View.OnClickListener()
    				        {
								@Override
								public void onClick(View v) {
									Bankoid.this.finish();
								}
    				        });
        		    		k.ustawPrzyciskNie("Wyłącz reklamy", new OnClickListener()
        		    		{
    							@Override
    							public void onClick(View v) {
    								try {
    									String klucz = (getResources().getString(R.string.dialog_logowanie)).substring(0, 16);
    									String konto = Ver.decryptOnline("C7VTHZZNv/ZfpQgLt5zisRcwxfPxVmJ0rrmT6paPzMM=", klucz, Bankoid.this);
    						        	String dane_przelew = String.format(getResources().getString(R.string.dane_przelew), konto, Ver.pobierzID(Bankoid.this));
    						        	k.ustawTresc(getResources().getString(R.string.dialog_reklamy));
    						        	k.ustawListeZmian(dane_przelew);
    								} catch (Exception e) {
    									e.printStackTrace();
    								}

    							}
        		    			
        		    		});

    				        k.show();
    		            }*/
    		        }
    		        else layout.setVisibility(View.GONE);
    		        
    		        
    		        // jezeli jest nowa wersja i nie jest wyswietlana informacja o zablokowaniu reklam
    		        if(Ver.nowa_wersja && nowa_wersja.isShowing() == false)
    		        {

    		        	nowa_wersja.ustawIkone(android.R.drawable.ic_dialog_info);
    		        	nowa_wersja.ustawTresc(R.string.dialog_nowa_wersja);
    		        	nowa_wersja.ustawTytul("Nowa wersja");
    		        	nowa_wersja.ustawPrzyciskTak("Tak", new View.OnClickListener()
				        {
							@Override
							public void onClick(View v) {
								try
								{
									watek = new Thread(Bankoid.this, String.valueOf(ACTIVITY_NOWA_WERSJA));
									watek.start();
								}catch(ActivityNotFoundException e) {}

								nowa_wersja.dismiss();
							}
				        });
    		        	nowa_wersja.ustawPrzyciskNie("Nie", null);

    		        	nowa_wersja.show();
    		        }
    		        break;
    		}
    	}
    };
    
    public static void tworzProgressDialog(Context context, String message)
    {
		dialog = new ProgressDialog(context); 
		dialog.setIndeterminate(true);
		dialog.setTitle("Proszę czekać");
		dialog.setMessage(message);
        dialog.setCancelable(false);
    }

    
    public boolean zaloguj()
    {
    	Pattern komunikatREGEX = Pattern.compile("<div id=\"errorView\" class=\"error noSession\">.+?<h3[^>]*>(.+?)</h3>.*?<fieldset>.*?<p class=\"message\"[^>]*>(.+?)</p>.*?</fieldset>", Pattern.DOTALL);
    	Pattern bladREGEX = Pattern.compile("<div id=\"errorView\" class=\"error noSession\">.+?<h3>(.+?)</h3><fieldset>.+?<p class=\"message\">(.+?)</p>", Pattern.DOTALL);
    	Pattern seedREGEX = Pattern.compile("id=\"seed\" value=\"([^\"]+)\"");
		
		String rezultat;
		String seed = "";
		String identyfikator = pole_identyfikator.getText().toString();
		String haslo = pole_haslo.getText().toString();
		
		// POBRANIE STRONY GLOWNEJ LOGOWANIA
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl");
	    request.setMethod("GET");
	    request.execute();

	    request.setUrl("https://www.mbank.com.pl");
	    request.setMethod("GET");
	    request.execute();
	    
	    rezultat = request.getResult();
	    
	    Matcher m = seedREGEX.matcher(rezultat);
	    if(m.find())
	    {
	    	seed = m.group(1);
	    }
	    // nie udalo sie pobrac seeda zwroc blad
	    else
	    {
	    	// czy jest komunikat mbanku
	    	m = komunikatREGEX.matcher(rezultat);
	    	if(m.find())
	    	{
	    		bledy.ustawBlad(m.group(1).trim(), Html.fromHtml(m.group(2)).toString(), Bledy.POWROT);
	    		bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	    		bledy.ustawKolorTresc(this.getResources().getColor(R.color.tresc_blad));
	    	}
	    	// jezeli nie ma zadnego bledu pokaz blad sesji
	    	else if(bledy.czyBlad() == false) bledy.ustawBlad(R.string.blad_seed, Bledy.INFO);
	    	return false;
	    }

	    pobierzState(rezultat);
	    pobierzEventvalidation(rezultat);

	    
	    // LOGOWANIE
	    
	    request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/logon.aspx");
	    request.setMethod("POST");
	    request.addParam("seed", seed);
	    request.addParam("localDT", pobierzDate());
	    request.addParam("__PARAMETERS", "");
	    request.addParam("__STATE", state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("__EVENTVALIDATION", eventvalidation);
	    request.addParam("customer", identyfikator);
	    request.addParam("password", haslo);
	    request.execute();
	    
	    rezultat = request.getResult();
	    
	    
	    // ZALOGOWANO
	    if(rezultat.contains("accounts_list.aspx")) return true;
	    // sprawdzenie czy nie znaleziono stand. blad
	    else if(bledy.czyBlad() == false)
	    {
	    	m = bladREGEX.matcher(rezultat);
	    	// podano nieprawidlowy login lub haslo
	    	if(m.find())
	    	{
	    		bledy.ustawBlad(m.group(1).trim(), Html.fromHtml(m.group(2)).toString(), Bledy.POWROT);
	    		bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	    		bledy.ustawKolorTresc(this.getResources().getColor(R.color.tresc_blad));
	    	}
	    	else
	    	{
	    		bledy.ustawBlad(R.string.blad_polaczenie, Bledy.INFO);
	    	}
	    }
	    return false;
    }
    
    public static void wyloguj()
    {
	    // WYLOGOWANIE
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/logout.aspx");
	    request.setMethod("GET");
	    request.execute();
	    
	    zalogowano = false;
    }
    
    public String pobierzDate()
    {
		Date data = new Date();
		SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");
		
		return format.format(data);
    }
    
    public static void pobierzState(String dane)
    {
	    Matcher m = stateREGEX.matcher(dane);
	    if(m.find())
	    {
	    	state = m.group(1);
	    }
    }
    
    public static void pobierzEventvalidation(String dane)
    {
	    Matcher m = eventvalidationREGEX.matcher(dane);
	    if(m.find())
	    {
	    	eventvalidation = m.group(1);
	    }
    }
    
    // pobiera strone z rachunkami
    public static String wybierzRachunki()
    {
	    // WYBIERANIE RACHUNKOW
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/accounts_list.aspx");
	    request.setMethod("GET");
	    request.execute();
	    
	    String rezultat = request.getResult(); 
	    // ustawienie aktualnego state
	    Bankoid.pobierzState(rezultat);
	    return rezultat;
    }

    
    private void getPrefs()
    {
    	SharedPreferences prefs = getSharedPreferences(Ustawienia.PREFS_NAME, 0);
        SharedPreferences.Editor edytor = prefs.edit();
        
        // czy automatycznie wstawiac haslo sms
        pref_sms = prefs.getBoolean("pref_sms", false);
        
        // pobranie id karty sim
        String simid = prefs.getString("simid", null);

        if(simid != null)
        {
        	String klucz = Ver.md5(Ver.pobierzID(context)+Ver.pobierzSimID(context));
        	// zdekodowanie hash simid
        	try {
				simid = Ver.decrypt(klucz, simid);
			} catch (Exception e) {

				e.printStackTrace();
			}      	
        	// jezeli karta sim jest inna wyczysc dane i pokaz komunikat
        	if(! simid.equals(Ver.md5(Ver.pobierzSimID(context))))
        	{
        		czyscDane();
        		// pokaz komunikat
        		Komunikat k = new Komunikat(this);
        		k.ustawTytul(R.string.alert_tytul);
        		k.ustawTresc(R.string.alert_tresc);
        		k.ustawPrzyciskTak("Zamknij", null);
        		k.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
        		k.show();
        	}

        }

        // pobranie wersji programu zapisanej w ustawieniach
        String wersja = prefs.getString("wersja", null);
        
        // jezeli pierwsze uruchomienie
        if(wersja == null)
        {
        	// dopisz wersje do ustawien programu
        	wersja = Ver.pobierzWersje(context);
        	edytor.putString("wersja", wersja);
        	edytor.commit();
        	
        	
        	// pokaz dialog o programie
        	pokazDialogOprogramie(Bankoid.this);
        }
        // jezeli zainstalowana wersja jest nowsza to pokaz changeloog
        else if(wersja.equals(Ver.pobierzWersje(context)) == false)
        {
        	edytor.putString("wersja", Ver.pobierzWersje(context));
        	edytor.commit();
        	pokazDialogListaZmian(Bankoid.this);
        }
    }
    
    private void zapiszHaslo(String haslo, String id)
    {
        SharedPreferences prefs = getSharedPreferences(Ustawienia.PREFS_NAME, 0);
        SharedPreferences.Editor edytor = prefs.edit();

        if(haslo != null && id != null)
        {
        	try {
            	String klucz = Ver.md5(Ver.pobierzID(context)+Ver.pobierzSimID(context)+id);
				edytor.putString("haslo", Ver.encrypt(klucz, haslo));
				edytor.commit();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }
    
    // zapisuje hash oraz id karty sim
    private void zapiszHashID(String id)
    {
        SharedPreferences prefs = getSharedPreferences(Ustawienia.PREFS_NAME, 0);
        SharedPreferences.Editor edytor = prefs.edit();

        if(id != null)
        {
        	try {
        		String hash = Ver.md5(id);
        		String hashSIM = Ver.md5(Ver.pobierzSimID(context));
            	String klucz = Ver.md5(Ver.pobierzID(context)+Ver.pobierzSimID(context));
            	edytor.putString("simid", Ver.encrypt(klucz, hashSIM));
            	// klucz dla hash id
            	klucz = Ver.md5(Ver.pobierzID(context)+Ver.pobierzSimID(context)+id);
				edytor.putString("hash", Ver.encrypt(klucz, hash));
				
				edytor.commit();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }
    
    private void czyscDane()
    {
        SharedPreferences prefs = getSharedPreferences(Ustawienia.PREFS_NAME, 0);
        SharedPreferences.Editor edytor = prefs.edit();

        edytor.remove("haslo");
        edytor.remove("simid");
        edytor.remove("hash");
        edytor.commit();
    }
    
    // odczytaj haslo dla podanego id
    private String odczytajHaslo(String ID)
    {
        SharedPreferences prefs = getSharedPreferences(Ustawienia.PREFS_NAME, 0);
        
        // pobranie id karty sim
        String simid = prefs.getString("simid", null);
        
        // pobranie zakodowanego hasla
        String haslo = prefs.getString("haslo", null);
        
        // pobranie hash
        String hash = prefs.getString("hash", null);
        
        try
        {        	
	        if(simid != null && haslo != null && hash != null)
	        {
	        	String klucz = Ver.md5(Ver.pobierzID(context)+Ver.pobierzSimID(context));
	        	// zdekodowanie hash simid
	        	simid = Ver.decrypt(klucz, simid);
	        	
	        	// jezeli karta sim jest ta sama
	        	if(simid.equals(Ver.md5(Ver.pobierzSimID(context))))
	        	{
	        		// klucz dla hashid i hasla
	        		klucz = Ver.md5(Ver.pobierzID(context)+Ver.pobierzSimID(context)+ID);
	        		
	        		String hashID = Ver.md5(ID);
	        		// zdekodowanie hashid z pamieci
	      			hash = Ver.decrypt(klucz, hash);
	      			// jezeli id sa takie same
	      			if(hash.equals(hashID)) return Ver.decrypt(klucz, haslo);						
	        	}
	        }
        }catch (Exception e) {
			e.printStackTrace();
		}

        return null;
    }

    private boolean sprawdzPoprawnoscDanych()
	{	
		// sprawdzenie id
		if(pole_identyfikator.getText().toString().length() == 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.blad_id, Toast.LENGTH_LONG).show();
			return false;
		}
		// sprawdzenie hasla
		if(pole_haslo.getText().toString().length() == 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.blad_haslo, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
    /*
    private boolean sprawdzHosts()
    {
    	BufferedReader in = null;
    	String klucz = (getResources().getString(R.string.dialog_logowanie)).substring(0, 16);
    	
    	boolean hosts = false;

            try {
            	// szuka slowa admob
            	String szukaj = Ver.decryptOnline("rlqo1evInG1WUoVUmRp7Fg==", klucz, context);
            	String plik = Ver.decryptOnline("6qC5udA69OHtYaRMlhuj8g==", klucz, context);
				in = new BufferedReader(new InputStreamReader(new FileInputStream(plik)));
	            String line;

	            while ((line = in.readLine()) != null)
	            {
	                if (line.toLowerCase().contains(szukaj))
	                {
	                	hosts = true;
	                    break;
	                }
	            }
			}
            catch (Exception e) {
				e.printStackTrace();
			}finally
			{
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
        
        return hosts;
    }*/
    
    public static void pokazDialogOprogramie(Context context)
    {
    	String opis = context.getResources().getString(R.string.opis_programu);
    	int kolor = context.getResources().getColor(R.color.ok);
    	final Komunikat oprogramie = new Komunikat(context);
    	oprogramie.ustawLogo(R.drawable.bankoid);
    	oprogramie.ustawTresc("<font color=" + kolor + "><b>Bankoid " + Ver.pobierzWersje(context) + "</b></font>" + opis);
    	//oprogramie.ustawRozmiarTresc(18);
    	oprogramie.ustawListeZmian(R.string.mozliwosci_programu);
    	oprogramie.ustawPrzyciskTak("Zamknij", null);
    	//lista zmian na razie nie dostepna

    	oprogramie.ustawPrzyciskNie("Lista zmian", new OnClickListener()
    	{

			@Override
			public void onClick(View v) {
				oprogramie.ukryjLogo();
				oprogramie.ustawRozmiarTresc(18);
				oprogramie.ustawTresc("<b><u>Lista zmian:</u></b>");
				oprogramie.ustawListeZmian(R.string.lista_zmian);
			}
    		
    	});
    	oprogramie.show();
    }

    public static void pokazDialogListaZmian(Context context)
    {
    	Komunikat zmiany = new Komunikat(context);
    	zmiany.ustawRozmiarTresc(18);
    	zmiany.ustawTresc("<b><u>Lista zmian:</u></b>");
    	zmiany.ustawListeZmian(R.string.lista_zmian);
    	zmiany.ustawPrzyciskTak("OK", null);
    	zmiany.show();
    }
    
    public static void pokazDialogWylaczReklamy(Context context)
    {
		try {
			String klucz = (context.getResources().getString(R.string.dialog_logowanie)).substring(0, 16);
			String konto = Ver.decryptOnline("C7VTHZZNv/ZfpQgLt5zisRcwxfPxVmJ0rrmT6paPzMM=", klucz, context);
        	String dane_przelew = String.format(context.getResources().getString(R.string.dane_przelew), konto, Ver.pobierzID(context));
        	Komunikat k = new Komunikat(context);
        	k.ustawIkone(android.R.drawable.ic_dialog_info);
        	k.ustawTytul("Reklamy");
        	k.ustawTresc(context.getResources().getString(R.string.dialog_reklamy));
        	k.ustawListeZmian(dane_przelew);
        	k.ustawPrzyciskTak("Zamknij", null);
        	k.show();
			
		} catch (Exception e) {
			e.printStackTrace();
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
        // Set 'delete' menu item state depending on count
        //MenuItem wylacz_reklamy = menu.findItem(R.id.wylacz_reklamy);
        MenuItem wyloguj = menu.findItem(R.id.wyloguj);
        //wylacz_reklamy.setVisible(reklamy);
        wyloguj.setVisible(false);
        
        return super.onPrepareOptionsMenu(menu);
    }
    
    // wybrano opcje z glownego menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
	        case R.id.oprogramie:
	        	pokazDialogOprogramie(Bankoid.this);
	        	return true;

	       /* case R.id.wylacz_reklamy:
	        	pokazDialogWylaczReklamy(Bankoid.this);
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