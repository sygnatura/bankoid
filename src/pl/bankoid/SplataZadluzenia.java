package pl.bankoid;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdView;

public class SplataZadluzenia extends Activity implements Runnable {

	private Thread watek;
	private AdView adView;
	
	// krok2
	private Dialog krok2;
	private String haslo_sms_label = null;
	private int radio_wybor;
	private boolean przeladuj = false;
	
	Pattern inputREGEX = Pattern.compile("id=\"([^\"]+)\" value=\"([^\"]+)\"");
	Pattern terminREGEX = Pattern.compile("<span id=\"dtMinAmtPaymentDate_label\">([^<]+)</span>");
	
	// REFERENCJE DO POL FORMULARZA I ZMIENNE
	TextView maIndebtednessAmt;
	TextView maIndebtednessAmt_Curr;
	TextView maImmediateAmtPayment;
	TextView maImmediateAmtPayment_Curr;
	TextView maAvaiableRelatedAccountFunds;
	TextView maAvaiableRelatedAccountFunds_Curr;
	TextView maMinAmtPayment;
	TextView maMinAmtPayment_Curr;
	TextView maDueTotalPayment;
	TextView maDueTotalPayment_Curr;
	TextView maTotalDebtPayment;
	TextView maTotalDebtPayment_Curr;
	EditText maAmountPayment;
	TextView maAmountPayment_Curr;
	TextView termin_splaty;
	String authTurnOff = "off";
	String RadioGroupPaymentAmount;
	String dtMinAmtPaymentDate_year = null;
	String dtMinAmtPaymentDate_month = null;
	String dtMinAmtPaymentDate_day = null;
	
	RadioButton radio_minimalna;
	RadioButton radio_pozostala;
	RadioButton radio_calosc;
	RadioButton radio_dowolna;
	
	// parametr dla przyciskow
	private String paramDalej;
	private String paramZatwierdz;
	private String paramModyfikuj;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.splata_zadluzenia);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");
    	
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    	
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();
        
        // INICJALIZACJA ZMIENNYCH
		maIndebtednessAmt = (TextView) findViewById(R.id.kwota_zadluzenia);
		maIndebtednessAmt_Curr = (TextView) findViewById(R.id.kwota_zadluzenia_waluta);
		maImmediateAmtPayment = (TextView) findViewById(R.id.kwota_natychmiastowa);
		maImmediateAmtPayment_Curr = (TextView) findViewById(R.id.kwota_natychmiastowa_waluta);
		maAvaiableRelatedAccountFunds = (TextView) findViewById(R.id.dostepne_srodki);
		maAvaiableRelatedAccountFunds_Curr = (TextView) findViewById(R.id.dostepne_srodki_waluta);
		maMinAmtPayment = (TextView) findViewById(R.id.kwota_minimalna);
		maMinAmtPayment_Curr = (TextView) findViewById(R.id.kwota_minimalna_waluta);
		maDueTotalPayment = (TextView) findViewById(R.id.kwota_pozostala);
		maDueTotalPayment_Curr = (TextView) findViewById(R.id.kwota_pozostala_waluta);
		maTotalDebtPayment = (TextView) findViewById(R.id.kwota_calosc);
		maTotalDebtPayment_Curr = (TextView) findViewById(R.id.kwota_calosc_waluta);
		maAmountPayment = (EditText) findViewById(R.id.kwota_dowolna);
		maAmountPayment_Curr = (TextView) findViewById(R.id.kwota_dowolna_waluta);
		termin_splaty = (TextView) findViewById(R.id.termin_splaty);
		maAmountPayment.setFilters(new InputFilter[]{new MoneyValueFilter(), new InputFilter.LengthFilter(15)});
		
		radio_minimalna = (RadioButton) findViewById(R.id.minimalna);
		radio_pozostala = (RadioButton) findViewById(R.id.pozostala);
		radio_calosc = (RadioButton) findViewById(R.id.calosc);
		radio_dowolna = (RadioButton) findViewById(R.id.dowolna);
		
        // reklama
       /* if(Bankoid.reklamy)
        {
        	adView = new AdView(this, AdSize.BANNER, Bankoid.ADMOB_ID);
            FrameLayout layout = (FrameLayout) this.findViewById(R.id.ad);
            layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            layout.addView(adView);
            layout.setVisibility(View.VISIBLE);
            adView.loadAd(Bankoid.adr);
            //adView.bringToFront();	
        }*/
        
        this.setTitle("Spłata zadłużenia, krok 1/2 ");
        
        // jezeli otwieranie formularza nie powiodlo sie
        if(tworzFormularzKrok1(dane) == false) Bankoid.bledy.pokazKomunikat(this);
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


    		case Przelewy.DALEJ_KROK1:
    			// wysylanie przelewu
    			String dane = wykonajPrzelewKrok1();
    			Message msg = handler.obtainMessage();
    			msg.what = Przelewy.DALEJ_KROK1;
				Bundle b = new Bundle();
				b.putString("dane", dane);
                msg.setData(b);
    			// jezeli wystapil blad tylko zle konto to przeladuj formularz
    			if(przeladuj)
    			{
    				przeladuj = false;
    				OperacjeKarta.wybierzSplataZadluzenia();
    			}
    			
    			handler.sendMessage(msg);
            	break;
            	
    		case Przelewy.MODYFIKUJ_KROK2:
    			// modyfikacja przelewu
    			modyfikujPrzelew();
    			handler.sendEmptyMessage(Przelewy.MODYFIKUJ_KROK2);
    			break;
    			
    		case Przelewy.ZATWIERDZ_KROK2:
    			wykonajPrzelewKrok2();
    			handler.sendEmptyMessage(Przelewy.ZATWIERDZ_KROK2);
    			break;

    	}
    	
    }
	
    private Handler handler = new Handler() {
    	public void handleMessage(Message msg) {

    		switch(msg.what)
    		{
    			 case Bankoid.ACTIVITY_WYLOGOWYWANIE:
    				 SplataZadluzenia.this.finish();
    				 break;
    				 
    			 case Przelewy.DALEJ_KROK1:
    				 // czy wystapil blad np nieprawidlowy nr telefonu
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(SplataZadluzenia.this);
    				 else
    				 {
       					 String dane = msg.getData().getString("dane");
       					 if(dane != null)
       					 {
           					 tworzFormularzKrok2(dane);
            				 krok2.show();
       					 }
    				 }
    				 break;

    			 case Przelewy.MODYFIKUJ_KROK2:
    				 // jezeli blad to pokaz inaczej zamknij okno dialogowe
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(SplataZadluzenia.this);
    				 else krok2.dismiss();
    				 break;
    				 
    			 case Przelewy.ZATWIERDZ_KROK2:
    				 // wyczyszczenie pola z haslem i ustawienie haslo label
    				 if(krok2.isShowing())
    				 {
    					 if(haslo_sms_label != null) ((TextView) krok2.findViewById(R.id.haslo_sms_label)).setText(haslo_sms_label);
    					 //((EditText) krok2.findViewById(R.id.haslo_sms)).setText("");
    				 }
    				 // pokaz blad zawierajacy czy przelew zostal zrealizowany
    				 Bankoid.bledy.pokazKomunikat(SplataZadluzenia.this);
    				 break;

    		}
    		
    		// zakmniecie dialogu
    		try
    		{
    			Bankoid.dialog.dismiss();
    		}catch(Exception e){}
    	}
    };
	
    public boolean tworzFormularzKrok1(String dane)
    {
    	Button przycisk_dalej = (Button) findViewById(R.id.przycisk_dalej);
    	Button przycisk_powrot = (Button) findViewById(R.id.przycisk_powrot);
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.karta_blad_splata_zadluzenia, Bledy.WYLOGUJ);
    		return false;
    	}
    	
    	// szukanie parametrow przycisku dalej
	    Matcher m = Przelewy.dalejREGEX.matcher(dane);
	    if(m.find()) paramDalej = m.group(1);
   	    
   	    // jezeli strona otworzyla sie prawidlowo
   	    if(dane.contains("Spłata zadłużenia") && paramDalej != null)
   	    {
   	    	// szukanie authTurnOff
   	    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
   	    	if(m.find()) authTurnOff = m.group(1);
   	    	
   	    	m = terminREGEX.matcher(dane);
   	    	if(m.find()) ((TextView) findViewById(R.id.termin_splaty)).setText(m.group(1));
   	    	
   	    	m = inputREGEX.matcher(dane);
   	    	while(m.find())
   	    	{
   	    		String id = m.group(1);
   	    		String wartosc = m.group(2);
   	    		
   	    		if(id.equals("maIndebtednessAmt")) maIndebtednessAmt.setText(wartosc);
   	    		else if(id.equals("maIndebtednessAmt_Curr")) maIndebtednessAmt_Curr.setText(wartosc);
   	    		else if(id.equals("maImmediateAmtPayment")) maImmediateAmtPayment.setText(wartosc);
   	    		else if(id.equals("maImmediateAmtPayment_Curr")) maImmediateAmtPayment_Curr.setText(wartosc);
   	    		else if(id.equals("maAvaiableRelatedAccountFunds")) maAvaiableRelatedAccountFunds.setText(wartosc);
   	    		else if(id.equals("maAvaiableRelatedAccountFunds_Curr")) maAvaiableRelatedAccountFunds_Curr.setText(wartosc);
   	    		else if(id.equals("maMinAmtPayment")) maMinAmtPayment.setText(wartosc);
   	    		else if(id.equals("maMinAmtPayment_Curr")) maMinAmtPayment_Curr.setText(wartosc);
   	    		else if(id.equals("maDueTotalPayment")) maDueTotalPayment.setText(wartosc);
   	    		else if(id.equals("maDueTotalPayment_Curr")) maDueTotalPayment_Curr.setText(wartosc);
   	    		else if(id.equals("maTotalDebtPayment")) maTotalDebtPayment.setText(wartosc);
   	    		else if(id.equals("maTotalDebtPayment_Curr")) maTotalDebtPayment_Curr.setText(wartosc);
   	    		else if(id.equals("maAmountPayment_Curr")) maAmountPayment_Curr.setText(wartosc);
   	    		else if(id.equals("dtMinAmtPaymentDate_year")) dtMinAmtPaymentDate_year = wartosc;
  	    		else if(id.equals("dtMinAmtPaymentDate_month")) dtMinAmtPaymentDate_month = wartosc;
  	    		else if(id.equals("dtMinAmtPaymentDate_day")) dtMinAmtPaymentDate_day = wartosc;
   	    	}
   	    	
   	    	// ustawienie sluchaczy na przyciski
  	    	przycisk_dalej.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View arg0) {
   					// jezeli dane zostaly wprowadzone poprawnie przejdz do krok2
   					if(sprawdzPoprawnoscDanych())
   					{
   						Bankoid.tworzProgressDialog(SplataZadluzenia.this, getResources().getString(R.string.dialog_pobinfo));
   						Bankoid.dialog.show();
   						
   						watek = new Thread(SplataZadluzenia.this, String.valueOf(Przelewy.DALEJ_KROK1));
   						watek.start();
   					}
   				}
   	    	});
  	    	
  	    	// zaznaczenie domyslnej opcji radio na dowolne
  	    	zaznaczRadio(R.id.dowolna);
  	    	
  	    	// ustawienie sluchaczy na przyciski
  	    	przycisk_powrot.setOnClickListener(new OnClickListener()
   	    	{
   				@Override
   				public void onClick(View arg0) {
   					SplataZadluzenia.this.finish();
   				}
   	    	});
  	    	
  	    	maAmountPayment.setOnClickListener(new OnClickListener()
  	    	{
				@Override
				public void onClick(View v) {
					zaznaczRadio(R.id.dowolna);
				}    		
  	    	});
  	    	
  	    	radio_minimalna.setOnClickListener(new OnClickListener()
  	    	{
				@Override
				public void onClick(View v) {
					zaznaczRadio(R.id.minimalna);
				}
  	    	});
  	    	
  	    	radio_pozostala.setOnClickListener(new OnClickListener()
  	    	{
				@Override
				public void onClick(View v) {
					zaznaczRadio(R.id.pozostala);
				}
  	    	});
  	    	
  	    	radio_calosc.setOnClickListener(new OnClickListener()
  	    	{
				@Override
				public void onClick(View v) {
					zaznaczRadio(R.id.calosc);
				}
  	    	});
  	    	
  	    	radio_dowolna.setOnClickListener(new OnClickListener()
  	    	{
				@Override
				public void onClick(View v) {
					zaznaczRadio(R.id.dowolna);
				}
  	    	});
  	    	
   	    	return true;
   	    }
   	    // czy wystapil standartowy blad
   	    if(Bankoid.bledy.czyBlad())
   	    {
   	    	Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
   	    }
   	    else
   	    {
   	    	// czy jest komunikat z bledem
   	    	m = Przelewy.bladPrzelewREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
		    	String bladTresc = Html.fromHtml(m.group(2)).toString();
		    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.ZAMKNIJ_OKNO);
		    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
   	    	}
	   	    // niestandartowy blad
	   	    else Bankoid.bledy.ustawBlad(R.string.karta_blad_splata_zadluzenia, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
    
    // zwraca false jezeli nalezy przeladowac
	private String wykonajPrzelewKrok1()
	{	
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+Karty.wybrana_karta.pobierzRozszerzenie()+"_manual_payment.aspx");
	    request.setMethod("POST");
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", this.paramDalej);
	    request.addParam("__CurrentWizardStep", "1");
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("maIndebtednessAmt", maIndebtednessAmt.getText().toString());
	    request.addParam("maIndebtednessAmt_Curr", maIndebtednessAmt_Curr.getText().toString());
	    if(dtMinAmtPaymentDate_year != null) request.addParam("dtMinAmtPaymentDate_year", dtMinAmtPaymentDate_year);
	    if(dtMinAmtPaymentDate_month != null) request.addParam("dtMinAmtPaymentDate_month", dtMinAmtPaymentDate_month);
	    if(dtMinAmtPaymentDate_day != null) request.addParam("dtMinAmtPaymentDate_day", dtMinAmtPaymentDate_day);
	    request.addParam("maImmediateAmtPayment", maImmediateAmtPayment.getText().toString().replace(".", ","));
	    request.addParam("maImmediateAmtPayment_Curr", maImmediateAmtPayment_Curr.getText().toString());
	    request.addParam("maAvaiableRelatedAccountFunds", maAvaiableRelatedAccountFunds.getText().toString());
	    request.addParam("maAvaiableRelatedAccountFunds_Curr", maAvaiableRelatedAccountFunds_Curr.getText().toString());
	    request.addParam("maMinAmtPayment", maMinAmtPayment.getText().toString());
	    request.addParam("maMinAmtPayment_Curr", maMinAmtPayment_Curr.getText().toString());
	    request.addParam("maDueTotalPayment", maDueTotalPayment.getText().toString());
	    request.addParam("maDueTotalPayment_Curr", maDueTotalPayment_Curr.getText().toString());
	    request.addParam("maTotalDebtPayment", maTotalDebtPayment.getText().toString());
	    request.addParam("maTotalDebtPayment_Curr", maTotalDebtPayment_Curr.getText().toString());
	    request.addParam("maAmountPayment_Curr", maAmountPayment_Curr.getText().toString());
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("RadioGroupPaymentAmount", RadioGroupPaymentAmount);
	    if(RadioGroupPaymentAmount.equals("rbPaymentOfAmount"))
		    request.addParam("maAmountPayment", maAmountPayment.getText().toString().replace(".", ","));
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
	    // udalo sie przejsc do kroku 2 zwroc true
	    Matcher m = Przelewy.zatwierdzREGEX.matcher(rezultat);
	    if(m.find())
	    {
	    	paramZatwierdz = m.group(1);
	    	//tworzFormularzKrok2(rezultat);
	    	return rezultat;
	    }
	    // szukanie bledu
	    else if(Bankoid.bledy.czyBlad() == false)
	    {		    	
		    m = Przelewy.bladPrzelewREGEX.matcher(rezultat);
		    if(m.find())
		    {
		    	String bladTresc = Html.fromHtml(m.group(2).replace("&shy;<wbr />", "")).toString();
		    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.INFO);
		    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
		    	// nalezy zaladowac na nowo formularz
		    	przeladuj = true;
		    }
		    // niestandartowy blad
		    else
		    {
		    	Bankoid.bledy.ustawBlad(R.string.karta_blad_splata_zadluzenia, Bledy.WYLOGUJ);
		    }
	    }
	    return null;
	}

	// tworzy i pokazuje formularz z kodem sms
	private void tworzFormularzKrok2(String dane)
	{
		// tworzenie dialogu z formularzem dla kroku 2
    	krok2 = new Dialog(this, R.style.CustomTheme);
		krok2.setContentView(R.layout.dialog_krok2_splata_zadluzenia);
		krok2.setTitle("Spłata zadłużenia, krok 2/2");

        krok2.getWindow().setFormat(PixelFormat.RGBA_8888);
        krok2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
		//////////////////////////////////////////////
	
		TextView kwota_zadluzenia = (TextView) krok2.findViewById(R.id.kwota_zadluzenia);
		TextView termin_splaty = (TextView) krok2.findViewById(R.id.termin_splaty);
		TextView kwota_natychmiastowa = (TextView) krok2.findViewById(R.id.kwota_natychmiastowa);
		TextView dostepne_srodki = (TextView) krok2.findViewById(R.id.dostepne_srodki);
		TextView kwota_splaty = (TextView) krok2.findViewById(R.id.kwota_splaty);

		Button przycisk_powrot = (Button) krok2.findViewById(R.id.przycisk_powrot);
		Button przycisk_modyfikuj = (Button) krok2.findViewById(R.id.przycisk_modyfikuj);
		Button przycisk_zatwierdz = (Button) krok2.findViewById(R.id.przycisk_zatwierdz);
		//////////////////////////////////////////////
		
		// szukanie parameters modyfikuj
		Matcher m = Przelewy.modyfikujREGEX.matcher(dane);
		if(m.find()) paramModyfikuj = m.group(1);

    	// szukanie authTurnOff
    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
    	if(m.find()) authTurnOff = m.group(1);
		
    	m = inputREGEX.matcher(dane);
    	while(m.find())
    	{
    		String id = m.group(1);
    		String wartosc = m.group(2);
    		
    		if(id.equals("maIndebtednessAmt")) maIndebtednessAmt.setText(wartosc);
    		else if(id.equals("maIndebtednessAmt_Curr")) maIndebtednessAmt_Curr.setText(wartosc);
    		else if(id.equals("maImmediateAmtPayment")) maImmediateAmtPayment.setText(wartosc);
    		else if(id.equals("maImmediateAmtPayment_Curr")) maImmediateAmtPayment_Curr.setText(wartosc);
    		else if(id.equals("maAvaiableRelatedAccountFunds")) maAvaiableRelatedAccountFunds.setText(wartosc);
    		else if(id.equals("maAvaiableRelatedAccountFunds_Curr")) maAvaiableRelatedAccountFunds_Curr.setText(wartosc);
    		else if(id.equals("maMinAmtPayment")) maMinAmtPayment.setText(wartosc);
    		else if(id.equals("maMinAmtPayment_Curr")) maMinAmtPayment_Curr.setText(wartosc);
    		else if(id.equals("maDueTotalPayment")) maDueTotalPayment.setText(wartosc);
    		else if(id.equals("maDueTotalPayment_Curr")) maDueTotalPayment_Curr.setText(wartosc);
    		else if(id.equals("maTotalDebtPayment")) maTotalDebtPayment.setText(wartosc);
    		else if(id.equals("maTotalDebtPayment_Curr")) maTotalDebtPayment_Curr.setText(wartosc);
    		else if(id.equals("maAmountPayment_Curr")) maAmountPayment_Curr.setText(wartosc);
    		else if(id.equals("dtMinAmtPaymentDate_year")) dtMinAmtPaymentDate_year = wartosc;
    		else if(id.equals("dtMinAmtPaymentDate_month")) dtMinAmtPaymentDate_month = wartosc;
    		else if(id.equals("dtMinAmtPaymentDate_day")) dtMinAmtPaymentDate_day = wartosc;
    	}
    	
    	kwota_zadluzenia.setText(maIndebtednessAmt.getText().toString() + " " + maIndebtednessAmt_Curr.getText().toString());
    	termin_splaty.setText(this.termin_splaty.getText());
    	kwota_natychmiastowa.setText(maImmediateAmtPayment.getText().toString() + " " + maImmediateAmtPayment_Curr.getText().toString());
    	dostepne_srodki.setText(maAvaiableRelatedAccountFunds.getText().toString() + " " + maAvaiableRelatedAccountFunds_Curr.getText().toString());
    	
    	switch(radio_wybor)
    	{
    		case R.id.minimalna:
    			kwota_splaty.setText(maMinAmtPayment.getText().toString() + " " + maMinAmtPayment_Curr.getText().toString());
    			break;
  
    		case R.id.pozostala:
    			kwota_splaty.setText(maDueTotalPayment.getText().toString() + " " + maDueTotalPayment_Curr.getText().toString());
    			break;
    			
    		case R.id.calosc:
    			kwota_splaty.setText(maTotalDebtPayment.getText().toString() + " " + maTotalDebtPayment_Curr.getText().toString());
    			break;
    			
    		case R.id.dowolna:
    			kwota_splaty.setText(maAmountPayment.getText().toString().replace(".", ",") + " " + maAmountPayment_Curr.getText().toString());
    			break;
    	}
    	
		// w przypadku anulowania powrot do OperacjeRachunek
		krok2.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface krok2) {
				krok2.dismiss();
				SplataZadluzenia.this.finish();
			}
			
		});
		
		// akcja dla przycisku powrot
		przycisk_powrot.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				krok2.cancel();
			}
		});
	
		// akcja dla przycisku modyfikuj
		przycisk_modyfikuj.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				Bankoid.tworzProgressDialog(SplataZadluzenia.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(SplataZadluzenia.this, String.valueOf(Przelewy.MODYFIKUJ_KROK2));
				watek.start();
			}
		});

		// akcja dla przycisku zatwierdz
		przycisk_zatwierdz.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {			
				Bankoid.tworzProgressDialog(SplataZadluzenia.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(SplataZadluzenia.this, String.valueOf(Przelewy.ZATWIERDZ_KROK2));
				watek.start();
			}
		});

	}
	
	// metoda wywolywana po kliknieciu modyfikuj
    private boolean modyfikujPrzelew()
    {
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+Karty.wybrana_karta.pobierzRozszerzenie()+"_manual_payment.aspx");
	    request.setMethod("POST");
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", this.paramModyfikuj);
	    request.addParam("__CurrentWizardStep", "2");
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("maIndebtednessAmt", maIndebtednessAmt.getText().toString());
	    request.addParam("maIndebtednessAmt_Curr", maIndebtednessAmt_Curr.getText().toString());
	    if(dtMinAmtPaymentDate_year != null) request.addParam("dtMinAmtPaymentDate_year", dtMinAmtPaymentDate_year);
	    if(dtMinAmtPaymentDate_month != null) request.addParam("dtMinAmtPaymentDate_month", dtMinAmtPaymentDate_month);
	    if(dtMinAmtPaymentDate_day != null) request.addParam("dtMinAmtPaymentDate_day", dtMinAmtPaymentDate_day);
	    request.addParam("maImmediateAmtPayment", maImmediateAmtPayment.getText().toString().replace(".", ","));
	    request.addParam("maImmediateAmtPayment_Curr", maImmediateAmtPayment_Curr.getText().toString());
	    request.addParam("maAvaiableRelatedAccountFunds", maAvaiableRelatedAccountFunds.getText().toString());
	    request.addParam("maAvaiableRelatedAccountFunds_Curr", maAvaiableRelatedAccountFunds_Curr.getText().toString());
	    request.addParam("maMinAmtPayment", maMinAmtPayment.getText().toString());
	    request.addParam("maMinAmtPayment_Curr", maMinAmtPayment_Curr.getText().toString());
	    request.addParam("maDueTotalPayment", maDueTotalPayment.getText().toString());
	    request.addParam("maDueTotalPayment_Curr", maDueTotalPayment_Curr.getText().toString());
	    request.addParam("maTotalDebtPayment", maTotalDebtPayment.getText().toString());
	    request.addParam("maTotalDebtPayment_Curr", maTotalDebtPayment_Curr.getText().toString());
	    request.addParam("maAmountPayment_Curr", maAmountPayment_Curr.getText().toString());
	    request.addParam("RadioGroupPaymentAmount", RadioGroupPaymentAmount);
	    if(RadioGroupPaymentAmount.equals("rbPaymentOfAmount"))
		    request.addParam("maAmountPayment", maAmountPayment.getText().toString().replace(".", ","));
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
    	// szukanie parametrow przycisku dalej
	    Matcher m = Przelewy.dalejREGEX.matcher(rezultat);
   	    // jezeli strona otworzyla sie prawidlowo
	    if(m.find()) 
   	    {
   	    	paramDalej = m.group(1);
   	    	return true;
   	    }
   	    // nieokreslony blad
   	    else if(Bankoid.bledy.czyBlad() == false)
   	    {
   	    	Bankoid.bledy.ustawBlad(R.string.karta_blad_splata_zadluzenia, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
	// metoda wywolywana po kliknieciu zatwierdz
    private void wykonajPrzelewKrok2()
    {
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+Karty.wybrana_karta.pobierzRozszerzenie()+"_manual_payment.aspx");
	    request.setMethod("POST");
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", this.paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "2");
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("maIndebtednessAmt", maIndebtednessAmt.getText().toString());
	    request.addParam("maIndebtednessAmt_Curr", maIndebtednessAmt_Curr.getText().toString());
	    if(dtMinAmtPaymentDate_year != null) request.addParam("dtMinAmtPaymentDate_year", dtMinAmtPaymentDate_year);
	    if(dtMinAmtPaymentDate_month != null) request.addParam("dtMinAmtPaymentDate_month", dtMinAmtPaymentDate_month);
	    if(dtMinAmtPaymentDate_day != null) request.addParam("dtMinAmtPaymentDate_day", dtMinAmtPaymentDate_day);
	    request.addParam("maImmediateAmtPayment", maImmediateAmtPayment.getText().toString().replace(".", ","));
	    request.addParam("maImmediateAmtPayment_Curr", maImmediateAmtPayment_Curr.getText().toString());
	    request.addParam("maAvaiableRelatedAccountFunds", maAvaiableRelatedAccountFunds.getText().toString());
	    request.addParam("maAvaiableRelatedAccountFunds_Curr", maAvaiableRelatedAccountFunds_Curr.getText().toString());
	    request.addParam("maMinAmtPayment", maMinAmtPayment.getText().toString());
	    request.addParam("maMinAmtPayment_Curr", maMinAmtPayment_Curr.getText().toString());
	    request.addParam("maDueTotalPayment", maDueTotalPayment.getText().toString());
	    request.addParam("maDueTotalPayment_Curr", maDueTotalPayment_Curr.getText().toString());
	    request.addParam("maTotalDebtPayment", maTotalDebtPayment.getText().toString());
	    request.addParam("maTotalDebtPayment_Curr", maTotalDebtPayment_Curr.getText().toString());
	    request.addParam("maAmountPayment_Curr", maAmountPayment_Curr.getText().toString());
	    request.addParam("RadioGroupPaymentAmount", RadioGroupPaymentAmount);
	    if(RadioGroupPaymentAmount.equals("rbPaymentOfAmount"))
		    request.addParam("maAmountPayment", maAmountPayment.getText().toString().replace(".", ","));
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);

	    // jezeli nie ma standartowego bledu
   	    if(Bankoid.bledy.czyBlad() == false)
   	    {
	    	// szukanie komunikatu potwierdzajacego przelew
   	    	Matcher m = Przelewy.komunikatREGEX.matcher(rezultat);
   	    	if(m.find())
   	    	{
   	    		OperacjeKarta.odswiez = true;
   	    		Bankoid.bledy.ustawBlad(m.group(1).trim(), m.group(2).trim(), Bledy.ZAMKNIJ_OKNO);
   	    		Bankoid.bledy.ustawKolorTresc(this.getResources().getColor(R.color.ok));
   	    		Bankoid.bledy.ustawIkone(android.R.drawable.ic_dialog_info);
   	    	}
   	    	else
   	    	{
	   	    	// szukanie komunikatu o bledzie (nieprawidlowe haslo)
	   	    	m = Przelewy.bladPrzelewREGEX.matcher(rezultat);
	   	    	if(m.find())
	   	    	{
	   	    		String bladTresc = Html.fromHtml(m.group(2)).toString();
	   	    		Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.POWROT);
	   	    		Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	   	    		Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
	   	    	}
	   	    	else
	   	    	{
	   	    		// niestandartowy blad
	   	    		Bankoid.bledy.ustawBlad(R.string.karta_blad_splata_zadluzenia, Bledy.WYLOGUJ);
	   	    	}
   	    	}
   	    }
    }
    
    private boolean sprawdzPoprawnoscDanych()
	{
    	BigDecimal kwota = null;
    	
    	switch(radio_wybor)
    	{
    		case R.id.minimalna:
    	    	try
    	    	{
    	    		kwota = new BigDecimal(maMinAmtPayment.getText().toString().replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    			break;
    			
    		case R.id.pozostala:
    	    	try
    	    	{
    	    		kwota = new BigDecimal(maDueTotalPayment.getText().toString().replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    			break;
    			
    		case R.id.calosc:
    	    	try
    	    	{
    	    		kwota = new BigDecimal(maTotalDebtPayment.getText().toString().replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    			break;
    			
    		case R.id.dowolna:
    	    	try
    	    	{
    	    		kwota = new BigDecimal(maAmountPayment.getText().toString());	
    	    	}catch(NumberFormatException e) {}
    			break;
    	}
    	
		// sprawdzenie kwtoy
		if(kwota == null || kwota.compareTo(new BigDecimal(0)) <= 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.karty_blad_kwota2, Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
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
    
    public void zaznaczRadio(int id)
    {	
    	radio_wybor = id;
    	switch(id)
    	{
		case R.id.minimalna:
			RadioGroupPaymentAmount = "rbPaymentOfMinAmount";
			radio_minimalna.setChecked(true);
			radio_pozostala.setChecked(false);
			radio_calosc.setChecked(false);
			radio_dowolna.setChecked(false);
			break;
			
		case R.id.pozostala:
			RadioGroupPaymentAmount = "rbPaymentOfLastBankStatementAmount";
			radio_minimalna.setChecked(false);
			radio_pozostala.setChecked(true);
			radio_calosc.setChecked(false);
			radio_dowolna.setChecked(false);
			break;
			
		case R.id.calosc:
			RadioGroupPaymentAmount = "rbPaymentOfWholeAmount";
			radio_minimalna.setChecked(false);
			radio_pozostala.setChecked(false);
			radio_calosc.setChecked(true);
			radio_dowolna.setChecked(false);
			break;
			
		case R.id.dowolna:
			RadioGroupPaymentAmount = "rbPaymentOfAmount";
			radio_minimalna.setChecked(false);
			radio_pozostala.setChecked(false);
			radio_calosc.setChecked(false);
			radio_dowolna.setChecked(true);
			break;
    		
    	}
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
						
	                	Bankoid.tworzProgressDialog(SplataZadluzenia.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(SplataZadluzenia.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
