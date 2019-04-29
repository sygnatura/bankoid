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
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class OdbiorcyZdefiniowani extends ListActivity implements Runnable {

	private AdapterOdbiorcy m_adapter = null;
	
	private Thread watek;
	private Odbiorca wybrany_odbiorca;
	private AdView adView;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_odbiorcy_zdefiniowani);
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
        
    	this.setTitle("Odbiorcy zdefiniowani");
    	// jezeli nie udalo sie otworzyc okna z odbiorcami zdefiniowanymi
        if(pobierzOdbiorcow(dane) == false) Bankoid.bledy.pokazKomunikat(this);

    }
    
    
    @Override
	 protected void onResume()
	 {
    	// automatycznie zamknij okno jezeli nie jest sie zalogowanym
    	if(Bankoid.zalogowano == false) this.finish();
		super.onResume();
	 }
	
	
	@Override
	public void run() {
    	
		switch(Integer.valueOf(watek.getName()))
    	{
			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
				Bankoid.wyloguj();
	        	handler.sendEmptyMessage(Bankoid.ACTIVITY_WYLOGOWYWANIE);
	        	break;

    		case Bankoid.ACTIVITY_PRZELEW_ZDEFINIOWANY:
    			String dane = przelewDoOdbiorcy(wybrany_odbiorca);
	    		Intent intent = new Intent(OdbiorcyZdefiniowani.this, PrzelewZdefiniowany.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_PRZELEW_ZDEFINIOWANY);
            	break;
            	
    	}
	}
	
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {

    		try
    		{
    			Bankoid.dialog.dismiss();
    		}catch(Exception e){}
    		
    		if(msg.what == Bankoid.ACTIVITY_WYLOGOWYWANIE) OdbiorcyZdefiniowani.this.finish();
    	}
    };

	// pobiera odbiorcow zdefiniowanych
    public boolean pobierzOdbiorcow(String dane)
    {
    	TextView naglowek_label = (TextView) this.findViewById(R.id.naglowek_label);
    	Pattern odbiorcaREGEX = Pattern.compile("<p class=\"TransferName\"><a id=[^>]+>(.+?)</a></p><p class=\"RecipientName\"><span id=\"[^\"]+\">(.+?)</span></p><p class=\"Numeric\"><span id=\"[^\"]+\">(\\d*)</span></p><p class=\"TransferName\"><span id=\"[^\"]+\">(.*?)</span></p><p class=\"Actions\"><a .+?'([^']{40,})'[^>]+>");

    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.odbiorcy_blad, Bledy.WYLOGUJ);
    		return false;
    	}

    	
    	// udalo sie otworzyc okno z odbiorcami zdefiniowanymi
   	    if(dane.contains(getResources().getString(R.string.odbiorcy_brak))) return true;
   	    
    	ArrayList<Odbiorca> odbiorcy = new ArrayList<Odbiorca>();
    	//odczytanie poszczegolnych odbiorcow
    	Matcher m = odbiorcaREGEX.matcher(dane);
    	while(m.find())
    	{
    		Odbiorca o = new Odbiorca();
    		o.ustawNazwe(m.group(1).trim());
    		o.ustawNazwisko(m.group(2).trim());
    		o.ustawNR(m.group(3).trim());
    		o.ustawNazweIVR(m.group(4).trim());
    		o.ustawParameters(m.group(5));
    		
    		odbiorcy.add(o);
    	}
	    // jezeli udalo sie pobrac rachunki to je wyswietl
	    if(odbiorcy.size() > 0)
	    {
	    	// pokaz label wykonaj przelew do
	    	naglowek_label.setVisibility(View.VISIBLE);
		    this.m_adapter = new AdapterOdbiorcy(this, R.layout.wiersz_odbiorcy_zdefiniowani, odbiorcy);
		    this.setListAdapter(m_adapter);
		    
    		ListView lv = getListView();
    		//this.registerForContextMenu(lv);
    		
    		lv.setOnItemClickListener(new OnItemClickListener() {
    		    public void onItemClick(AdapterView<?> parent, View view,
    		        int position, long id) {
    		    	

        		    	wybrany_odbiorca = (Odbiorca) OdbiorcyZdefiniowani.this.getListView().getItemAtPosition(position);
        		    	
        		    	if(wybrany_odbiorca != null)
        		    	{
                        	Bankoid.tworzProgressDialog(OdbiorcyZdefiniowani.this, getResources().getString(R.string.dialog_pobinfo));
                        	Bankoid.dialog.show();
                        	
                        	watek = new Thread(OdbiorcyZdefiniowani.this, String.valueOf(Bankoid.ACTIVITY_PRZELEW_ZDEFINIOWANY));
                        	watek.start();
        		    	}
    		    	/*
    		    	else
    		    	{
    		    		final Komunikat k = new Komunikat(OdbiorcyZdefiniowani.this);
    		    		k.ustawIkone(android.R.drawable.ic_dialog_info);
    		    		k.ustawTresc(R.string.dialog_odbiorcy_zdefiniowani);
    		    		k.ustawTytul("Funkcja niedostępna");
    		    		k.ustawPrzyciskTak("OK", null);
    		    		k.ustawPrzyciskNie("Jak wyłączyć ?", new OnClickListener()
    		    		{
							@Override
							public void onClick(View v) {
								try {
									String klucz = (getResources().getString(R.string.dialog_logowanie)).substring(0, 16);
									String konto = Ver.decryptOnline("C7VTHZZNv/ZfpQgLt5zisRcwxfPxVmJ0rrmT6paPzMM=", klucz, OdbiorcyZdefiniowani.this);
						        	String dane_przelew = String.format(getResources().getString(R.string.dane_przelew), konto, Ver.pobierzID(OdbiorcyZdefiniowani.this));
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
   	    else Bankoid.bledy.ustawBlad(R.string.odbiorcy_blad, Bledy.WYLOGUJ);

	    // ukryj naglowek wykonaj przelew do
	    naglowek_label.setVisibility(View.GONE);
	    
   	    return false;
    }
    
    private String przelewDoOdbiorcy(Odbiorca odbiorca)
    {
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/defined_transfer_exec.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", odbiorca.pobierzParameters());
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
    	    
   	    String rezultat = request.getResult();
   	    Bankoid.pobierzState(rezultat);
   	    
   	    if(rezultat.contains("Przelew do odbiorcy zdefiniowanego")) return rezultat;
   	    // jezeli to niestandardowy blad to wyloguj
   	    else if(Bankoid.bledy.czyBlad() == false)
	    {
	    	Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
	    }
   	    return null;
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
						
	                	Bankoid.tworzProgressDialog(OdbiorcyZdefiniowani.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(OdbiorcyZdefiniowani.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
    
	private class AdapterOdbiorcy extends ArrayAdapter<Odbiorca> {

		private ArrayList<Odbiorca> odbiorcy;
		
		public AdapterOdbiorcy(Context context, int textViewResourceId, ArrayList<Odbiorca> odbiorcy)
		{
	        super(context, textViewResourceId, odbiorcy);
	        this.odbiorcy = odbiorcy;
		}

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	            View v = convertView;
	            if (v == null) {
	                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                v = vi.inflate(R.layout.wiersz_odbiorcy_zdefiniowani, null);
	            }
	            Odbiorca o = odbiorcy.get(position);
	            
	            if (o != null) {
	                    TextView nazwa_odbiorcy = (TextView) v.findViewById(R.id.nazwa_odbiorcy);
	                    TextView odbiorca = (TextView) v.findViewById(R.id.odbiorca);
	                    TextView nr_ivr = (TextView) v.findViewById(R.id.nr_ivr);
	                    TextView label_nazwa = (TextView) v.findViewById(R.id.label_nazwa);
	                    TextView nazwa_ivr = (TextView) v.findViewById(R.id.nazwa_ivr);
	                    TextView parameters = (TextView) v.findViewById(R.id.przelew_parameters);
	            
	                    if(o.pobierzNazwe() != null) nazwa_odbiorcy.setText(o.pobierzNazwe());
	                    if(o.pobierzNazwisko() != null) odbiorca.setText(o.pobierzNazwisko());
	                    if(o.pobierzParameters() != null) parameters.setText(o.pobierzParameters());
	                    if(o.pobierzNR() != null)
	                    {
	                    	nr_ivr.setVisibility(View.VISIBLE);
	                    	nr_ivr.setText(o.pobierzNR());
	                    }
	                    else
	                    {
	                    	nr_ivr.setVisibility(View.GONE);
	                    }
	                    if(o.pobierzNazweIVR() != null)
	                    {
	                    	label_nazwa.setVisibility(View.VISIBLE);
	                    	nazwa_ivr.setVisibility(View.VISIBLE);
	                    	nazwa_ivr.setText(o.pobierzNazweIVR());
	                    }
	                    else
	                    {
	                    	label_nazwa.setVisibility(View.GONE);
	                    	nazwa_ivr.setVisibility(View.GONE);
	                    }
	            }
	            return v;
	    }
	}
}
