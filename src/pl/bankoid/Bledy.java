package pl.bankoid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;

public class Bledy {

	final static int BRAK = 0;
	final static int INFO = 1;
	final static int WYLOGUJ = 2;
	final static int ZAMKNIJ_OKNO = 3;
	final static int POWROT = 4;
	final static int OK = 5;
	
	private String bladTytul;
	private String bladTresc;
	private int kodBledu = BRAK;
	private int kolorTytul = -1;
	private int kolorTresc = -1;
	private int ikona = -1;
	
	private final Pattern bladsesjiREGEX = Pattern.compile("<div id=\"errorView\" class=\"error noSession\">.+?<h3>(.+?)</h3><fieldset>.+?<p class=\"message\">([^<]+)</p>", Pattern.DOTALL);
	//private final Pattern bladsesjiREGEX = Pattern.compile("<div id=\"errorView\" class=\"error noSession\">.+?<h3[^>]*>(.+?)</h3>.*?<fieldset>.*?<p class=\"message\"[^>]*>(.+?)</p>.*?</fieldset>", Pattern.DOTALL);
	
	
	
	public void ustawBlad(int tytulID, int trescID, int bladID)
	{
		this.ustawTytul(tytulID);
		this.ustawTresc(trescID);
		this.ustawKodBledu(bladID);
	}
	
	public void ustawBlad(String tytul, String tresc, int bladID)
	{
		this.ustawTytul(tytul);
		this.ustawTresc(tresc);
		this.ustawKodBledu(bladID);
	}
	
	public void ustawBlad(int trescID, int bladID)
	{
		this.ustawTytul("Błąd");
		this.ustawTresc(trescID);
		this.ustawKodBledu(bladID);
	}
	
	public void ustawKodBledu(int blad)
	{
		// USTAW NOWY KOD BLEDU TYLKO W PRZYPADKU GDY NIE JEST WYMAGANE WYLOGOWANIE
		if(kodBledu != WYLOGUJ)
			kodBledu = blad;
	}
	
	public void ustawKolorTytul(int kolor)
	{
		kolorTytul = kolor;
	}
	
	public void ustawKolorTresc(int kolor)
	{
		kolorTresc = kolor;
	}
	
	public void ustawIkone(int id)
	{
		ikona = id;
	}
	
	public int pobierzKodBledu()
	{
		return this.kodBledu;
	}
	
	public void ustawTytul(String tytul)
	{
		bladTytul = tytul;
	}
	
	public void ustawTresc(String tresc)
	{
		bladTresc = tresc;
	}
	
	public void ustawTytul(int id)
	{
		
		bladTytul = Bankoid.context.getResources().getString(id);
	}
	
	public void ustawTresc(int id)
	{
		bladTresc = Bankoid.context.getResources().getString(id);
	}
	
	public void diagnozujBlad(String dane)
	{
		if(dane == null || dane.equals("timeout"))
    	{
    		bladTytul = Bankoid.context.getResources().getString(R.string.blad_polaczenie_tytul);
    		bladTresc = Bankoid.context.getResources().getString(R.string.blad_timeout);
    		kodBledu = INFO;
    	}
    	else if(dane.startsWith("IllegalArgumentException"))
    	{
    		bladTytul = Bankoid.context.getResources().getString(R.string.blad_polaczenie_tytul);
    		bladTresc = dane;
    		kodBledu = INFO;
    	}
    	else if(dane.startsWith("IllegalStateException"))
    	{
    		bladTytul = Bankoid.context.getResources().getString(R.string.blad_polaczenie_tytul);
    		bladTresc = dane;
    		kodBledu = INFO;
    	}
    	else if(dane.equals("dns"))
    	{
    		bladTytul = Bankoid.context.getResources().getString(R.string.blad_polaczenie_tytul);
    		bladTresc = Bankoid.context.getResources().getString(R.string.blad_dns);
    		kodBledu = INFO;
    	}
    	else
    	{
    		// ALERT ZABEZPIECZEN
    		Matcher m = bladsesjiREGEX.matcher(dane);
    		if(m.find())
    		{
    			bladTytul = (Html.fromHtml(m.group(1)).toString()).trim();
    			bladTresc = (Html.fromHtml(m.group(2)).toString()).trim();
    			ustawKolorTytul(Bankoid.context.getResources().getColor(R.color.tytul_blad));
    			ustawKolorTresc(Bankoid.context.getResources().getColor(R.color.tresc_blad));
    			kodBledu = WYLOGUJ;
    		}
    	}
	}
	
	public boolean czyBlad()
	{
		if(kodBledu != BRAK && bladTytul != null & bladTresc != null) return true;
		else return false;
	}
	
	public void pokazKomunikat(final Activity activity)
	{
		// jezeli jest jakis blad to go pokaz
		if(czyBlad())
		{
			// utworzenie nowego komunikatu
			Komunikat komunikat = new Komunikat(activity);
			// ustawienie koloru tresci i tytulu
			if(kolorTytul != -1) komunikat.ustawKolorTytul(kolorTytul);
			if(kolorTresc != -1) komunikat.ustawKolorTresc(kolorTresc);
			// ustawienie ikony
			if(ikona != -1) komunikat.ustawIkone(ikona);
			else komunikat.ustawIkone(android.R.drawable.ic_dialog_alert);
			komunikat.ustawTytul(bladTytul);
			komunikat.ustawTresc(bladTresc);
			komunikat.setCancelable(false);
			
			try
			{
				switch(kodBledu)
				{
					case INFO:
						komunikat.ustawPrzyciskTak("Zamknij", null);
						break;

					case POWROT:
						komunikat.ustawPrzyciskTak("Powrót", null);
						break;
							
					case WYLOGUJ:
						komunikat.ustawKolorTytul(Bankoid.context.getResources().getColor(R.color.tytul_blad));
						komunikat.ustawKolorTresc(Bankoid.context.getResources().getColor(R.color.tresc_blad));
						komunikat.ustawPrzyciskTak("Zamknij", new OnClickListener()
						{

							@Override
							public void onClick(View v) {
		    	            	Bankoid.wyloguj();
		    	            	activity.finish();
							}
							
						});
						break;
						
					case ZAMKNIJ_OKNO:
						komunikat.ustawPrzyciskTak("Zamknij", new OnClickListener()
						{
							@Override
							public void onClick(View v) {
		    	            	activity.finish();
							}
							
						});
						break;
						
					case OK:
						komunikat.ustawPrzyciskTak("OK", null);
						break;
				}
				
				komunikat.show();
				
			}catch(BadTokenException e) {}
			
			// blad zostal pokazany wyczyszczenie komunikatu
			kodBledu = BRAK;
			bladTytul = null;
			bladTresc = null;
			ikona = -1;
			kolorTytul = -1;
			kolorTresc = -1;
		}
	}
}
