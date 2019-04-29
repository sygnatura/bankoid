package pl.bankoid;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class Komunikat extends Dialog {

	public Komunikat(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog);

	}
	
	public void ustawTresc(String tresc)
	{
		TextView pole_tresc = (TextView) this.findViewById(R.id.tresc);
		pole_tresc.setMovementMethod(LinkMovementMethod.getInstance());
		pole_tresc.setText(Html.fromHtml(tresc));
	}
	
	public void ustawTresc(int id)
	{
		ustawTresc(getContext().getResources().getString(id));
	}
	
	public void ustawRozmiarTresc(int dip)
	{
		TextView pole_tresc = (TextView) this.findViewById(R.id.tresc);
		pole_tresc.setTextSize(this.convertDensityPixel(dip));
	}

	public void ustawListeZmian(String tresc)
	{
		TextView lista_zmian = (TextView) this.findViewById(R.id.lista_zmian);
		lista_zmian.setMovementMethod(LinkMovementMethod.getInstance());
		lista_zmian.setText(Html.fromHtml(tresc));
		lista_zmian.setVisibility(View.VISIBLE);
	}
	
	public void ustawListeZmian(int id)
	{
		ustawListeZmian(getContext().getResources().getString(id));
	}

	
	public void ustawTytul(String tytul)
	{
		ImageView ikona = (ImageView) this.findViewById(R.id.ikona);
		TextView pole_tytul = (TextView) this.findViewById(R.id.tytul);
		LinearLayout ramka_tytul = (LinearLayout) this.findViewById(R.id.ramka_tytul);
		ramka_tytul.setVisibility(View.VISIBLE);
		ikona.setVisibility(View.VISIBLE);
		
		pole_tytul.setText(Html.fromHtml(tytul));
	}
	
	public void ustawTytul(int id)
	{
		ustawTytul(getContext().getResources().getString(id));
	}
	
	public void ustawKolorTytul(int kolor)
	{
		TextView pole_tytul = (TextView) this.findViewById(R.id.tytul);
		pole_tytul.setTextColor(kolor);
	}
	
	public void ustawKolorTresc(int kolor)
	{
		TextView pole_tresc = (TextView) this.findViewById(R.id.tresc);
		pole_tresc.setTextColor(kolor);
	}
	
	public void ustawIkone(int id)
	{
		ImageView ikona = (ImageView) this.findViewById(R.id.ikona);
		ikona.setImageResource(id);
		ikona.setVisibility(View.VISIBLE);
	}
	

	public void ustawPrzyciskTak(String tekst, View.OnClickListener sluchacz)
	{
		LinearLayout panel_taknie = (LinearLayout) this.findViewById(R.id.panel_taknie);
		panel_taknie.setVisibility(View.VISIBLE);
		Button przycisk_tak = (Button) this.findViewById(R.id.przycisk_tak);
		Button przycisk_nie = (Button) this.findViewById(R.id.przycisk_nie);
		przycisk_tak.setVisibility(View.VISIBLE);
		przycisk_tak.setText(tekst);
		
		// ustawienie domyslnego sluchacza na zamkniecie okna
		if(sluchacz == null)
		{
			sluchacz = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Komunikat.this.dismiss();
				}
			};
		}
		przycisk_tak.setOnClickListener(sluchacz);
		// jezeli przycisk nie jest widoczny to ustaw wagi
		if(przycisk_nie.getVisibility() == View.VISIBLE)
		{
			ustawWage(przycisk_nie);
			ustawWage(przycisk_tak);
		}
	}
	
	public void ustawPrzyciskNie(String tekst, View.OnClickListener sluchacz)
	{
		LinearLayout panel_taknie = (LinearLayout) this.findViewById(R.id.panel_taknie);
		panel_taknie.setVisibility(View.VISIBLE);
		Button przycisk_tak = (Button) this.findViewById(R.id.przycisk_tak);
		Button przycisk_nie = (Button) this.findViewById(R.id.przycisk_nie);
		przycisk_nie.setVisibility(View.VISIBLE);
		przycisk_nie.setText(tekst);
		
		// ustawienie domyslnego sluchacza na zamkniecie okna
		if(sluchacz == null)
		{
			sluchacz = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Komunikat.this.dismiss();
				}
			};
		}
		przycisk_nie.setOnClickListener(sluchacz);
		// jezeli przycisk nie jest widoczny to ustaw wagi
		if(przycisk_tak.getVisibility() == View.VISIBLE)
		{
			ustawWage(przycisk_nie);
			ustawWage(przycisk_tak);
		}
	}
	
	public void ustawLogo(int id)
	{
		ImageView logo = (ImageView) this.findViewById(R.id.logo);
		logo.setImageResource(id);
		logo.setVisibility(View.VISIBLE);
	}
	
	public void ukryjLogo()
	{
		ImageView logo = (ImageView) this.findViewById(R.id.logo);
		logo.setVisibility(View.GONE);
	}
	
	public void ustawWage(Button przycisk)
	{
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, .5f);
		int dip = convertDensityPixel(5);
		params.setMargins(dip, dip, dip, dip);
		przycisk.setLayoutParams(params);
	}
	
	public int convertDensityPixel(int dip) {
		  return (int) (dip * this.getContext().getResources().getDisplayMetrics().density);
	} 
}
