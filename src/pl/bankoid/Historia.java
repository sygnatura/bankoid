package pl.bankoid;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;


public class Historia {

	private String data_operacji = null;
	private String data_ksiegowania = null;
	private String rodzaj = null;
	private String dane_nadawcy = null;
	private String rachunek_nadawcy = null;
	private String tytul_przelewu = null;
	private String kwota = null;
	private String saldo = null;
	

	public void ustawDateOperacji(String data_operacji)
	{
		this.data_operacji = data_operacji;
	}
	
	public void ustawDateKsiegowania(String data)
	{
		this.data_ksiegowania = data;
	}
	
	public void ustawRodzajPrzelewu(String rodzaj)
	{
		this.rodzaj = rodzaj;
	}
	
	public void ustawDaneNadawcy(String nadawca)
	{
		if(nadawca != null && nadawca.length() > 1) this.dane_nadawcy = oczyscTekst(nadawca);
	}
	
	public void ustawRachunekNadawcy(String rachunek)
	{
		if(rachunek != null) this.rachunek_nadawcy = oczyscTekst(rachunek);
	}
	
	public void ustawTytulPrzelewu(String tytul)
	{
		if(tytul != null) this.tytul_przelewu = oczyscTekst(tytul);
	}
	
	public void ustawKwoteOperacji(String kwota)
	{
		this.kwota = kwota;
	}
	
	public void ustawSaldoPoOperacji(String saldo)
	{
		this.saldo = saldo;
	}
	
	// POBIERZ
	
	public String pobierzDateOperacji()
	{
		return this.data_operacji;
	}
	
	public String pobierzDateKsiegowania()
	{
		return this.data_ksiegowania;
	}
	
	public Spannable pobierzRodzajPrzelewu()
	{
	    SpannableString content = new SpannableString(rodzaj);
	    content.setSpan(new UnderlineSpan(), 0, content.length(), 0); 
		return new SpannableString(content);
	}
	
	public String pobierzDaneNadawcy()
	{
		return this.dane_nadawcy;
	}
	
	public String pobierzRachunekNadawcy()
	{
		return this.rachunek_nadawcy;
	}
	
	public String pobierzTytulPrzelewu()
	{
		return this.tytul_przelewu;
	}
	
	public String pobierzKwoteOperacji()
	{
		/*if(kwota.contains("-"))
		{
			return "<font color=" + Bankoid.context.getResources().getColor(R.color.wartosc_ujemna) + ">" + this.kwota +"</font>";
		}
		else return this.kwota;*/
		return this.kwota;
	}
	
	public String pobierzSaldoPoOperacji()
	{
		return saldo;
	}
	
	public String oczyscTekst(String dane)
	{
		dane = dane.replaceAll("\\s{2,}", " ");
		dane = dane.replace("&shy;<wbr />", "");
		dane = dane.replace("<span>", "");
		dane = dane.replace("</span>", "");
		return dane;
	}
	
}
