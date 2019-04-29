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

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class ListaBlokady extends ListActivity implements Runnable {

	private ArrayList<Blokada> m_blokady = new ArrayList<Blokada>();
	private AdapterBlokada m_adapter = null;
	private AdView adView;

	private Thread watek;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_blokad);
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
            adView.loadAd(new AdRequest());
            //adView.bringToFront();	
        }
        ////////////////////
        
        // usuwanie dividera pomiedzy kolejnymi wierszami
        getListView().setDivider(null); 
        getListView().setDividerHeight(0);
        
    	// automatycznie zamknij okno jezeli nie jest sie zalogowanym
    	if(Bankoid.zalogowano == false) this.finish();
        
    	// jezeli nie udalo sie otworzyc okno z param hisotrii
        if(pobierzBlokady(dane) == false) Bankoid.bledy.pokazKomunikat(this);

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
					 ListaBlokady.this.finish();
					 break;

    		}
    	}
    };
    
    // pobiera blokady
    public boolean pobierzBlokady(String dane)
    {  	
   	    // REGEX
    	Pattern blokadyREGEX = Pattern.compile("<p class=\"Date\"><span id=[^>]+>([^<]+)</span></p><p class=\"Date\"><span id=[^>]+>([^<]+)</span></p><p class=\"Amount\"><span id=[^>]+>([^<]+)</span></p><p class=\"WitholdingDescription\"><span id=[^>]+>([^<]+)</span></p><p class=\"WitholdingType\"><span id=[^>]+>([^<]+)</span></p>");
    	
   	    // udalo sie otworzyc okno z lista operacji
   	    if(dane.contains("Lista blokad"))
   	    {
   	    	this.setTitle("Lista blokad");
   	    	
    		Matcher m = blokadyREGEX.matcher(dane);
       	    while(m.find())
       	    {
       	    	Blokada b = new Blokada();
       	    	b.ustawDateRejestracji(m.group(1));
       	    	b.ustawDateZakonczenia(m.group(2));
       	    	b.ustawKwoteOperacji(m.group(3));
       	    	b.ustawOpisBlokady(m.group(4));
       	    	b.ustawTypBlokady(m.group(5));
       	    	
       	    	m_blokady.add(b);
       	    }
       	    
       	    if(m_blokady.size() > 0)
       	    {
        	    m_adapter = new AdapterBlokada(getApplicationContext(), R.layout.wiersz_blokad, m_blokady);
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
						
	                	Bankoid.tworzProgressDialog(ListaBlokady.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(ListaBlokady.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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



	private class AdapterBlokada extends ArrayAdapter<Blokada> {
	
		private ArrayList<Blokada> blokady;
		
		public AdapterBlokada(Context context, int textViewResourceId, ArrayList<Blokada> blokady)
		{
	        super(context, textViewResourceId, blokady);
	        this.blokady = blokady;
		}
	
		
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	            View v = convertView;
	            if (v == null) {
	                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                v = vi.inflate(R.layout.wiersz_blokad, null);
	            }
	            Blokada b = blokady.get(position);
	            
	            if (b != null)
	            {
	            	TextView data_rejestracji = (TextView) v.findViewById(R.id.data_rejestracji);
	            	TextView kwota = (TextView) v.findViewById(R.id.kwota);
	            	TextView data_zakonczenia = (TextView) v.findViewById(R.id.data_zakonczenia);
	                TextView typ_blokady = (TextView) v.findViewById(R.id.typ_blokady);
	                TextView opis_blokady = (TextView) v.findViewById(R.id.opis_blokady);
	            	
	                if(b.pobierzDateRejestracji() != null) data_rejestracji.setText(b.pobierzDateRejestracji());
                	if(b.pobierzKwoteOperacji() != null)
                	{
                		kwota.setText(b.pobierzKwoteOperacji());
                    	kwota.setBackgroundResource(R.drawable.kwota_ujemna);
    	                kwota.setTextColor(ListaBlokady.this.getResources().getColor(R.color.wartosc_ujemna));
                	}
	                if(b.pobierzDateZakonczenia() != null) data_zakonczenia.setText(b.pobierzDateZakonczenia());
	                if(b.pobierzTypBlokady() != null) typ_blokady.setText(b.pobierzTypBlokady());
	                if(b.pobierzOpisBlokady() != null) opis_blokady.setText(b.pobierzOpisBlokady());
	            }
	            return v;
	    }
	}

}
