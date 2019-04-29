package pl.bankoid;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class Karty extends ListActivity implements Runnable {

	private AdapterKarty m_adapter = null;
	private String noweDane = null;
	private Thread watek;
	public static boolean odswiez = false;
	public static Karta wybrana_karta;
	private AdView adView;
	
	// REGEX
	public static Pattern kartyREGEX = Pattern.compile("<div id=\"(\\w+)_CardsGrid\" class=\"grid\">.+?</a>(.+?)</h3><ul class=\"table\">(.+?)</ul>", Pattern.DOTALL);
	public static Pattern kartaREGEX = Pattern.compile("<p class=\"Card\"><a id=\"[^\"]+\" title=\"[^\"]+\" onclick=\"doSubmit\\('[^']+','[^']*','POST','([^']+)'[^>]+>([^<]+)</a></p><p class=\"CardOwner\"><span id=\"[^\"]+\">(.+?)</span></p><p class=\"CardStatus\"><span id=\"[^\"]+\">(.+?)</span></p>(<p class=\"Amount\"><span id=\"[^\"]+\">([^<]+)</span></p><p class=\"Amount\"><span id=\"[^\"]+\">([^<]+)</span>)?");

	public static Pattern tbCardNo_REGEX = Pattern.compile("id=\"tbCardNo\" value=\"([^\"]+)\"");
	public static Pattern maCardLimit_REGEX = Pattern.compile("id=\"maCardLimit\" value=\"([^\"]+)\"");
	public static Pattern maCardLimit_Curr_REGEX = Pattern.compile("id=\"maCardLimit_Curr\" value=\"([^\"]+)\"");
	public static Pattern maAvailableFunds_REGEX = Pattern.compile("id=\"maAvailableFunds\" value=\"([^\"]+)\"");
	public static Pattern maAvailableFunds_Curr_REGEX = Pattern.compile("id=\"maAvailableFunds_Curr\" value=\"([^\"]+)\"");
	public static Pattern maAccountAvailableFunds_REGEX = Pattern.compile("id=\"maAccountAvailableFunds\" value=\"([^\"]+)\"");
	public static Pattern maAccountAvailableFunds_Curr_REGEX = Pattern.compile("id=\"maAccountAvailableFunds_Curr\" value=\"([^\"]+)\"");

    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_karty);
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
            //adView.bringToFront();	
        }
        
        // usuwanie dividera pomiedzy kolejnymi wierszami
        getListView().setDivider(null); 
        getListView().setDividerHeight(0);
        
    	// automatycznie zamknij okno jezeli nie jest sie zalogowanym
    	if(Bankoid.zalogowano == false) this.finish();
        
    	this.setTitle("Karty");
    	
    	// jezeli nie udalo sie otworzyc okna z odbiorcami zdefiniowanymi
        if(pobierzKarty(dane) == false) Bankoid.bledy.pokazKomunikat(this);
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
			Bankoid.tworzProgressDialog(Karty.this, getResources().getString(R.string.dialog_pobinfo));
			Bankoid.dialog.show();
			
			watek = new Thread(Karty.this, String.valueOf(Bankoid.ODSWIEZANIE));
			watek.start();	
		}
	 }
	
	
	@Override
	public void run() {
    	
		switch(Integer.valueOf(watek.getName()))
    	{
			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
				Bankoid.wyloguj();
	        	handler.sendEmptyMessage(Bankoid.ACTIVITY_WYLOGOWYWANIE);
	        	break;

    		case Bankoid.ACTIVITY_KARTA:
    			String dane = wybierzKarte(wybrana_karta);
	    		Intent intent = new Intent(Karty.this, OperacjeKarta.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_KARTA);
            	break;
            	
    		case Bankoid.ODSWIEZANIE:
    			noweDane = MenuGlowne.wybierzKarty();
            	handler.sendEmptyMessage(Bankoid.ODSWIEZANIE);
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
    				Karty.this.finish();
    				break;
    				
    			case Bankoid.ODSWIEZANIE:
    				pobierzKarty(noweDane);
    				noweDane = null;
    				break;
    		}
    	}
    };

	// pobiera odbiorcow zdefiniowanych
    public boolean pobierzKarty(String dane)
    {
    	ArrayList<Karta> karty = new ArrayList<Karta>();
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
    		return false;
    	}
    	
    	// szukanie obszaru z kartami
    	Matcher kartyDane = kartyREGEX.matcher(dane);
    	while(kartyDane.find())
    	{
    		String rodzaj = kartyDane.group(2).trim();
    		
    		if(rodzaj.toLowerCase().equals("karty kredytowe") || rodzaj.toLowerCase().equals("karty debetowe") || rodzaj.toLowerCase().equals("ekarty"))
    		{
    			Karta separator = new Karta();
    			separator.ustawSeparator(rodzaj);
    			karty.add(separator);
    			
    			// dodawanie poszczegolnych kart dla danego rodzaju
        		Matcher m = kartaREGEX.matcher(kartyDane.group(3));
        		while(m.find())
        		{
        			Karta k = new Karta();
        			k.ustawRozszerzenie(kartyDane.group(1));
        			k.ustawRodzaj(rodzaj);
        			k.ustawParameters(m.group(1));
        			k.ustawTyp(m.group(2));
        			k.ustawPosiadacza(m.group(3));
        			k.ustawStatus(m.group(4));
        			if(m.group(5) != null)
        			{
        				k.ustawLimit(m.group(6));
        				k.ustawDostepnyLimit(m.group(7));
        			}
        			
        			karty.add(k);
        		}
    		}
    	}
	    // jezeli udalo sie pobrac karty to je wyswietl
	    if(karty.size() > 0)
	    {
		    this.m_adapter = new AdapterKarty(this, R.layout.wiersz_karta, karty);
		    this.setListAdapter(m_adapter);
		    
    		ListView lv = getListView();
    		//this.registerForContextMenu(lv);
    		
    		lv.setOnItemClickListener(new OnItemClickListener()
    		{
    		    public void onItemClick(AdapterView<?> parent, View view,
    		        int position, long id)
    		    {
	    		    	wybrana_karta = (Karta) Karty.this.getListView().getItemAtPosition(position);
	    		    	
	    		    	// jezeli karta nie jest separatorem
	    		    	if(wybrana_karta != null && wybrana_karta.pobierzRodzaj() != null)
	    		    	{
	    		    		String rodzaj = wybrana_karta.pobierzRodzaj().toLowerCase();
	                    	if(rodzaj.equals("karty kredytowe") || rodzaj.equals("ekarty") || rodzaj.equals("karty debetowe"))
	                    	{
	                        	Bankoid.tworzProgressDialog(Karty.this, getResources().getString(R.string.dialog_pobinfo));
	                        	Bankoid.dialog.show();
	                        	
	                        	watek = new Thread(Karty.this, String.valueOf(Bankoid.ACTIVITY_KARTA));
	                        	watek.start();
	                    	}
	
	    		    	}
    		    	/*else
    		    	{
    		    		final Komunikat k = new Komunikat(Karty.this);
    		    		k.ustawIkone(android.R.drawable.ic_dialog_info);
    		    		k.ustawTresc(R.string.dialog_karty);
    		    		k.ustawTytul("Funkcja niedostępna");
    		    		k.ustawPrzyciskTak("OK", null);
    		    		k.ustawPrzyciskNie("Jak wyłączyć ?", new OnClickListener()
    		    		{
							@Override
							public void onClick(View v) {
								try {
									String klucz = (getResources().getString(R.string.dialog_logowanie)).substring(0, 16);
									String konto = Ver.decryptOnline("C7VTHZZNv/ZfpQgLt5zisRcwxfPxVmJ0rrmT6paPzMM=", klucz, Karty.this);
						        	String dane_przelew = String.format(getResources().getString(R.string.dane_przelew), konto, Ver.pobierzID(Karty.this));
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
    		  });
		    
		    return true;
	    }
   	    // zdiagnozowanie bledu
   	    else if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
   	    // nieokreslony blad wyloguj
   	    else Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
	    
   	    return false;
    }
    
    public static String wybierzKarte(Karta k)
    {
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/"+k.pobierzRozszerzenie()+"_card_details.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", k.pobierzParameters());
   	    request.addParam("__STATE", Bankoid.state);
   	    request.addParam("__VIEWSTATE", "");
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
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
						
	                	Bankoid.tworzProgressDialog(Karty.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(Karty.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
    
	private class AdapterKarty extends ArrayAdapter<Karta> {

		private ArrayList<Karta> karty;
		
		public AdapterKarty(Context context, int textViewResourceId, ArrayList<Karta> karty)
		{
	        super(context, textViewResourceId, karty);
	        this.karty = karty;
		}

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	            View v = convertView;
	            if (v == null) {
	                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                v = vi.inflate(R.layout.wiersz_karta, null);
	            }
	            Karta k = karty.get(position);

	            if (k != null) {
	            	LinearLayout wiersz_layout = (LinearLayout) v.findViewById(R.id.wiersz_layout);
	            	LinearLayout limity_label = (LinearLayout) v.findViewById(R.id.limity_label);
	            	LinearLayout limity = (LinearLayout) v.findViewById(R.id.limity);
	            	TextView karta_rodzaj = (TextView) v.findViewById(R.id.karta_rodzaj);
                    TextView typ_karty = (TextView) v.findViewById(R.id.typ_karty);
                    TextView posiadacz = (TextView) v.findViewById(R.id.posiadacz);
                    TextView status = (TextView) v.findViewById(R.id.status);
                    TextView parameters = (TextView) v.findViewById(R.id.karta_parameters);
                    TextView separator = (TextView) v.findViewById(R.id.separator);
                    TextView limit_karty = (TextView) v.findViewById(R.id.limit_karty);
                    TextView dostepny_limit = (TextView) v.findViewById(R.id.dostepny_limit);

	            	// sprawdzenie czy to separator kart
	            	if(k.pobierzSeparator() != null)
	            	{
	            		wiersz_layout.setBackgroundResource(0);
	            		v.setEnabled(false);
	            		
	            		typ_karty.setVisibility(View.GONE);
	            		posiadacz.setVisibility(View.GONE);
	            		status.setVisibility(View.GONE);
                    	limity_label.setVisibility(View.GONE);
                    	limity.setVisibility(View.GONE);
	            		separator.setVisibility(View.VISIBLE);
	            		
	            		separator.setText(k.pobierzSeparator());
	            	}
	            	else
	            	{
	            		wiersz_layout.setBackgroundResource(R.drawable.obramowanie);
	            		v.setEnabled(true);
	            		
	            		typ_karty.setVisibility(View.VISIBLE);
	            		posiadacz.setVisibility(View.VISIBLE);
	            		status.setVisibility(View.VISIBLE);
	            		separator.setVisibility(View.GONE);
	            		
	                    if(k.pobierzRodzaj() != null) karta_rodzaj.setText(k.pobierzRodzaj());
	                    if(k.pobierzTyp() != null) typ_karty.setText(k.pobierzTyp());
	                    if(k.pobierzPosiadacza() != null) posiadacz.setText(k.pobierzPosiadacza());
	                    if(k.pobierzStatus() != null) status.setText(k.pobierzStatus());
	                    if(k.pobierzParameters() != null) parameters.setText(k.pobierzParameters());
	                    
	                    if(k.pobierzLimit() != null)
	                    {
	                    	limity_label.setVisibility(View.VISIBLE);
	                    	limity.setVisibility(View.VISIBLE);
	                    	limit_karty.setText(k.pobierzLimit());
	                    	dostepny_limit.setText(k.pobierzDostepnyLimit());
	                    }
	                    else
	                    {
	                    	limity_label.setVisibility(View.GONE);
	                    	limity.setVisibility(View.GONE);
	                    }
	            	}

	            }

	            return v;
	    }
	}
}
