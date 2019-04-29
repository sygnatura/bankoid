package pl.bankoid;

public class Potwierdzenie {

	private String numer_operacji = null;
	private String data_operacji = null;
	private String operacja = null;
	private String opis_operacji = null;
	private String potwierdzParam = null;
	private String wyslijParam = null;
	private String usunParam = null;

	public void ustawDateOperacji(String data_operacji)
	{
		this.data_operacji = data_operacji;
	}
	
	public void ustawNumerOperacji(String nr)
	{
		this.numer_operacji = nr;
	}
	
	public void ustawOperacje(String operacja)
	{
		this.operacja = operacja;
	}
	
	public void ustawOpisOperacji(String opis)
	{
		this.opis_operacji = opis;
	}
	
	public void ustawPotwierdzParam(String param)
	{
		this.potwierdzParam = param;
	}
	
	public void ustawWyslijParam(String param)
	{
		this.wyslijParam = param;
	}
	
	public void ustawUsunParam(String param)
	{
		this.usunParam = param;
	}

	// POBIERZ
	
	public String pobierzDateOperacji()
	{
		return this.data_operacji;
	}
	
	public String pobierzNumerOperacji()
	{
		return this.numer_operacji;
	}
	
	public String pobierzOperacje()
	{
		return this.operacja;
	}
	
	public String pobierzOpisOperacji()
	{
		return this.opis_operacji;
	}
	
	public String pobierzPotwierdzParam()
	{
		return this.potwierdzParam;
	}
	
	public String pobierzWyslijParam()
	{
		return this.wyslijParam;
	}
	
	public String pobierzUsunParam()
	{
		return this.usunParam;
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
