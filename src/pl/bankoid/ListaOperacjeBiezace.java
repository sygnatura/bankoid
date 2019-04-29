package pl.bankoid;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class ListaOperacjeBiezace extends ListActivity implements Runnable {

	private ArrayList<Historia> m_historia = new ArrayList<Historia>();
	private AdapterHistoria m_adapter = null;
	private AdView adView;
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
            //adView.bringToFront();	
        }
        ////////////////////
        
        // usuwanie dividera pomiedzy kolejnymi wierszami
        getListView().setDivider(null); 
        getListView().setDividerHeight(0);
        
    	// automatycznie zamknij okno jezeli nie jest sie zalogowanym
    	if(Bankoid.zalogowano == false) this.finish();
        
    	// jezeli nie udalo sie otworzyc okno z param hisotrii
        if(pobierzHistorie(dane) == false) Bankoid.bledy.pokazKomunikat(this);

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
					 ListaOperacjeBiezace.this.finish();
					 break;

    		}
    	}
    };
    
    // pobiera historie z podanymi parametrami
    public boolean pobierzHistorie(String dane)
    {  	
   	    // REGEX
    	Pattern tytulREGEX = Pattern.compile("<div id=\"cc_current_operations[^\"]*\" class=\"[^\"]+\">.+?<h3>.+?</a>(.+?)</h3>", Pattern.DOTALL);
    	Pattern historia_kartyREGEX = Pattern.compile("<li( class=\"alternate\")?><p class=\"CardNumber\"><span id=[^>]+>([^<]*)</span></p><p class=\"Date\"><span id=[^>]+>([^<]+)</span><span>([^<]+)</span></p><p class=\"OperationType\"><a [^>]+>([^<]+)</a></p><p class=\"Merchant\"><span id=[^>]+>([^<]*)</span></p><p class=\"Amount\"><span id=[^>]+>([^<]+)</span></p><p class=\"Amount\"><span id=[^>]+>([^<]+)</span>");
    	Pattern sumaREGEX = Pattern.compile("Suma obciążeń</span></p><p class=\"Date\">[^<]*</p><p [^<]*</p><p [^<]*</p><p [^<]*</p><p [^<]*<span [^>]*>([^<]+)</span>");
    	
    	TextView suma_obciazen = (TextView) findViewById(R.id.suma_obciazen);
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.historia_blad, Bledy.WYLOGUJ);
    		return false;
    	}
    	
    	Matcher m = tytulREGEX.matcher(dane);
    	
   	    // udalo sie otworzyc okno z lista operacji
   	    if(m.find())
   	    {
   	    	this.setTitle(m.group(1).trim());
   	    	
    		m = historia_kartyREGEX.matcher(dane);
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
       	    
       	    m = sumaREGEX.matcher(dane);
       	    if(m.find())
       	    {
       	    	suma_obciazen.setText("Suma obciążeń: "+m.group(1));
       	    	suma_obciazen.setVisibility(View.VISIBLE);
       	    }
       	    
       	    if(m_historia.size() > 0)
       	    {
        	    m_adapter = new AdapterHistoria(getApplicationContext(), R.layout.wiersz_historia, m_historia);
        	    setListAdapter(m_adapter);
       	    }

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
						
	                	Bankoid.tworzProgressDialog(ListaOperacjeBiezace.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(ListaOperacjeBiezace.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
	            	dane.setVisibility(View.GONE);
	            	
	        		data.setText(h.pobierzDateOperacji());
	        		kwota.setText(h.pobierzKwoteOperacji()+ " / " + h.pobierzSaldoPoOperacji());
	                if(kwota.getText().toString().startsWith("-"))
	                {
	                	kwota.setBackgroundResource(R.drawable.kwota_ujemna);
	                	kwota.setTextColor(ListaOperacjeBiezace.this.getResources().getColor(R.color.wartosc_ujemna));
	                }
	                else
	                {
	                	kwota.setTextColor(ListaOperacjeBiezace.this.getResources().getColor(R.color.wartosc_dodatnia));
	                	kwota.setBackgroundResource(R.drawable.kwota_dodatnia);
	                }
	                rodzaj.setText(h.pobierzRodzajPrzelewu());
	            	if(h.pobierzTytulPrzelewu() != null)
	            	{
	            		tytul.setText(h.pobierzTytulPrzelewu());
	            		tytul.setVisibility(View.VISIBLE);
	           		}else tytul.setVisibility(View.GONE);
	                
	            	if(h.pobierzRachunekNadawcy() != null)
	            	{
	            		rachunek.setText("Numer karty: " + h.pobierzRachunekNadawcy());
	            		rachunek.setVisibility(View.GONE);
	            	}
	            	else rachunek.setVisibility(View.GONE);
	
	            }
	            return v;
	    }
	}

	@Override
	protected void onDestroy() {
		if(adView != null) adView.destroy();
		super.onDestroy();
	}
	
}
