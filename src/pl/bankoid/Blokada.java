package pl.bankoid;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;


public class Blokada {

	private String data_rejestracji = null;
	private String data_zakonczenia = null;
	private String opis_blokady = null;
	private String kwota = null;
	private String typ_blokady = null;
	

	public void ustawDateRejestracji(String data_rejestracji)
	{
		this.data_rejestracji = data_rejestracji;
	}
	
	public void ustawDateZakonczenia(String data_zakonczenia)
	{
		this.data_zakonczenia = data_zakonczenia;
	}
	
	public void ustawOpisBlokady(String opis)
	{
		this.opis_blokady = opis;
	}
	
	public void ustawKwoteOperacji(String kwota)
	{
		this.kwota = kwota;
	}
	
	public void ustawTypBlokady(String typ_blokady)
	{
		this.typ_blokady = typ_blokady;
	}
	
	// POBIERZ
	
	public String pobierzDateRejestracji()
	{
		return this.data_rejestracji;
	}
	
	public String pobierzDateZakonczenia()
	{
		return this.data_zakonczenia;
	}
	
	public Spannable pobierzOpisBlokady()
	{
	    SpannableString content = new SpannableString(opis_blokady);
	    content.setSpan(new UnderlineSpan(), 0, content.length(), 0); 
		return new SpannableString(content);
	}
	
	
	public String pobierzKwoteOperacji()
	{
		return this.kwota;
	}
	
	public String pobierzTypBlokady()
	{
		return typ_blokady;
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
