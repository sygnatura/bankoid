package pl.bankoid;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;

import com.google.ads.AdSize;
import com.google.ads.AdView;

public class ListaPotwierdzenia extends ListActivity implements Runnable {

	private AdapterPotwierdzenia m_adapter = null;
	private Potwierdzenie wybranePotwierdzenie = null;
	private Thread watek;
	private String noweDane = null;
	private AdView adView;
	
	// regex
	Pattern operacjaREGEX = Pattern.compile("id=\"tbOperationType\" value=\"([^\"]+)\"");
	Pattern opisREGEX = Pattern.compile("id=\"tbOperationDesc\" value=\"([^\"]+)\"");
	Pattern wiadomoscREGEX = Pattern.compile("<p class=\"message\">([^<]+)</p>.+?</fieldset><fieldset>.+?<label class=\"label\">Operacja</label>", Pattern.DOTALL);
	
	// operacje
	private final int POTWIERDZ_HASLEM = 1000;
	private final int WYSLIJ_HASLO_SMS = 1001;
	private final int USUN = 1002;
	private final int POTWIERDZENIE_USUN = 1003;
	
	// dialog krok2
	private Dialog krok2;
	private Komunikat potwierdzUsun;
	
	// param przyciskow
	private String paramZatwierdz;
	private String paramPowrot;
	
	// zmienne
	String tbOperationType;
	String tbOperationDesc;
	String TransactionType;
	String authTurnOff;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_potwierdzenia);
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
       
        this.setTitle("Operacje do potwierdzenia");
        
    	// automatycznie zamknij okno jezeli nie jest sie zalogowanym
    	if(Bankoid.zalogowano == false) this.finish();
        
    	// jezeli nie udalo sie otworzyc okno z param hisotrii
        if(tworzPotwierdzenia(dane) == false) Bankoid.bledy.pokazKomunikat(this);

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

    		case POTWIERDZ_HASLEM:
    			String dane = wybierzPotwierdzHaslem();
				Message msg = handler.obtainMessage();
				msg.what = POTWIERDZ_HASLEM;
				Bundle b = new Bundle();
				b.putString("dane", dane);
                msg.setData(b);
                handler.sendMessage(msg);
            	break;
    		
    		case WYSLIJ_HASLO_SMS:
    			dane = wybierzWyslijPonownieHaslo();
				msg = handler.obtainMessage();
				msg.what = WYSLIJ_HASLO_SMS;
				b = new Bundle();
				b.putString("dane", dane);
                msg.setData(b);
                handler.sendMessage(msg);
            	break;
            	
            	// pokazuje dialog z potwierdzeniem usuniecia
    		case USUN:
    			dane = wybierzUsunPotwierdzenie();
				msg = handler.obtainMessage();
				msg.what = USUN;
				b = new Bundle();
				b.putString("dane", dane);
                msg.setData(b);
                handler.sendMessage(msg);
            	break;
            	
            	// potwierdzenie usuniecia wybranego potwierdzenia
    		case POTWIERDZENIE_USUN:
    			usuwaniePotwierdzenia();
            	handler.sendEmptyMessage(POTWIERDZENIE_USUN);
            	break;
            	
            	// potwierdzenie smsa
    		case Przelewy.ZATWIERDZ_KROK2:
    			zatwierdzKodSMS();
            	handler.sendEmptyMessage(Przelewy.ZATWIERDZ_KROK2);
            	break;
            	
            	// wywylywane po skasowaniu badz zatwierdzeniu potwierdzenia
    		case Bankoid.ODSWIEZANIE:
    			noweDane = MenuGlowne.wybierzPotwierdzenia();
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
					 ListaPotwierdzenia.this.finish();
					 break;
    		
    			case POTWIERDZ_HASLEM:
  					 String dane = msg.getData().getString("dane");
					 if(dane != null)
					 {
						 tworzFormularzKrok2(dane);
	    				// czy wystapil blad
						 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ListaPotwierdzenia.this);
						 else krok2.show();
					 }
	   				break;
    				
    			case WYSLIJ_HASLO_SMS:
   					 dane = msg.getData().getString("dane");
					 if(dane != null)
					 {
						 tworzFormularzKrok2(dane);
						 if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ListaPotwierdzenia.this);
						 else krok2.show();
					 }
	   				break;
	   				
    			case USUN:
   					dane = msg.getData().getString("dane");
	   				if(dane != null)
	   				{
		   				tworzDialogUsun(dane);
		   				if(Bankoid.bledy.czyBlad()) Bankoid.bledy.pokazKomunikat(ListaPotwierdzenia.this);
		   				else potwierdzUsun.show();
	   				}
    				break;
    				
    			case POTWIERDZENIE_USUN:
    				// czy wystapil blad
	   				Bankoid.bledy.pokazKomunikat(ListaPotwierdzenia.this);
	   				// odswiezenie oka z lista potwierdzen
					Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
						
					watek = new Thread(ListaPotwierdzenia.this, String.valueOf(Bankoid.ODSWIEZANIE));
					watek.start();	

    				break;
    				
    			case Przelewy.ZATWIERDZ_KROK2:
    				// pokaz blad zawierajacy czy przelew zostal zrealizowany
    				if(Bankoid.bledy.pobierzKodBledu() == Bledy.OK)
    				{
    					krok2.dismiss();
    					Bankoid.bledy.pokazKomunikat(ListaPotwierdzenia.this);
    	   				// odswiezenie okna nowa lista
    					tworzPotwierdzenia(noweDane);
    				}else Bankoid.bledy.pokazKomunikat(ListaPotwierdzenia.this);
   						

    				break;
    				
    			case Bankoid.ODSWIEZANIE:
    				// jezeli nie udalo sie odswiezyc listy potwierdzen to pokaz komunikat
    				if(tworzPotwierdzenia(noweDane) == false) Bankoid.bledy.pokazKomunikat(ListaPotwierdzenia.this);
    				noweDane = null;
    				break;
    		}

    	}
    };
    
    public boolean tworzPotwierdzenia(String dane)
    {
    	ArrayList<Potwierdzenie> potwierdzenia = new ArrayList<Potwierdzenie>();
    	
    	// nie pobrano danych
    	if(dane == null)
    	{
    		if(Bankoid.bledy.czyBlad()) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
    		else Bankoid.bledy.ustawBlad(R.string.potwierdzenia_blad, Bledy.WYLOGUJ);
    		return false;
    	}

    	// brak operacji do potwierdzenia
   	    if(dane.contains(getResources().getString(R.string.potwierdzenia_brak)))
   	    {
   	    	if(this.m_adapter != null)
   	    	{
   	    		this.m_adapter.clear();
   	    		this.m_adapter.notifyDataSetChanged();
   	    		wybranePotwierdzenie = null;
   	    	}
   	    	return true;
   	    }

    	Pattern potwierdzenieREGEX = Pattern.compile("<li( class=\"alternate\")?><p class=\"Numeric\"><span id=[^>]+>(\\d+)</span></p><p class=\"Date\"><span id=[^>]+>([^<]+)</span></p><p class=\"ShortDescription\"><span id=[^>]+>([^<]+)</span></p><p class=\"OperationDescription\"><a .+?doSubmit.+?'([^']{60,})'[^>]+>(.+?)</a></p><p class=\"Actions\"><a id.+?</a><a id.+?doSubmit.+?'([^']{60,})'[^>]+>.+?<a id.+?doSubmit.+?'([^']{60,})'[^>]+>");
    	Matcher m = potwierdzenieREGEX.matcher(dane);
    	while(m.find())
    	{
    		Potwierdzenie p = new Potwierdzenie();
    		p.ustawNumerOperacji(m.group(2));
    		p.ustawDateOperacji(m.group(3));
    		p.ustawOperacje(m.group(4));
    		p.ustawPotwierdzParam(m.group(5));
    		p.ustawOpisOperacji(m.group(6));
    		p.ustawWyslijParam(m.group(7));
    		p.ustawUsunParam(m.group(8));
    		
    		potwierdzenia.add(p);
    	}
	    // jezeli udalo sie pobrac potwierdzenia
	    if(potwierdzenia.size() > 0)
	    {
		    this.m_adapter = new AdapterPotwierdzenia(this, R.layout.wiersz_potwierdzenia, potwierdzenia);
		    this.setListAdapter(m_adapter);
		    
    		ListView lv = getListView();
    		this.registerForContextMenu(lv);
    		
    		lv.setOnItemClickListener(new OnItemClickListener() {
    		    public void onItemClick(AdapterView<?> parent, View view,
    		        int position, long id) {
    		    	
    		    	wybranePotwierdzenie = (Potwierdzenie) ListaPotwierdzenia.this.getListView().getItemAtPosition(position);
    		    	
    		    	if(wybranePotwierdzenie != null)
    		    	{
                    	Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
                    	Bankoid.dialog.show();
                    	
                    	watek = new Thread(ListaPotwierdzenia.this, String.valueOf(POTWIERDZ_HASLEM));;
                    	watek.start();
    		    	}
    		    }
    		  });
		    
		    return true;
	    }
	    return false;
    }
    
	// tworzy i pokazuje formularz z kodem sms
	private void tworzFormularzKrok2(String dane)
	{
    	// tworzenie dialogu z formularzem dla kroku 2
    	krok2 = new Dialog(this, R.style.CustomTheme);
    	krok2.getWindow().setFormat(PixelFormat.RGBA_8888);
        krok2.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        krok2.setCancelable(false);
		krok2.setContentView(R.layout.potwierdzenie_dialog);
		krok2.setTitle("Potwierdzenie operacji");
		//////////////////////////////////////////////
	
		TextView operacja = (TextView) krok2.findViewById(R.id.operacja);
		TextView opis_operacji = (TextView) krok2.findViewById(R.id.opis_operacji);
		TextView haslo_sms_label = (TextView) krok2.findViewById(R.id.haslo_sms_label);
		TextView tresc_stopka = (TextView) krok2.findViewById(R.id.tresc_stopka);
		TextView wiadomosc = (TextView) krok2.findViewById(R.id.wiadomosc);
		final EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
		Button przycisk_powrot = (Button) krok2.findViewById(R.id.przycisk_powrot);
		Button przycisk_zatwierdz = (Button) krok2.findViewById(R.id.przycisk_zatwierdz);
		//////////////////////////////////////////////
		
		// szukanie parameters zatwierdz
		Matcher m = Przelewy.zatwierdzREGEX.matcher(dane);
		if(m.find())
		{
			paramZatwierdz = m.group(1);
			
			// szukanie operacji
			m = operacjaREGEX.matcher(dane);
			if(m.find()) operacja.setText(Html.fromHtml(m.group(1)));
			
			// szukanie opisu
			m = opisREGEX.matcher(dane);
			if(m.find()) opis_operacji.setText(Html.fromHtml(m.group(1)));
			
			// szukanie tresci label sms
			m = Przelewy.smslabelREGEX.matcher(dane);
			if(m.find())
			{
				// jezeli wersja bez reklam
				if(Bankoid.pref_sms)
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

			// szukanie tresci wiadomosci
			m = wiadomoscREGEX.matcher(dane);
			if(m.find())
			{
				wiadomosc.setText(Html.fromHtml(m.group(1)));
				wiadomosc.setVisibility(View.VISIBLE);
			}else wiadomosc.setVisibility(View.GONE);
			
			// szukanie TransactionType
			m = Przelewy.TransactionType_REGEX.matcher(dane);
			if(m.find()) TransactionType = m.group(1);
			
			// akcja dla przycisku powrot odswiezenie okna
			przycisk_powrot.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v) {
					krok2.cancel();
					Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
					Bankoid.dialog.show();
						
					watek = new Thread(ListaPotwierdzenia.this, String.valueOf(Bankoid.ODSWIEZANIE));
					watek.start();	
				}
			});

			// akcja dla przycisku zatwierdz
			przycisk_zatwierdz.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v) {
					if(haslo_sms.getText().length() != 8)
					{
						Toast.makeText(ListaPotwierdzenia.this, R.string.przelewy_blad_haslo, Toast.LENGTH_LONG).show();						
					}
					else
					{
						Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
						Bankoid.dialog.show();
						
						watek = new Thread(ListaPotwierdzenia.this, String.valueOf(Przelewy.ZATWIERDZ_KROK2));
						watek.start();
					}
				}
			});
		}
   	    // czy wystapil standartowy blad
   	    else if(Bankoid.bledy.czyBlad() == false)
   	    {
   	    	// czy jest komunikat z bledem
   	    	m = Przelewy.bladPrzelewREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
		    	String bladTresc = Html.fromHtml(m.group(2)).toString();
		    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.INFO);
		    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
   	    	}
	   	    // niestandartowy blad
	   	    else Bankoid.bledy.ustawBlad(R.string.potwierdzenia_blad, Bledy.WYLOGUJ);
   	    }
	}
	
	// tworzy dialog z potwierdzeniem usunieca
	private void tworzDialogUsun(String dane)
	{
        // tworzenie dialogu z potwierdzeniem usiniecia
        potwierdzUsun = new Komunikat(ListaPotwierdzenia.this);
        potwierdzUsun.ustawIkone(android.R.drawable.ic_menu_help);
        potwierdzUsun.ustawTytul("Usunięcie operacji do potwierdzenia");
        potwierdzUsun.ustawPrzyciskNie("Powrót", new View.OnClickListener()
        {
        	// kazdorazowe odswiezanie listy potwierdzen po udanej lub nie operacji na potwierdzeniach	
			@Override
			public void onClick(View v)
			{
				potwierdzUsun.dismiss();
				Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
					
				watek = new Thread(ListaPotwierdzenia.this, String.valueOf(Bankoid.ODSWIEZANIE));
				watek.start();	
			}
		});
        potwierdzUsun.ustawPrzyciskTak("Usuń", new View.OnClickListener()
        {
			@Override
			public void onClick(View v) {
				potwierdzUsun.dismiss();
				Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
				Bankoid.dialog.show();
				
				watek = new Thread(ListaPotwierdzenia.this, String.valueOf(POTWIERDZENIE_USUN));
				watek.start();
			}

        });
        potwierdzUsun.setCancelable(false);
        
		// szukanie parameters zatwierdz
		Matcher m = Przelewy.zatwierdzREGEX.matcher(dane);
		if(m.find())
		{
			paramZatwierdz = m.group(1);
			
			// szukanie opisu
			m = opisREGEX.matcher(dane);
			if(m.find()) tbOperationDesc = m.group(1);
			
			// szukanie TransactionType
			m = Przelewy.TransactionType_REGEX.matcher(dane);
			if(m.find()) TransactionType = m.group(1);
			
	    	// szukanie authTurnOff
	    	m = Przelewy.authTurnOff_REGEX.matcher(dane);
	    	if(m.find()) authTurnOff = m.group(1);
			
	        potwierdzUsun.ustawTresc(tbOperationDesc);

		}
   	    // czy wystapil standartowy blad
   	    else if(Bankoid.bledy.czyBlad() == false)
   	    {
   	    	// czy jest komunikat z bledem
   	    	m = Przelewy.bladPrzelewREGEX.matcher(dane);
   	    	if(m.find())
   	    	{
		    	String bladTresc = Html.fromHtml(m.group(2)).toString();
		    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.INFO);
		    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
   	    	}
	   	    // niestandartowy blad
	   	    else Bankoid.bledy.ustawBlad(R.string.potwierdzenia_blad, Bledy.WYLOGUJ);
   	    }
	}
	
	// menu context
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.context_menu_potwierdzenia, menu);
    }

    // wybrano opcje z context menu
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
	      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	      wybranePotwierdzenie = (Potwierdzenie) ListaPotwierdzenia.this.getListView().getItemAtPosition(info.position);
	      
	      switch (item.getItemId())
	      {
		      case R.id.potwierdz:
					if(wybranePotwierdzenie != null)
					{
						Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
						Bankoid.dialog.show();
						
						watek = new Thread(ListaPotwierdzenia.this, String.valueOf(POTWIERDZ_HASLEM));;
						watek.start();
					}
		    	  return true;
		    	  
		      case R.id.wyslij:
					if(wybranePotwierdzenie != null)
					{
						Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
						Bankoid.dialog.show();
						
						watek = new Thread(ListaPotwierdzenia.this, String.valueOf(WYSLIJ_HASLO_SMS));
						watek.start();
					}
		    	  return true;

		      case R.id.usun:
					if(wybranePotwierdzenie != null)
					{
						Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_pobinfo));
						Bankoid.dialog.show();
						
						watek = new Thread(ListaPotwierdzenia.this, String.valueOf(USUN));
						watek.start();
					}
		    	  return true;

		      default:
	        return super.onContextItemSelected(item);
      }
    }
    
	// metoda wywolywana po kliknieciu zatwierdz
    private void zatwierdzKodSMS()
    {
		TextView operacja = (TextView) krok2.findViewById(R.id.operacja);
		TextView opis_operacji = (TextView) krok2.findViewById(R.id.opis_operacji);
		EditText haslo_sms = (EditText) krok2.findViewById(R.id.haslo_sms);
    	
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/transfer_exec.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "1");
	    request.addParam("TransactionType", TransactionType);
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("tbOperationType", operacja.getText().toString());
	    request.addParam("tbOperationDesc", opis_operacji.getText().toString());
	    request.addParam("__EVENTVALIDATION", Bankoid.eventvalidation);
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
   	    		// pobranie nowej odswiezonej listy potwierdzen
   	    		noweDane = MenuGlowne.wybierzPotwierdzenia();
   	    		MenuGlowne.odswiez = true;
   	    		Bankoid.bledy.ustawBlad(m.group(1).trim(), m.group(2).trim(), Bledy.OK);
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
	   	    		// zaladuj ponownie formularz umozliwiajac wpisanie poprawnego hasla
	   	    		// jezeli nie udalo sie zaladowac ponownie formularza to zamknij liste potwierdzanie
	   	    		if(ponowHaslo(rezultat) == false) Bankoid.bledy.ustawKodBledu(Bledy.ZAMKNIJ_OKNO);
	   	    	}
	   	    	else
	   	    	{
	   	    		// niestandartowy blad
	   	    		Bankoid.bledy.ustawBlad(R.string.przelewy_blad, Bledy.WYLOGUJ);
	   	    	}
   	    	}
   	    }
    }
    
    // laduje ponownie formularz z krokiem 2 umozliwiajac ponowienie hasla
    private boolean ponowHaslo(String dane)
    {
    	Matcher m = Przelewy.powrotREGEX.matcher(dane);
    	if(m.find()) paramPowrot = m.group(1);

		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/suspended_transaction_handling.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", paramPowrot);
	    request.addParam("__STATE", Bankoid.state);
	    request.execute();

	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
		// szukanie parameters zatwierdz
		m = Przelewy.zatwierdzREGEX.matcher(rezultat);
	    // jezeli zatwierdz istnieje to udalo sie przejsc do kroku 2 zwroc true
		if(m.find())
	    {
			paramZatwierdz = m.group(1);
			return true;
	    }
	    else return false;
    }

    private void usuwaniePotwierdzenia()
	{
		sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/suspended_transaction_handling.aspx");
	    request.setMethod("POST");
	    request.addParam("authTurnOff", authTurnOff);
	    request.addParam("__PARAMETERS", paramZatwierdz);
	    request.addParam("__CurrentWizardStep", "1");
	    request.addParam("TransactionType", TransactionType);
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.addParam("tbOperationType", tbOperationType);
	    request.addParam("tbOperationDesc", tbOperationDesc);
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);

	    // jezeli operacja nie powiodla sie
	    if(! rezultat.contains("Operacja wykonana poprawnie"))
	    {
	   	    // czy wystapil standartowy blad
	   	    if(Bankoid.bledy.czyBlad() == false)
	   	    {
	   	    	// czy jest komunikat z bledem
	   	    	Matcher m = Przelewy.bladPrzelewREGEX.matcher(rezultat);
	   	    	if(m.find())
	   	    	{
			    	String bladTresc = Html.fromHtml(m.group(2)).toString();
			    	Bankoid.bledy.ustawBlad(m.group(1).trim(), bladTresc, Bledy.INFO);
			    	Bankoid.bledy.ustawKolorTytul(this.getResources().getColor(R.color.tytul_blad));
	   	    	}
		   	    // niestandartowy blad
		   	    else Bankoid.bledy.ustawBlad(R.string.usuwanie_blad, Bledy.WYLOGUJ);
	   	    }
	    }
	}


	// wybiera formularz z mozliwoscia potwierdzenia chasla
    private String wybierzPotwierdzHaslem()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/suspended_transaction_handling.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", wybranePotwierdzenie.pobierzPotwierdzParam());
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
	    return rezultat; 
    }

    // wybiera formularz z wyslij haslo ponownie
    private String wybierzWyslijPonownieHaslo()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/suspended_transaction_handling.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", wybranePotwierdzenie.pobierzWyslijParam());
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
	    return rezultat; 
    }
    
    // wybiera formularz usun potwierdzenie
    private String wybierzUsunPotwierdzenie()
    {
    	sfRequest request = sfClient.getInstance().createRequest();
	    request.setUrl("https://www.mbank.com.pl/suspended_transaction_handling.aspx");
	    request.setMethod("POST");
	    request.addParam("__PARAMETERS", wybranePotwierdzenie.pobierzUsunParam());
	    request.addParam("__STATE", Bankoid.state);
	    request.addParam("__VIEWSTATE", "");
	    request.execute();
	    
	    String rezultat = request.getResult();
	    Bankoid.pobierzState(rezultat);
	    Bankoid.pobierzEventvalidation(rezultat);
	    
	    return rezultat; 
    }
    
	private class AdapterPotwierdzenia extends ArrayAdapter<Potwierdzenie> {

		private ArrayList<Potwierdzenie> potwierdzenia;
		
		public AdapterPotwierdzenia(Context context, int textViewResourceId, ArrayList<Potwierdzenie> potwierdzenia)
		{
	        super(context, textViewResourceId, potwierdzenia);
	        this.potwierdzenia = potwierdzenia;
		}

		
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	            View v = convertView;
	            if (v == null) {
	                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                v = vi.inflate(R.layout.wiersz_potwierdzenia, null);
	            }
	            Potwierdzenie p = potwierdzenia.get(position);
	            
	            if (p != null) {
                    TextView nr_operacji = (TextView) v.findViewById(R.id.numer_operacji);
                    TextView data_operacji = (TextView) v.findViewById(R.id.data_operacji);
                    TextView operacja = (TextView) v.findViewById(R.id.operacja);
                    TextView opis_operacji = (TextView) v.findViewById(R.id.opis_operacji);
                    
                    if(p.pobierzNumerOperacji() != null) nr_operacji.setText(p.pobierzNumerOperacji());
                    if(p.pobierzDateOperacji() != null) data_operacji.setText(p.pobierzDateOperacji());
                    if(p.pobierzOperacje() != null) operacja.setText(p.pobierzOperacje());
                    if(p.pobierzOpisOperacji() != null) opis_operacji.setText(p.pobierzOpisOperacji());

	            }
	            return v;
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
						
	                	Bankoid.tworzProgressDialog(ListaPotwierdzenia.this, getResources().getString(R.string.dialog_wylogowywanie));
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
	                	
	                	watek = new Thread(ListaPotwierdzenia.this, String.valueOf(Bankoid.ACTIVITY_WYLOGOWYWANIE));
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
