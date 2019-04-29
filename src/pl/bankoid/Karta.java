package pl.bankoid;

public class Karta {
	private String typ = null;
	private String posiadacz = null;
	private String status = null;
	private String rodzaj = null;
	private String separator = null;
	private String rozszerzenie = null;
	private String limit = null;
	private String dostepny_limit = null;
	private String parameters = null;

	
	public void ustawTyp(String typ)
	{
		if(typ != null && typ.length() > 0) this.typ = typ.replace("&amp;", " ").toString();
		else this.typ = null;
	}
	
	public void ustawParameters(String id)
	{
		this.parameters = id;
	}
	
	public void ustawPosiadacza(String posiadacz)
	{
		if(posiadacz != null && posiadacz.length() > 0) this.posiadacz = posiadacz;
		else this.posiadacz = null;
	}
	
	public void ustawStatus(String status)
	{
		if(status != null && status.length() > 0) this.status = status;
		else this.status = null;
	}
	
	public void ustawRodzaj(String rodzaj)
	{
		if(rodzaj != null && rodzaj.length() > 0) this.rodzaj = rodzaj.replace("&amp;", " ").toString();
		else this.rodzaj = null;
	}
	
	public void ustawSeparator(String rodzaj)
	{
		if(rodzaj != null && rodzaj.length() > 0) this.separator = rodzaj;
		else this.separator = null;
	}
	
	public void ustawRozszerzenie(String rozszerzenie)
	{
		if(rozszerzenie != null && rozszerzenie.length() > 0) this.rozszerzenie = rozszerzenie;
		else this.rozszerzenie = null;
	}
	
	public void ustawLimit(String limit)
	{
		if(limit != null && limit.length() > 0) this.limit = limit;
	}
	
	public void ustawDostepnyLimit(String limit)
	{
		if(limit != null && limit.length() > 0) this.dostepny_limit = limit;
	}
	
	public String pobierzTyp()
	{
		return typ;
	}
	
	public String pobierzParameters()
	{
		return parameters;
	}
	
	public String pobierzPosiadacza()
	{
		return posiadacz;
	}
	
	public String pobierzStatus()
	{
		return status;
	}

	public String pobierzRodzaj()
	{
		return rodzaj;
	}
	
	public String pobierzSeparator()
	{
		return separator;
	}
	
	public String pobierzRozszerzenie()
	{
		return rozszerzenie;
	}
	
	public String pobierzLimit()
	{
		return limit;
	}
	
	public String pobierzDostepnyLimit()
	{
		return dostepny_limit;
	}
}
