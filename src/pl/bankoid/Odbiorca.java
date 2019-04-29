package pl.bankoid;

public class Odbiorca {

	private String nazwa = null;
	private String nazwisko = null;
	private String nr_ivr = null;
	private String nazwa_ivr = null;
	private String parameters = null;
	
	public void ustawNazwe(String nazwa)
	{
		if(nazwa != null && nazwa.length() > 0) this.nazwa = nazwa;
		else this.nazwa = null;
	}
	
	public void ustawParameters(String id)
	{
		this.parameters = id;
	}
	
	public void ustawNazwisko(String nazwisko)
	{
		if(nazwisko != null && nazwisko.length() > 0) this.nazwisko = nazwisko;
		else this.nazwisko = null;
	}
	
	public void ustawNR(String nr)
	{
		if(nr != null && nr.length() > 0) this.nr_ivr = nr;
		else this.nr_ivr = null;
	}

	public void ustawNazweIVR(String nazwa)
	{
		if(nazwa != null && nazwa.length() > 0) this.nazwa_ivr = nazwa;
		else this.nazwa_ivr = null;
	}
	
	public String pobierzNazwe()
	{
		return nazwa;
	}
	
	public String pobierzParameters()
	{
		return parameters;
	}
	
	public String pobierzNazwisko()
	{
		return nazwisko;
	}
	
	public String pobierzNR()
	{
		return nr_ivr;
	}
	
	public String pobierzNazweIVR()
	{
		return nazwa_ivr;
	}

}
