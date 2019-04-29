package pl.bankoid;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
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
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ZmianaLimitowKwotowych extends Activity implements Runnable {

	private Thread watek;
	// krok2
	private Dialog krok2;
	private String haslo_sms_label = null;
	private InputFilter[] filtr = new InputFilter[]{new MoneyValueFilter(),  new InputFilter.LengthFilter(15)}; 
	
	// REFERENCJE DO POL FORMULARZA I ZMIENNE
	private EditText MAmount_0_1_ID_Day_Amount_0000;
	private EditText MAmount_0_3_ID_Month_Amount_0000;
	private EditText MAmount_1_1_ID_Day_Amount_0003;
	private EditText MAmount_1_3_ID_Month_Amount_0003;
	private EditText MAmount_2_1_ID_Day_Amount_0004;
	private EditText MAmount_2_3_ID_Month_Amount_0004;
	private EditText MAmount_3_1_ID_Day_Amount_0100;
	private EditText MAmount_3_3_ID_Month_Amount_0100;
	private EditText MAmount_4_1_ID_Day_Amount_0200;
	private EditText MAmount_4_3_ID_Month_Amount_0200;
	
	private TextView MAmount_0_1_ID_Day_Amount_0000_Curr;
	private TextView MAmount_0_3_ID_Month_Amount_0000_Curr;
	private TextView MAmount_1_1_ID_Day_Amount_0003_Curr;
	private TextView MAmount_1_3_ID_Month_Amount_0003_Curr;
	private TextView MAmount_2_1_ID_Day_Amount_0004_Curr;
	private TextView MAmount_2_3_ID_Month_Amount_0004_Curr;
	private TextView MAmount_3_1_ID_Day_Amount_0100_Curr;
	private TextView MAmount_3_3_ID_Month_Amount_0100_Curr;
	private TextView MAmount_4_1_ID_Day_Amount_0200_Curr;
	private TextView MAmount_4_3_ID_Month_Amount_0200_Curr;
	private String authTurnOff;
	private String TransactionType;
	// limity
	private BigDecimal limit_MAmount_0_1 = null;
	private BigDecimal limit_MAmount_0_3 = null;
	private BigDecimal limit_MAmount_1_1 = null;
	private BigDecimal limit_MAmount_1_3 = null;
	private BigDecimal limit_MAmount_2_1 = null;
	private BigDecimal limit_MAmount_2_3 = null;
	private BigDecimal limit_MAmount_3_1 = null;
	private BigDecimal limit_MAmount_3_3 = null;
	private BigDecimal limit_MAmount_4_1 = null;
	private BigDecimal limit_MAmount_4_3 = null;
	
	// parametr dla przyciskow
	private String paramDalej;
	private String paramZatwierdz;
	private String paramModyfikuj;
	private String paramPotwierdzPozniej;
	private String paramPowrot;
	

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.limit_kwotowy);
    	Bundle extras = getIntent().getExtras();
    	String dane = extras.getString("dane");

        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        
        // automatycznie zamknij okno jezeli nie jest sie zalogowanym
        if(Bankoid.zalogowano == false) this.finish();

        this.setTitle("Zmiana limitów - kwotowych");
        
        // inicjalizacja zmiennych
        MAmount_0_1_ID_Day_Amount_0000 = (EditText) this.findViewById(R.id.MAmount_0_1_ID_Day_Amount_0000);
        MAmount_0_1_ID_Day_Amount_0000.setFilters(filtr);
        MAmount_0_3_ID_Month_Amount_0000 = (EditText) this.findViewById(R.id.MAmount_0_3_ID_Month_Amount_0000);
        MAmount_0_3_ID_Month_Amount_0000.setFilters(filtr);
        MAmount_1_1_ID_Day_Amount_0003 = (EditText) this.findViewById(R.id.MAmount_1_1_ID_Day_Amount_0003);
        MAmount_1_1_ID_Day_Amount_0003.setFilters(filtr);
        MAmount_1_3_ID_Month_Amount_0003 = (EditText) this.findViewById(R.id.MAmount_1_3_ID_Month_Amount_0003);
        MAmount_1_3_ID_Month_Amount_0003.setFilters(filtr);
        MAmount_2_1_ID_Day_Amount_0004 = (EditText) this.findViewById(R.id.MAmount_2_1_ID_Day_Amount_0004);
        MAmount_2_1_ID_Day_Amount_0004.setFilters(filtr);
        MAmount_2_3_ID_Month_Amount_0004 = (EditText) this.findViewById(R.id.MAmount_2_3_ID_Month_Amount_0004);
        MAmount_2_3_ID_Month_Amount_0004.setFilters(filtr);
        MAmount_3_1_ID_Day_Amount_0100 = (EditText) this.findViewById(R.id.MAmount_3_1_ID_Day_Amount_0100);
        MAmount_3_1_ID_Day_Amount_0100.setFilters(filtr);
        MAmount_3_3_ID_Month_Amount_0100 = (EditText) this.findViewById(R.id.MAmount_3_3_ID_Month_Amount_0100);
        MAmount_3_3_ID_Month_Amount_0100.setFilters(filtr);
        MAmount_4_1_ID_Day_Amount_0200 = (EditText) this.findViewById(R.id.MAmount_4_1_ID_Day_Amount_0200);
        MAmount_4_1_ID_Day_Amount_0200.setFilters(filtr);
        MAmount_4_3_ID_Month_Amount_0200 = (EditText) this.findViewById(R.id.MAmount_4_3_ID_Month_Amount_0200);
        MAmount_4_3_ID_Month_Amount_0200.setFilters(filtr);
        // waluty
        MAmount_0_1_ID_Day_Amount_0000_Curr = (TextView) this.findViewById(R.id.MAmount_0_1_ID_Day_Amount_0000_Curr);
    	MAmount_0_3_ID_Month_Amount_0000_Curr = (TextView) this.findViewById(R.id.MAmount_0_3_ID_Month_Amount_0000_Curr);
		MAmount_1_1_ID_Day_Amount_0003_Curr = (TextView) this.findViewById(R.id.MAmount_1_1_ID_Day_Amount_0003_Curr);
		MAmount_1_3_ID_Month_Amount_0003_Curr = (TextView) this.findViewById(R.id.MAmount_1_3_ID_Month_Amount_0003_Curr);
		MAmount_2_1_ID_Day_Amount_0004_Curr = (TextView) this.findViewById(R.id.MAmount_2_1_ID_Day_Amount_0004_Curr);
		MAmount_2_3_ID_Month_Amount_0004_Curr = (TextView) this.findViewById(R.id.MAmount_2_3_ID_Month_Amount_0004_Curr);
		MAmount_3_1_ID_Day_Amount_0100_Curr = (TextView) this.findViewById(R.id.MAmount_3_1_ID_Day_Amount_0100_Curr);
		MAmount_3_3_ID_Month_Amount_0100_Curr = (TextView) this.findViewById(R.id.MAmount_3_3_ID_Month_Amount_0100_Curr);
		MAmount_4_1_ID_Day_Amount_0200_Curr = (TextView) this.findViewById(R.id.MAmount_4_1_ID_Day_Amount_0200_Curr);
		MAmount_4_3_ID_Month_Amount_0200_Curr = (TextView) this.findViewById(R.id.MAmount_4_3_ID_Month_Amount_0200_Curr);


        // jezeli otwieranie formularza z przelewem nie powiodlo sie
        if(dane == null || tworzFormularzKrok1(dane) == false) Bankoid.bledy.pokazKomunikat(this);
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
                handler.sendMessage(msg);
            	break;
            			
    		case Przelewy.MODYFIKUJ_KROK2:
    			// modyfikacja przelewu
    			modyfikujPrzelew();
    			handler.sendEmptyMessage(Przelewy.MODYFIKUJ_KROK2);
    			break;
    			
    		case Przelewy.ZATWIERDZ_POZNIEJ_KROK2:
    			// potwierdz pozniej przelew
    			potwierdzPozniejPrzelew();
    			handler.sendEmptyMessage(Przelewy.ZATWIERDZ_POZNIEJ_KROK2);
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
					 ZmianaLimitowKwotowych.this.finish();
					 break;
    		
    			 case Przelewy.DALEJ_KROK1:
    				 // czy wystapil blad np nieprawidlowy nr konta
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ZmianaLimitowKwotowych.this);
    				// otworz formularz z haslem potwierdzajacym przelew
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
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ZmianaLimitowKwotowych.this);
    				 else krok2.dismiss();
    				 break;
    				 
    			 case Przelewy.ZATWIERDZ_POZNIEJ_KROK2:
    				 // jezeli blad to pokaz inaczej przejdz do szczegoly rachunku
    				 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ZmianaLimitowKwotowych.this);
    				 else
    				 {
    					 krok2.dismiss();
    					 ZmianaLimitowKwotowych.this.finish();
    				 }
    				 break;
    				
    			 case Przelewy.ZATWIERDZ_KROK2:
    				 // wyczyszczenie pola z haslem i ustawienie haslo label
    				 if(krok2.isShowing())
    				 {
    					 if(haslo_sms_label != null) ((TextView) krok2.findViewById(R.id.haslo_sms_label)).setText(haslo_sms_label);
    					 //((EditText) krok2.findViewById(R.id.haslo_sms)).setText("");
    				 }
    				 
    				 // pokaz blad zawierajacy czy przelew zostal zrealizowany
    				 Bankoid.bledy.pokazKomunikat(ZmianaLimitowKwotowych.this);
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
    	Button przycisk_powrot = (Button) this.findViewById(R.id.przycisk_powrot);
    	Button przycisk_dalej = (Button) this.findViewById(R.id.przycisk_dalej);
    	
    	// REGEX
    	Pattern MAmount_0_1_ID_Day_Amount_0000_REGEX = Pattern.compile("name=\"[^\"]+MAmount_0_1_ID_Day_Amount_0000\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_0_3_ID_Month_Amount_0000_REGEX = Pattern.compile("name=\"[^\"]+MAmount_0_3_ID_Month_Amount_0000\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_1_1_ID_Day_Amount_0003_REGEX = Pattern.compile("name=\"[^\"]+MAmount_1_1_ID_Day_Amount_0003\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_1_3_ID_Month_Amount_0003_REGEX = Pattern.compile("name=\"[^\"]+MAmount_1_3_ID_Month_Amount_0003\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_2_1_ID_Day_Amount_0004_REGEX = Pattern.compile("name=\"[^\"]+MAmount_2_1_ID_Day_Amount_0004\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_2_3_ID_Month_Amount_0004_REGEX = Pattern.compile("name=\"[^\"]+MAmount_2_3_ID_Month_Amount_0004\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_3_1_ID_Day_Amount_0100_REGEX = Pattern.compile("name=\"[^\"]+MAmount_3_1_ID_Day_Amount_0100\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_3_3_ID_Month_Amount_0100_REGEX = Pattern.compile("name=\"[^\"]+MAmount_3_3_ID_Month_Amount_0100\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_4_1_ID_Day_Amount_0200_REGEX = Pattern.compile("name=\"[^\"]+MAmount_4_1_ID_Day_Amount_0200\".+?value=\"([^\"]+)\"");
    	Pattern MAmount_4_3_ID_Month_Amount_0200_REGEX = Pattern.compile("name=\"[^\"]+MAmount_4_3_ID_Month_Amount_0200\".+?value=\"([^\"]+)\"");

    	// limity
    	Pattern limit_MAmount_0_1_REGEX = Pattern.compile("MAmount_0_1_ID_Day_Amount_0000, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_0_3_REGEX = Pattern.compile("MAmount_0_3_ID_Month_Amount_0000, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_1_1_REGEX = Pattern.compile("MAmount_1_1_ID_Day_Amount_0003, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_1_3_REGEX = Pattern.compile("MAmount_1_3_ID_Month_Amount_0003, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_2_1_REGEX = Pattern.compile("MAmount_2_1_ID_Day_Amount_0004, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_2_3_REGEX = Pattern.compile("MAmount_2_3_ID_Month_Amount_0004, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_3_1_REGEX = Pattern.compile("MAmount_3_1_ID_Day_Amount_0100, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_3_3_REGEX = Pattern.compile("MAmount_3_3_ID_Month_Amount_0100, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_4_1_REGEX = Pattern.compile("MAmount_4_1_ID_Day_Amount_0200, '[^']+', '(\\d{1,9},\\d{2})',");
    	Pattern limit_MAmount_4_3_REGEX = Pattern.compile("MAmount_4_3_ID_Month_Amount_0200, '[^']+', '(\\d{1,9},\\d{2})',");

    	// waluty
    	Pattern MAmount_0_1_ID_Day_Amount_0000_Curr_REGEX = Pattern.compile("id=\"MAmount_0_1_ID_Day_Amount_0000_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_0_3_ID_Month_Amount_0000_Curr_REGEX = Pattern.compile("id=\"MAmount_0_3_ID_Month_Amount_0000_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_1_1_ID_Day_Amount_0003_Curr_REGEX = Pattern.compile("id=\"MAmount_1_1_ID_Day_Amount_0003_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_1_3_ID_Month_Amount_0003_Curr_REGEX = Pattern.compile("id=\"MAmount_1_3_ID_Month_Amount_0003_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_2_1_ID_Day_Amount_0004_Curr_REGEX = Pattern.compile("id=\"MAmount_2_1_ID_Day_Amount_0004_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_2_3_ID_Month_Amount_0004_Curr_REGEX = Pattern.compile("id=\"MAmount_2_3_ID_Month_Amount_0004_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_3_1_ID_Day_Amount_0100_Curr_REGEX = Pattern.compile("id=\"MAmount_3_1_ID_Day_Amount_0100_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_3_3_ID_Month_Amount_0100_Curr_REGEX = Pattern.compile("id=\"MAmount_3_3_ID_Month_Amount_0100_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_4_1_ID_Day_Amount_0200_Curr_REGEX = Pattern.compile("id=\"MAmount_4_1_ID_Day_Amount_0200_Curr\" value=\"([^\"]+)\"");
    	Pattern MAmount_4_3_ID_Month_Amount_0200_Curr_REGEX = Pattern.compile("id=\"MAmount_4_3_ID_Month_Amount_0200_Curr\" value=\"([^\"]+)\"");
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
    		return false;
    	}
    	
    	// szukanie parametru dal przycisku dalej
    	Matcher m = Przelewy.dalejREGEX.matcher(dane);
    	if(m.find()) 
    	{
    		paramDalej = m.group(1);
    	    
    	    m = MAmount_0_1_ID_Day_Amount_0000_REGEX.matcher(dane);
    	    if(m.find()) MAmount_0_1_ID_Day_Amount_0000.setText(m.group(1).replace(",", "."));

    	    m = MAmount_0_3_ID_Month_Amount_0000_REGEX.matcher(dane);
    	    if(m.find()) MAmount_0_3_ID_Month_Amount_0000.setText(m.group(1).replace(",", "."));

    	    m = MAmount_1_1_ID_Day_Amount_0003_REGEX.matcher(dane);
    	    if(m.find()) MAmount_1_1_ID_Day_Amount_0003.setText(m.group(1).replace(",", "."));

    	    m = MAmount_1_3_ID_Month_Amount_0003_REGEX.matcher(dane);
    	    if(m.find()) MAmount_1_3_ID_Month_Amount_0003.setText(m.group(1).replace(",", "."));

    	    m = MAmount_2_1_ID_Day_Amount_0004_REGEX.matcher(dane);
    	    if(m.find()) MAmount_2_1_ID_Day_Amount_0004.setText(m.group(1).replace(",", "."));

    	    m = MAmount_2_3_ID_Month_Amount_0004_REGEX.matcher(dane);
    	    if(m.find()) MAmount_2_3_ID_Month_Amount_0004.setText(m.group(1).replace(",", "."));

    	    m = MAmount_3_1_ID_Day_Amount_0100_REGEX.matcher(dane);
    	    if(m.find()) MAmount_3_1_ID_Day_Amount_0100.setText(m.group(1).replace(",", "."));

    	    m = MAmount_3_3_ID_Month_Amount_0100_REGEX.matcher(dane);
    	    if(m.find()) MAmount_3_3_ID_Month_Amount_0100.setText(m.group(1).replace(",", "."));

    	    m = MAmount_4_1_ID_Day_Amount_0200_REGEX.matcher(dane);
    	    if(m.find()) MAmount_4_1_ID_Day_Amount_0200.setText(m.group(1).replace(",", "."));

    	    m = MAmount_4_3_ID_Month_Amount_0200_REGEX.matcher(dane);
    	    if(m.find()) MAmount_4_3_ID_Month_Amount_0200.setText(m.group(1).replace(",", "."));
    	    
    	    // limity
    	    m = limit_MAmount_0_1_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_0_1 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_0_3_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_0_3 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_1_1_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_1_1 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_1_3_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_1_3 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_2_1_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_2_1 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_2_3_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_2_3 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_3_1_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_3_1 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_3_3_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_3_3 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_4_1_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_4_1 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    m = limit_MAmount_4_3_REGEX.matcher(dane);
    	    if(m.find())
    	    {
    	    	try
    	    	{
    	    		limit_MAmount_4_3 = new BigDecimal(m.group(1).replace(",", "."));	
    	    	}catch(NumberFormatException e) {}
    	    }

    	    // waluty
    	    m = MAmount_0_1_ID_Day_Amount_0000_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_0_1_ID_Day_Amount_0000_Curr.setText(m.group(1));

    	    m = MAmount_0_3_ID_Month_Amount_0000_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_0_3_ID_Month_Amount_0000_Curr.setText(m.group(1));

    	    m = MAmount_1_1_ID_Day_Amount_0003_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_1_1_ID_Day_Amount_0003_Curr.setText(m.group(1));
    	    
    	    m = MAmount_1_3_ID_Month_Amount_0003_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_1_3_ID_Month_Amount_0003_Curr.setText(m.group(1));

    	    m = MAmount_2_1_ID_Day_Amount_0004_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_2_1_ID_Day_Amount_0004_Curr.setText(m.group(1));

    	    m = MAmount_2_3_ID_Month_Amount_0004_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_2_3_ID_Month_Amount_0004_Curr.setText(m.group(1));

    	    m = MAmount_3_1_ID_Day_Amount_0100_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_3_1_ID_Day_Amount_0100_Curr.setText(m.group(1));

    	    m = MAmount_3_3_ID_Month_Amount_0100_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_3_3_ID_Month_Amount_0100_Curr.setText(m.group(1));

    	    m = MAmount_4_1_ID_Day_Amount_0200_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_4_1_ID_Day_Amount_0200_Curr.setText(m.group(1));

    	    m = MAmount_4_3_ID_Month_Amount_0200_Curr_REGEX.matcher(dane);
    	    if(m.find()) MAmount_4_3_ID_Month_Amount_0200_Curr.setText(m.group(1));

    	    // szukanie authTurnOff
        	m = Przelewy.authTurnOff_REGEX.matcher(dane);
        	if(m.find()) authTurnOff = m.group(1);
        	
        	// szukanie TransactionType
        	m = Przelewy.TransactionType_REGEX.matcher(dane);
        	if(m.find()) TransactionType = m.group(1);

        	
        	// dodanie sluchaczy na przyciski dalej i powrot
        	przycisk_powrot.setOnClickListener(new OnClickListener()
        	{
    			@Override
    			public void onClick(View arg0) {
    				finish();
    				
    			}
        	});
        	
        	przycisk_dalej.setOnClickListener(new OnClickListener()
        	{
    			@Override
    			public void onClick(View arg0) {
    				// jezeli dane zostaly wprowadzone poprawnie wykonaj przelew
    				if(sprawdzPoprawnoscDanych())
    				{
    					Bankoid.tworzProgressDialog(ZmianaLimitowKwotowych.this, getResources().getString(R.string.dialog_pobinfo));
    					Bankoid.dialog.show();
    					
    					watek = new Thread(ZmianaLimitowKwotowych.this, String.valueOf(Przelewy.DALEJ_KROK1));
    					watek.start();
    				}
    			}
        	});
        	return true;
    	}
   	    // czy wystapil standartowy blad
   	    else if(Bankoid.bledy.czyBlad())
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
	   	    else Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
   	    }

   	    return false;
    }
    
    private boolean sprawdzPoprawnoscDanych()
	{
		if(MAmount_0_1_ID_Day_Amount_0000.getText().length() == 0
				|| MAmount_0_3_ID_Month_Amount_0000.getText().length() == 0
				|| MAmount_1_1_ID_Day_Amount_0003.getText().length() == 0
				|| MAmount_1_3_ID_Month_Amount_0003.getText().length() == 0
				|| MAmount_2_1_ID_Day_Amount_0004.getText().length() == 0
				|| MAmount_2_3_ID_Month_Amount_0004.getText().length() == 0
				|| MAmount_3_1_ID_Day_Amount_0100.getText().length() == 0
				|| MAmount_3_3_ID_Month_Amount_0100.getText().length() == 0
				|| MAmount_4_1_ID_Day_Amount_0200.getText().length() == 0
				|| MAmount_4_3_ID_Month_Amount_0200.getText().length() == 0)
		{
			Toast.makeText(this.getApplicationContext(), R.string.karty_blad_puste, Toast.LENGTH_LONG).show();
			return false;
		}
		else
		{
	    	BigDecimal  MAmount_0_1 = null;
	    	BigDecimal  MAmount_0_3 = null;
	    	BigDecimal  MAmount_1_1 = null;
	    	BigDecimal  MAmount_1_3 = null;
	    	BigDecimal  MAmount_2_1 = null;
	    	BigDecimal  MAmount_2_3 = null;
	    	BigDecimal  MAmount_3_1 = null;
	    	BigDecimal  MAmount_3_3 = null;
	    	BigDecimal  MAmount_4_1 = null;
	    	BigDecimal  MAmount_4_3 = null;
			try
			{
				MAmount_0_1 = new BigDecimal(MAmount_0_1_ID_Day_Amount_0000.getText().toString());
				MAmount_0_3 = new BigDecimal(MAmount_0_3_ID_Month_Amount_0000.getText().toString());
				MAmount_1_1 = new BigDecimal(MAmount_1_1_ID_Day_Amount_0003.getText().toString());
				MAmount_1_3 = new BigDecimal(MAmount_1_3_ID_Month_Amount_0003.getText().toString());
				MAmount_2_1 = new BigDecimal(MAmount_2_1_ID_Day_Amount_0004.getText().toString());
				MAmount_2_3 = new BigDecimal(MAmount_2_3_ID_Month_Amount_0004.getText().toString());
				MAmount_3_1 = new BigDecimal(MAmount_3_1_ID_Day_Amount_0100.getText().toString());
				MAmount_3_3 = new BigDecimal(MAmount_3_3_ID_Month_Amount_0100.getText().toString());
				MAmount_4_1 = new BigDecimal(MAmount_4_1_ID_Day_Amount_0200.getText().toString());
				MAmount_4_3 = new BigDecimal(MAmount_4_3_ID_Month_Amount_0200.getText().toString());
			}catch(NumberFormatException e) {
				Toast.makeText(this.getApplicationContext(), R.string.karty_blad_kwota, Toast.LENGTH_LONG).show();
				return false;
			}
			
			
			// sprawdzanie czy liczby mieszcza sie w limitach
			if(MAmount_0_1 != null && limit_MAmount_0_1 != null)
			{
				if(MAmount_0_1.compareTo(limit_MAmount_0_1) == 1)
				{
					String komunikat = String.format(getString(R.string.karty_blad_limit_dzienny), MAmount_0_1, limit_MAmount_0_1);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;
				}
						
			}
			if(MAmount_0_3 != null && limit_MAmount_0_3 != null)
			{
				if(MAmount_0_3.compareTo(limit_MAmount_0_3) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_miesieczny), MAmount_0_3, limit_MAmount_0_3);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;

				}	
			}
			if(MAmount_1_1 != null && limit_MAmount_1_1 != null)
			{
				if(MAmount_1_1.compareTo(limit_MAmount_1_1) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_dzienny), MAmount_1_1, limit_MAmount_1_1);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;

				}	
			}
			if(MAmount_1_3 != null && limit_MAmount_1_3 != null)
			{
				if(MAmount_1_3.compareTo(limit_MAmount_1_3) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_miesieczny), MAmount_1_3, limit_MAmount_1_3);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;
				}	
			}
			if(MAmount_2_1 != null && limit_MAmount_2_1 != null)
			{
				if(MAmount_2_1.compareTo(limit_MAmount_2_1) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_dzienny), MAmount_2_1, limit_MAmount_2_1);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;
				}	
			}
			if(MAmount_2_3 != null && limit_MAmount_2_3 != null)
			{
				if(MAmount_2_3.compareTo(limit_MAmount_2_3) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_miesieczny), MAmount_2_3, limit_MAmount_2_3);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;
				}	
			}
			if(MAmount_3_1 != null && limit_MAmount_3_1 != null)
			{
				if(MAmount_3_1.compareTo(limit_MAmount_3_1) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_dzienny), MAmount_3_1, limit_MAmount_3_1);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;

				}	
			}
			if(MAmount_3_3 != null && limit_MAmount_3_3 != null)
			{
				if(MAmount_3_3.compareTo(limit_MAmount_3_3) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_miesieczny), MAmount_3_3, limit_MAmount_3_3);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;
				}	
			}
			if(MAmount_4_1 != null && limit_MAmount_4_1 != null)
			{
				if(MAmount_4_1.compareTo(limit_MAmount_4_1) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_dzienny), MAmount_4_1, limit_MAmount_4_1);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;
				}	
			}
			if(MAmount_4_3 != null && limit_MAmount_4_3 != null)
			{
				if(MAmount_4_3.compareTo(limit_MAmount_4_3) == 1) {
					String komunikat = String.format(getString(R.string.karty_blad_limit_miesieczny), MAmount_4_3, limit_MAmount_4_3);
					Toast.makeText(this.getApplicationContext(), komunikat, Toast.LENGTH_LONG).show();
					return false;
				}	
			}
			
			// sprawdzanie czy limit dzienny jest wiekszy od miesiecznego
			if(MAmount_0_1 != null && MAmount_0_3 != null)
			{
				if(MAmount_0_1.compareTo(MAmount_0_3) == 1)
				{
					Toast.makeText(this.getApplicationContext(), R.string.karty_blad_dzienny, Toast.LENGTH_LONG).show();
					return false;
				}
			}

			if(MAmount_1_1 != null && MAmount_1_3 != null)
			{
				if(MAmount_1_1.compareTo(MAmount_1_3) == 1)
				{
					Toast.makeText(this.getApplicationContext(), R.string.karty_blad_dzienny, Toast.LENGTH_LONG).show();
					return false;
				}
			}
			if(MAmount_2_1 != null && MAmount_2_3 != null)
			{
				if(MAmount_2_1.compareTo(MAmount_2_3) == 1)
				{
					Toast.makeText(this.getApplicationContext(), R.string.karty_blad_dzienny, Toast.LENGTH_LONG).show();
					return false;
				}
			}
			if(MAmount_3_1 != null && MAmount_3_3 != null)
			{
				if(MAmount_3_1.compareTo(MAmount_3_3) == 1)
				{
					Toast.makeText(this.getApplicationContext(), R.string.karty_blad_dzienny, Toast.LENGTH_LONG).show();
					return false;
				}
			}
			if(MAmount_4_1 != null && MAmount_4_3 != null)
			{
				if(MAmount_4_1.compareTo(MAmount_4_3) == 1)
				{
					Toast.makeText(this.getApplicationContext(), R.string.karty_blad_dzienny, Toast.LENGTH_LONG).show();
					return false;
				}
			}
		}

		return true;
	}

    // zwraca false jezeli nalezy przeladowac
	private String wykonajPrzelewKrok1()
	{
		String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_change_limits.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramDalej);
	    request.addParam("__CurrentWizardStep", "1");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("MAmount_0_1_ID_Day_Amount_0000_Curr", MAmount_0_1_ID_Day_Amount_0000_Curr.getText().toString());
	    request.addParam("MAmount_0_3_ID_Month_Amount_0000_Curr", MAmount_0_3_ID_Month_Amount_0000_Curr.getText().toString());
	    request.addParam("MAmount_1_1_ID_Day_Amount_0003_Curr", MAmount_1_1_ID_Day_Amount_0003_Curr.getText().toString());
	    request.addParam("MAmount_1_3_ID_Month_Amount_0003_Curr", MAmount_1_3_ID_Month_Amount_0003_Curr.getText().toString());
	    request.addParam("MAmount_2_1_ID_Day_Amount_0004_Curr", MAmount_2_1_ID_Day_Amount_0004_Curr.getText().toString());
	    request.addParam("MAmount_2_3_ID_Month_Amount_0004_Curr", MAmount_2_3_ID_Month_Amount_0004_Curr.getText().toString());
	    request.addParam("MAmount_3_1_ID_Day_Amount_0100_Curr", MAmount_3_1_ID_Day_Amount_0100_Curr.getText().toString());
	    request.addParam("MAmount_3_3_ID_Month_Amount_0100_Curr", MAmount_3_3_ID_Month_Amount_0100_Curr.getText().toString());
	    request.addParam("MAmount_4_1_ID_Day_Amount_0200_Curr", MAmount_4_1_ID_Day_Amount_0200_Curr.getText().toString());
	    request.addParam("MAmount_4_3_ID_Month_Amount_0200_Curr", MAmount_4_3_ID_Month_Amount_0200_Curr.getText().toString());
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("cardMaxAmountLimitsList_grid$ctl03$MAmount_0_1_ID_Day_Amount_0000", MAmount_0_1_ID_Day_Amount_0000.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl03$MAmount_0_3_ID_Month_Amount_0000", MAmount_0_3_ID_Month_Amount_0000.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl04$MAmount_1_1_ID_Day_Amount_0003", MAmount_1_1_ID_Day_Amount_0003.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl04$MAmount_1_3_ID_Month_Amount_0003", MAmount_1_3_ID_Month_Amount_0003.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl05$MAmount_2_1_ID_Day_Amount_0004", MAmount_2_1_ID_Day_Amount_0004.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl05$MAmount_2_3_ID_Month_Amount_0004", MAmount_2_3_ID_Month_Amount_0004.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl06$MAmount_3_1_ID_Day_Amount_0100", MAmount_3_1_ID_Day_Amount_0100.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl06$MAmount_3_3_ID_Month_Amount_0100", MAmount_3_3_ID_Month_Amount_0100.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl07$MAmount_4_1_ID_Day_Amount_0200", MAmount_4_1_ID_Day_Amount_0200.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl07$MAmount_4_3_ID_Month_Amount_0200", MAmount_4_3_ID_Month_Amount_0200.getText().toString().replace(".", ","));
	    
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
	    	// jezeli odczytano blad to zamknij formularz
		    m = Przelewy.bladPrzelewREGEX.matcher(rezultat);
		    if(m.find())
		    {
		    	String bladTresc = Html.fromHtml(m.group(2).replace("&shy;<wbr />", "")).toString();
		    	Bankoid.bledy.ustawBlad(m.group(1).replace("&shy;<wbr />", "").trim(), bladTresc, Bledy.ZAMKNIJ_OKNO);

		    }
		    // niestandartowy blad
		    else
		    {
		    	Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
		    }
	    }
	    return null;
	    
	}

	// tworzy i pokazuje formularz z kodem sms
	private void tworzFormularzKrok2(String dane)
	{
    	// tworzenie dialogu z formularzem dla kroku 2
    	krok2 = new Dialog(this, R.style.CustomTheme);
		krok2.setContentView(R.layout.dialog_krok2_limit);
		krok2.setTitle("Zmiana limitów - kwotowych");

        krok2.getWindow().setFormat(PixelFormat.RGBA_8888);
        krok2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

		//////////////////////////////////////////////
    	TextView MAmount_0_1_ID_Day_Amount_0000 = (TextView) krok2.findViewById(R.id.pole_0_1_ID_Day);
    	TextView MAmount_0_3_ID_Month_Amount_0000 = (TextView) krok2.findViewById(R.id.pole_0_3_ID_Month);
    	TextView MAmount_1_1_ID_Day_Amount_0003 = (TextView) krok2.findViewById(R.id.pole_1_1_ID_Day);
    	TextView MAmount_1_3_ID_Month_Amount_0003 = (TextView) krok2.findViewById(R.id.pole_1_3_ID_Month);
    	TextView MAmount_2_1_ID_Day_Amount_0004 = (TextView) krok2.findViewById(R.id.pole_2_1_ID_Day);
    	TextView MAmount_2_3_ID_Month_Amount_0004 = (TextView) krok2.findViewById(R.id.pole_2_3_ID_Month);
    	TextView MAmount_3_1_ID_Day_Amount_0100 = (TextView) krok2.findViewById(R.id.pole_3_1_ID_Day);
    	TextView MAmount_3_3_ID_Month_Amount_0100 = (TextView) krok2.findViewById(R.id.pole_3_3_ID_Month);
    	TextView MAmount_4_1_ID_Day_Amount_0200 = (TextView) krok2.findViewById(R.id.pole_4_1_ID_Day);
    	TextView MAmount_4_3_ID_Month_Amount_0200 = (TextView) krok2.findViewById(R.id.pole_4_3_ID_Month);
		TextView haslo_sms_label = (TextView) krok2.findViewById(R.id.haslo_sms_label);
		TextView tresc_stopka = (TextView) krok2.findViewById(R.id.tresc_stopka);
    	final EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
		Button przycisk_powrot = (Button) krok2.findViewById(R.id.przycisk_powrot);
		Button przycisk_modyfikuj = (Button) krok2.findViewById(R.id.przycisk_modyfikuj);
		Button przycisk_zatwierdz = (Button) krok2.findViewById(R.id.przycisk_zatwierdz);
		Button przycisk_potwierdz_pozniej = (Button) krok2.findViewById(R.id.przycisk_potwierdz_pozniej);
		//////////////////////////////////////////////
		
		MAmount_0_1_ID_Day_Amount_0000.setText(ZmianaLimitowKwotowych.this.MAmount_0_1_ID_Day_Amount_0000.getText().toString().replace(".", ",") + " " +MAmount_0_1_ID_Day_Amount_0000_Curr.getText());
		MAmount_0_3_ID_Month_Amount_0000.setText(ZmianaLimitowKwotowych.this.MAmount_0_3_ID_Month_Amount_0000.getText().toString().replace(".", ",") + " " +MAmount_0_3_ID_Month_Amount_0000_Curr.getText());
		MAmount_1_1_ID_Day_Amount_0003.setText(ZmianaLimitowKwotowych.this.MAmount_1_1_ID_Day_Amount_0003.getText().toString().replace(".", ",") + " " +MAmount_1_1_ID_Day_Amount_0003_Curr.getText());
		MAmount_1_3_ID_Month_Amount_0003.setText(ZmianaLimitowKwotowych.this.MAmount_1_3_ID_Month_Amount_0003.getText().toString().replace(".", ",") + " " +MAmount_1_3_ID_Month_Amount_0003_Curr.getText());
		MAmount_2_1_ID_Day_Amount_0004.setText(ZmianaLimitowKwotowych.this.MAmount_2_1_ID_Day_Amount_0004.getText().toString().replace(".", ",") + " " +MAmount_2_1_ID_Day_Amount_0004_Curr.getText());
		MAmount_2_3_ID_Month_Amount_0004.setText(ZmianaLimitowKwotowych.this.MAmount_2_3_ID_Month_Amount_0004.getText().toString().replace(".", ",") + " " +MAmount_2_3_ID_Month_Amount_0004_Curr.getText());
		MAmount_3_1_ID_Day_Amount_0100.setText(ZmianaLimitowKwotowych.this.MAmount_3_1_ID_Day_Amount_0100.getText().toString().replace(".", ",") + " " +MAmount_3_1_ID_Day_Amount_0100_Curr.getText());
		MAmount_3_3_ID_Month_Amount_0100.setText(ZmianaLimitowKwotowych.this.MAmount_3_3_ID_Month_Amount_0100.getText().toString().replace(".", ",") + " " +MAmount_3_3_ID_Month_Amount_0100_Curr.getText());
		MAmount_4_1_ID_Day_Amount_0200.setText(ZmianaLimitowKwotowych.this.MAmount_4_1_ID_Day_Amount_0200.getText().toString().replace(".", ",") + " " +MAmount_4_1_ID_Day_Amount_0200_Curr.getText());
		MAmount_4_3_ID_Month_Amount_0200.setText(ZmianaLimitowKwotowych.this.MAmount_4_3_ID_Month_Amount_0200.getText().toString().replace(".", ",") + " " +MAmount_4_3_ID_Month_Amount_0200_Curr.getText());
		
		// szukanie tresci label sms
		Matcher m = Przelewy.smslabelREGEX.matcher(dane);
		if(m.find())
		{
			// jezeli wersja bez reklam
			if(Bankoid.reklamy == false && Ver.wersja != null && Bankoid.pref_sms)
				registerReceiver(new SMSReceiver(m.group(1), haslo_sms), new IntentFilter(SMSReceiver.SMS_RECEIVED));

			haslo_sms_label.setText(Html.fromHtml(m.group(1)).toString().trim());
		}
		
		// szukanie tresci label sms stopka
		m = Przelewy.smslabelstopkaREGEX.matcher(dane);
		if(m.find())
		{
			tresc_stopka.setText(Html.fromHtml(m.group(1)).toString().trim());
			tresc_stopka.setVisibility(View.VISIBLE);
		}else tresc_stopka.setVisibility(View.GONE);
		
		// szukanie TransactionType
		m = Przelewy.TransactionType_REGEX.matcher(dane);
		if(m.find()) TransactionType = m.group(1);

		// szukanie parameters modyfikuj
		m = Przelewy.modyfikujREGEX.matcher(dane);
		if(m.find()) paramModyfikuj = m.group(1);
		
		// szukanie parameters potwierdz pozniej
		m = Przelewy.potwierdzpozniejREGEX.matcher(dane);
		if(m.find())
		{
			paramPotwierdzPozniej = m.group(1);
			przycisk_potwierdz_pozniej.setVisibility(View.VISIBLE);
			haslo_sms_label.setVisibility(View.VISIBLE);
			haslo_sms.setVisibility(View.VISIBLE);
			tresc_stopka.setVisibility(View.VISIBLE);
		}
		// jezeli nie znaleziono tzn ze nie potrzeba autoryzacji na kod sms
		else
		{
			przycisk_potwierdz_pozniej.setVisibility(View.GONE);
			haslo_sms_label.setVisibility(View.GONE);
			haslo_sms.setVisibility(View.GONE);
			tresc_stopka.setVisibility(View.GONE);
		}
		
	
		// w przypadku anulowania powrot do szczegolow karty
		krok2.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface krok2) {
				krok2.dismiss();
				ZmianaLimitowKwotowych.this.finish();
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
				Bankoid.tworzProgressDialog(ZmianaLimitowKwotowych.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(ZmianaLimitowKwotowych.this, String.valueOf(Przelewy.MODYFIKUJ_KROK2));
				watek.start();
			}
		});

		// akcja dla przycisku zatwierdz
		przycisk_zatwierdz.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				if(haslo_sms.isShown() && haslo_sms.getText().length() < 5)
				{
					Toast.makeText(ZmianaLimitowKwotowych.this, R.string.przelewy_blad_haslo, Toast.LENGTH_LONG).show();						
				}
				else
				{
					Bankoid.tworzProgressDialog(ZmianaLimitowKwotowych.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
					
					watek = new Thread(ZmianaLimitowKwotowych.this, String.valueOf(Przelewy.ZATWIERDZ_KROK2));
					watek.start();
				}
			}
		});

		// akcja dla przycisku zatwierdz pozniej
		przycisk_potwierdz_pozniej.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				Bankoid.tworzProgressDialog(ZmianaLimitowKwotowych.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(ZmianaLimitowKwotowych.this, String.valueOf(Przelewy.ZATWIERDZ_POZNIEJ_KROK2));
				watek.start();
			}
		});
	}
	
	// metoda wywolywana po kliknieciu modyfikuj
    private boolean modyfikujPrzelew()
    {    	
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_change_limits.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("__PARAMETERS", paramModyfikuj);
	    request.addParam("__CurrentWizardStep", "2");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("MAmount_0_1_ID_Day_Amount_0000_Curr", MAmount_0_1_ID_Day_Amount_0000_Curr.getText().toString());
	    request.addParam("MAmount_0_3_ID_Month_Amount_0000_Curr", MAmount_0_3_ID_Month_Amount_0000_Curr.getText().toString());
	    request.addParam("MAmount_1_1_ID_Day_Amount_0003_Curr", MAmount_1_1_ID_Day_Amount_0003_Curr.getText().toString());
	    request.addParam("MAmount_1_3_ID_Month_Amount_0003_Curr", MAmount_1_3_ID_Month_Amount_0003_Curr.getText().toString());
	    request.addParam("MAmount_2_1_ID_Day_Amount_0004_Curr", MAmount_2_1_ID_Day_Amount_0004_Curr.getText().toString());
	    request.addParam("MAmount_2_3_ID_Month_Amount_0004_Curr", MAmount_2_3_ID_Month_Amount_0004_Curr.getText().toString());
	    request.addParam("MAmount_3_1_ID_Day_Amount_0100_Curr", MAmount_3_1_ID_Day_Amount_0100_Curr.getText().toString());
	    request.addParam("MAmount_3_3_ID_Month_Amount_0100_Curr", MAmount_3_3_ID_Month_Amount_0100_Curr.getText().toString());
	    request.addParam("MAmount_4_1_ID_Day_Amount_0200_Curr", MAmount_4_1_ID_Day_Amount_0200_Curr.getText().toString());
	    request.addParam("MAmount_4_3_ID_Month_Amount_0200_Curr", MAmount_4_3_ID_Month_Amount_0200_Curr.getText().toString());
	    request.addParam("cardMaxAmountLimitsList_grid$ctl03$MAmount_0_1_ID_Day_Amount_0000", MAmount_0_1_ID_Day_Amount_0000.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl03$MAmount_0_3_ID_Month_Amount_0000", MAmount_0_3_ID_Month_Amount_0000.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl04$MAmount_1_1_ID_Day_Amount_0003", MAmount_1_1_ID_Day_Amount_0003.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl04$MAmount_1_3_ID_Month_Amount_0003", MAmount_1_3_ID_Month_Amount_0003.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl05$MAmount_2_1_ID_Day_Amount_0004", MAmount_2_1_ID_Day_Amount_0004.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl05$MAmount_2_3_ID_Month_Amount_0004", MAmount_2_3_ID_Month_Amount_0004.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl06$MAmount_3_1_ID_Day_Amount_0100", MAmount_3_1_ID_Day_Amount_0100.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl06$MAmount_3_3_ID_Month_Amount_0100", MAmount_3_3_ID_Month_Amount_0100.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl07$MAmount_4_1_ID_Day_Amount_0200", MAmount_4_1_ID_Day_Amount_0200.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl07$MAmount_4_3_ID_Month_Amount_0200", MAmount_4_3_ID_Month_Amount_0200.getText().toString().replace(".", ","));
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("authCode", "");
	    
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
   	    	Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
	// metoda wywolywana po kliknieciu potwierdz pozniej
    private boolean potwierdzPozniejPrzelew()
    {
    	String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
    	sfRequest request = sfClient.getInstance().createRequest();
    	request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_card_max_limits_list.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", this.paramPotwierdzPozniej);
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
    	// sprawdzenie czy udalo sie wrocic do szczegolow karty	
	    if(rezultat.contains("krok 1/3")) return true;
   	    else if(Bankoid.bledy.czyBlad() == false)
   	    {
   	    	Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
   	    }
   	    return false;
    }
    
	// metoda wywolywana po kliknieciu zatwierdz
    private void wykonajPrzelewKrok2()
    {
		EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
    	
		String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_change_limits.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("__PARAMETERS", paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "2");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("MAmount_0_1_ID_Day_Amount_0000_Curr", MAmount_0_1_ID_Day_Amount_0000_Curr.getText().toString());
	    request.addParam("MAmount_0_3_ID_Month_Amount_0000_Curr", MAmount_0_3_ID_Month_Amount_0000_Curr.getText().toString());
	    request.addParam("MAmount_1_1_ID_Day_Amount_0003_Curr", MAmount_1_1_ID_Day_Amount_0003_Curr.getText().toString());
	    request.addParam("MAmount_1_3_ID_Month_Amount_0003_Curr", MAmount_1_3_ID_Month_Amount_0003_Curr.getText().toString());
	    request.addParam("MAmount_2_1_ID_Day_Amount_0004_Curr", MAmount_2_1_ID_Day_Amount_0004_Curr.getText().toString());
	    request.addParam("MAmount_2_3_ID_Month_Amount_0004_Curr", MAmount_2_3_ID_Month_Amount_0004_Curr.getText().toString());
	    request.addParam("MAmount_3_1_ID_Day_Amount_0100_Curr", MAmount_3_1_ID_Day_Amount_0100_Curr.getText().toString());
	    request.addParam("MAmount_3_3_ID_Month_Amount_0100_Curr", MAmount_3_3_ID_Month_Amount_0100_Curr.getText().toString());
	    request.addParam("MAmount_4_1_ID_Day_Amount_0200_Curr", MAmount_4_1_ID_Day_Amount_0200_Curr.getText().toString());
	    request.addParam("MAmount_4_3_ID_Month_Amount_0200_Curr", MAmount_4_3_ID_Month_Amount_0200_Curr.getText().toString());
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
	    request.addParam("cardMaxAmountLimitsList_grid$ctl03$MAmount_0_1_ID_Day_Amount_0000", MAmount_0_1_ID_Day_Amount_0000.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl03$MAmount_0_3_ID_Month_Amount_0000", MAmount_0_3_ID_Month_Amount_0000.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl04$MAmount_1_1_ID_Day_Amount_0003", MAmount_1_1_ID_Day_Amount_0003.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl04$MAmount_1_3_ID_Month_Amount_0003", MAmount_1_3_ID_Month_Amount_0003.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl05$MAmount_2_1_ID_Day_Amount_0004", MAmount_2_1_ID_Day_Amount_0004.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl05$MAmount_2_3_ID_Month_Amount_0004", MAmount_2_3_ID_Month_Amount_0004.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl06$MAmount_3_1_ID_Day_Amount_0100", MAmount_3_1_ID_Day_Amount_0100.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl06$MAmount_3_3_ID_Month_Amount_0100", MAmount_3_3_ID_Month_Amount_0100.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl07$MAmount_4_1_ID_Day_Amount_0200", MAmount_4_1_ID_Day_Amount_0200.getText().toString().replace(".", ","));
	    request.addParam("cardMaxAmountLimitsList_grid$ctl07$MAmount_4_3_ID_Month_Amount_0200", MAmount_4_3_ID_Month_Amount_0200.getText().toString().replace(".", ","));
	    request.addParam("authCode", haslo_sms.getText().toString());
	    		
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
   	    		Bankoid.bledy.ustawBlad(m.group(1).replace("&shy;<wbr />", "").trim(), m.group(2).trim(), Bledy.ZAMKNIJ_OKNO);
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
	   	    		Bankoid.bledy.ustawBlad(m.group(1).replace("&shy;<wbr />", "").trim(), bladTresc, Bledy.POWROT);
	   	    		Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	   	    		// zaladuj ponownie formularz umozliwiajac wpisanie poprawnego hasla
	   	    		// jezeli nie udalo sie zaladowac ponownie formularza to zamknij przelewy
	   	    		if(ponowHaslo(rezultat) == false) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
	   	    	}
	   	    	else
	   	    	{
	   	    		// niestandartowy blad
	   	    		Bankoid.bledy.ustawBlad(R.string.karty_blad, Bledy.WYLOGUJ);
	   	    	}
   	    	}
   	    }
    }
    
    // laduje ponownie formularz z krokiem 2 umozliwiajac ponowienie hasla
    private boolean ponowHaslo(String dane)
    {
    	
    	Matcher m = Przelewy.powrotREGEX.matcher(dane);
    	if(m.find()) paramPowrot = m.group(1);
    	
    	// szukanie authTurnOff
    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
    	if(m.find()) authTurnOff = m.group(1);
    	
		String rozszerzenie = Karty.wybrana_karta.pobierzRozszerzenie();
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/"+rozszerzenie+"_change_limits.aspx");
	    request.setMethod("POST");
	    
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramPowrot);
	    request.addParam("__CurrentWizardStep", "3");
	    if(TransactionType != null)
	    {
	    	request.addParam("TransactionType", TransactionType);
	    }
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    
	    request.execute();

	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    
		// szukanie parameters zatwierdz
		m = Przelewy.zatwierdzREGEX.matcher(rezultat);
	    // jezeli zatwierdz istnieje to udalo sie przejsc do kroku 2 zwroc true
		if(m.find())
	    {
			paramZatwierdz = m.group(1);
			
			// szukanie tresci label sms
			m = Przelewy.smslabelREGEX.matcher(rezultat);
			if(m.find()) haslo_sms_label = Html.fromHtml(m.group(1)).toString().trim();

			// szukanie parameters potwierdz pozniej
			m = Przelewy.potwierdzpozniejREGEX.matcher(rezultat);
			if(m.find()) paramPotwierdzPozniej = m.group(1);

			// szukanie parameters modyfikuj
			m = Przelewy.modyfikujREGEX.matcher(rezultat);
			if(m.find()) paramModyfikuj = m.group(1);

			return true;
	    }
	    else return false;
	    
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
       // MenuItem wylacz_reklamy = menu.findItem(R.id.wylacz_reklamy);
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
						
	                	Bankoid.tworzProgressDialog(ZmianaLimitowKwotowych.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(ZmianaLimitowKwotowych.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
}
