package pl.bankoid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class OperacjeRachunek extends Activity implements Runnable {

	private Thread watek;
	private String noweDane = null;
	public static boolean odswiez = false;
	private AdView adView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rachunek);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");
    	
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        // reklama
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
        ///////////////////////////
        
        // automatycznie zamknij okno jezeli brak zalogowania
        if(Bankoid.zalogowano == false) this.finish();
        
        if(wybierzRachunek(dane))
        {
        	if(MenuGlowne.wybrany != null) this.setTitle("Wybrany rachunek: " + MenuGlowne.wybrany.pobierzSkroconaNazwe());
            //Log.v("rachpar", MenuGlowne.wybrany.pobierzParameters());
            //Log.v("rachnaz", MenuGlowne.wybrany.pobierzNazwe());
        }
        // pokaz blad
        else
        {
        	Bankoid.bledy.pokazKomunikat(this);
        }
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
			// odswiez rowniez glowne menu
			MenuGlowne.odswiez = true;
			Bankoid.tworzProgressDialog(OperacjeRachunek.this, getResources().getString(R.string.dialog_pobinfo));
			Bankoid.dialog.show();
			
			watek = new Thread(OperacjeRachunek.this, String.valueOf(Bankoid.ODSWIEZANIE));
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
    	
    		case Bankoid.ACTIVITY_HISTORIA:
    			String dane = wybierzHistorie();
	    		Intent intent = new Intent(OperacjeRachunek.this, ListaHistoria.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_HISTORIA);
            	break;
            	
    		case Bankoid.ACTIVITY_BLOKADY:
    			dane = wybierzBlokady();
	    		intent = new Intent(OperacjeRachunek.this, ListaBlokady.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_BLOKADY);
            	break;            	
            	
    		case Bankoid.ACTIVITY_PRZELEWY:
    			dane = wybierzPrzelewy();
	    		intent = new Intent(OperacjeRachunek.this, Przelewy.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_PRZELEWY);
            	break;

    		case Bankoid.ACTIVITY_ODBIORCY_ZDEFINIOWANI:
    			dane = wybierzOdbiorcyZdefiniowani();
	    		intent = new Intent(OperacjeRachunek.this, OdbiorcyZdefiniowani.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);
            	handler.sendEmptyMessage(Bankoid.ACTIVITY_ODBIORCY_ZDEFINIOWANI);
            	break;	
            	
    		case Bankoid.ACTIVITY_DOLADUJ_TELEFON:
    			dane = wybierzDoladujTelefon();
	    		intent = new Intent(OperacjeRachunek.this, DoladujTelefon.class);
	    		intent.putExtra("dane", dane);
	    		startActivity(intent);

            	handler.sendEmptyMessage(Bankoid.ACTIVITY_DOLADUJ_TELEFON);
            	break;
            	
    		case Bankoid.ODSWIEZANIE:
    			noweDane = MenuGlowne.wybierzRachunek();
            	handler.sendEmptyMessage(Bankoid.ODSWIEZANIE);
            	break;
    	}
    	
    }
    
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {

    		switch(msg.what)
    		{
    			case Bankoid.ACTIVITY_WYLOGOWYWANIE:
					 OperacjeRachunek.this.finish();
					 break;

    			case Bankoid.ODSWIEZANIE:
    				wybierzRachunek(noweDane);
    				noweDane = null;
    				break;
    		}

    		try
    		{
    			Bankoid.dialog.dismiss();
    		}catch(Exception e){}
    	}
    };
    
    
    // wybiera dany rachunek
    public boolean wybierzRachunek(String dane)
    {
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.rachunek_blad, Bledy.WYLOGUJ);
    		return false;
    	}

	    if(dane.contains("Wybrany rachunek"))
	    {
	    	odswiezOkno(dane);
	    	tworzPrzyciski(dane);
	    	return true;
	    }
    	else if(Bankoid.bledy.czyBlad())
    	{
    		// zamknij okno poniwaz nie pobrano info o rachunku
    		Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    	}
    	else
    	{
    		// nieokreslony blad wyloguj
    		Bankoid.bledy.ustawBlad(R.string.rachunek_blad, Bledy.WYLOGUJ);
    	}
	    return false;
    }
    
    public void tworzPrzyciski(String dane)
    {
    	Pattern szukajOperacje = Pattern.compile("Rachunki</a><ul class=\"dynamic\">.+?</ul>", Pattern.DOTALL);
    	Pattern operacjaREGEX = Pattern.compile("<li( class=\"selected\")?><a onclick[^>]+>([^<]+)</a></li>");
    	
    	// przyciski
    	Button przycisk_historia = (Button) this.findViewById(R.id.przycisk_historia);
    	Button przycisk_blokady = (Button) this.findViewById(R.id.przycisk_blokady);
    	Button przycisk_przelew_jednorazowy = (Button) this.findViewById(R.id.przycisk_przelew_jednorazowy);
    	Button przycisk_odbiorcy = (Button) this.findViewById(R.id.przycisk_odbiorcy);
    	Button przycisk_doladuj_telefon = (Button) this.findViewById(R.id.przycisk_doladuj_telefon);
    	
    	// sliding
    	final SlidingDrawer sliding = (SlidingDrawer) this.findViewById(R.id.sliding);
    	
    	// aktywuje kolejne przyciski dostepne dla rachunku
    	Matcher m = szukajOperacje.matcher(dane);
    	if(m.find())
    	{
    		dane = m.group();
    		m = operacjaREGEX.matcher(dane);
    		// jezeli znaleziono operacje
    		while(m.find())
    		{
    			// jezeli operacja jest na liscie dozwolonych to uaktywnij dany przycisk
    			if(czyOperacjaDostepna(m.group(2)))
    			{
    				if(m.group(2).equals("Historia")) przycisk_historia.setVisibility(View.VISIBLE);
    				else if(m.group(2).equals("Przelewy")) przycisk_przelew_jednorazowy.setVisibility(View.VISIBLE);
    				else if(m.group(2).equals("Odbiorcy zdefiniowani")) przycisk_odbiorcy.setVisibility(View.VISIBLE);
    				else if(m.group(2).equals("Doładuj telefon")) przycisk_doladuj_telefon.setVisibility(View.VISIBLE);
    			}

    		}
    		
    		if(MenuGlowne.wybrany.pobierzSkroconaNazwe().equals("eKONTO")) przycisk_blokady.setVisibility(View.VISIBLE);
    		
    		if(przycisk_historia.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Historia
        		przycisk_historia.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeRachunek.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeRachunek.this, String.valueOf(Bankoid.ACTIVITY_HISTORIA));
    					watek.start();
    				}
        			
        		});
    		}
    		if(przycisk_blokady.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Historia
        		przycisk_blokady.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeRachunek.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeRachunek.this, String.valueOf(Bankoid.ACTIVITY_BLOKADY));
    					watek.start();
    				}
        			
        		});
    		}
    		if(przycisk_przelew_jednorazowy.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Przelew jednorazowy    		
        		przycisk_przelew_jednorazowy.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeRachunek.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeRachunek.this, String.valueOf(Bankoid.ACTIVITY_PRZELEWY));
    					watek.start();
    				}
        			
        		});
    		}
    		
    		if(przycisk_odbiorcy.getVisibility() == View.VISIBLE)
    		{
    	   		// akcja dla przycisku Odbiorcy zdefiniowani
        		przycisk_odbiorcy.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeRachunek.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeRachunek.this, String.valueOf(Bankoid.ACTIVITY_ODBIORCY_ZDEFINIOWANI));
    					watek.start();
    				}
        			
        		});
    		}
 
    		if(przycisk_doladuj_telefon.getVisibility() == View.VISIBLE)
    		{
        		// akcja dla przycisku Doladuj telefon
        		przycisk_doladuj_telefon.setOnClickListener(new OnClickListener()
        		{
    				@Override
    				public void onClick(View v) {
    					sliding.animateClose();
    					Bankoid.tworzProgressDialog(OperacjeRachunek.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(OperacjeRachunek.this, String.valueOf(Bankoid.ACTIVITY_DOLADUJ_TELEFON));
    					watek.start();	
    				}
        			
        		});
    		}
    	}
    }
    
    private boolean czyOperacjaDostepna(String operacja)
    {
    	String[] spisDostepnychOperacji = this.getResources().getStringArray(R.array.dostepne_operacje);
    	String rachunekRodzaj = ((TextView) this.findViewById(R.id.rachunek_rodzaj)).getText().toString();
    	
    	// jezeli rachunek nie jest rachunkiem biezacym i dostepna jest opcja przelewy to jej nie dodwaja
    	if(operacja.equalsIgnoreCase("Przelewy") && rachunekRodzaj.equalsIgnoreCase("RACHUNEK BIEŻĄCY") == false) return false;
    	else
    	{
        	for(String dozwolona : spisDostepnychOperacji)
        	{
        		if(operacja.equalsIgnoreCase(dozwolona)) return true;
        	}
    	}
    	
    	return false;
    }
    
    public void odswiezOkno(String dane)
    {
    	Pattern wpisyREGEX = Pattern.compile("<label class=\"label\">([^:]+):</label><div class=\"content\">(.+?)</div>", Pattern.DOTALL);
    	Pattern posiadaczREGEX = Pattern.compile("<li>([^<]+)</li>");
    	Pattern kwotaREGEX = Pattern.compile("<span[^>]+class=\"text amount\">([^<]+)</span>");
    	Matcher m = wpisyREGEX.matcher(dane);
    	
    	while(m.find())
    	{
    		String nazwa = m.group(1).trim();
    		String wartosc = m.group(2).trim();
    		
    		if(nazwa.equals("Nazwa rachunku"))
    		{
    			((TextView) findViewById(R.id.rachunek_nazwa)).setText(wartosc);
    		}
    		else if(nazwa.equals("Numer rachunku"))
    		{
    			((TextView) findViewById(R.id.rachunek_numer)).setText(wartosc);
    		}
    		else if(nazwa.equals("Numer rachunku IBAN"))
    		{
    			((TextView) findViewById(R.id.rachunek_numer_iban)).setText(wartosc);
    		}
    		else if(nazwa.equals("Numer BIC"))
    		{
    			((TextView) findViewById(R.id.rachunek_numer_bic)).setText(wartosc);
    		}
    		else if(nazwa.equals("Rodzaj rachunku"))
    		{
    			((TextView) findViewById(R.id.rachunek_rodzaj)).setText(wartosc);
    		}
    		else if(nazwa.equals("Posiadacz rachunku"))
    		{
    			Matcher posiadacz = posiadaczREGEX.matcher(wartosc);
    			StringBuilder lista = new StringBuilder();
    			while(posiadacz.find())
    			{
    				lista.append(posiadacz.group(1)+", ");
    			}
    			wartosc = lista.toString().substring(0, lista.length()-2);
    			((TextView) findViewById(R.id.rachunek_posiadacz)).setText(wartosc);
    		}
    		else if(nazwa.equals("Pełnomocnik rodzajowy"))
    		{
    			Matcher posiadacz = posiadaczREGEX.matcher(wartosc);
    			StringBuilder lista = new StringBuilder();
    			while(posiadacz.find())
    			{
    				lista.append(posiadacz.group(1)+", ");
    			}
    			if(lista.length() > 0)
    			{
    				wartosc = lista.toString().substring(0, lista.length()-2);
    			}
    			((TextView) findViewById(R.id.rachunek_pelnomocnik)).setText(wartosc);
    		}
    		else if(nazwa.equals("Rola klienta"))
    		{
    			((TextView) findViewById(R.id.rachunek_rola_klienta)).setText(wartosc);
    		}
    		else if(nazwa.equals("Saldo"))
    		{
    			Matcher kwota = kwotaREGEX.matcher(wartosc);
    			if(kwota.find()) wartosc = kwota.group(1);
    			else wartosc = "Brak";
    			((TextView) findViewById(R.id.rachunek_saldo)).setText(wartosc);
    		}
    		else if(nazwa.equals("Dostępne środki"))
    		{
    			Matcher kwota = kwotaREGEX.matcher(wartosc);
    			if(kwota.find()) wartosc = kwota.group(1);
    			else wartosc = "Brak";
    			((TextView) findViewById(R.id.rachunek_srodki)).setText(wartosc);
    		}
    	}
    }
    
    public String wybierzHistorie()
    {
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/account_oper_list.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
   	    
   	    String rezultat = request.getResult();
    	
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    public static String wybierzPrzelewy()
    {
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/transfer_exec.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
    	
   	    String rezultat = request.getResult();
   	    
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    public String wybierzOdbiorcyZdefiniowani()
    {
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/defined_transfers_list.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
    	    
   	    String rezultat = request.getResult();
   	    Bankoid.pobierzState(rezultat);
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    
   	    return rezultat;
    }
    
    public String wybierzDoladujTelefon()
    {
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/prepaid_handling.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
   	    request.execute();
    	    
   	    String rezultat = request.getResult();
   	    Bankoid.pobierzEventvalidation(rezultat);
   	    Bankoid.pobierzState(rezultat);
   	    
   	    return rezultat;
    }
    
    public String wybierzBlokady()
    {
    	wybierzHistorie();
   	   	sfRequest request = sfClient.getInstance().createRequest();
   	    request.setUrl("https://www.mbank.com.pl/witholdings_list.aspx");
   	    request.setMethod("POST");
   	    request.addParam("__PARAMETERS", "");
   	    request.addParam("__STATE", Bankoid.state);
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
						
	                	Bankoid.tworzProgressDialog(OperacjeRachunek.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(OperacjeRachunek.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
