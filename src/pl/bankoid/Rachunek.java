package pl.bankoid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rachunek {

	private String nazwa = null;
	private String parameters = null;
	private String saldo = null;
	private String srodki = null;
	
	Pattern nazwaREGEX = Pattern.compile("^(\\D+)([0-9 ]+)");
	
	public void ustawNazwe(String nazwa)
	{
		Matcher m = nazwaREGEX.matcher(nazwa);
		if(m.find()) this.nazwa = m.group(1).trim() + "\n" + m.group(2);
		else this.nazwa = nazwa;
	}
	
	public void ustawParameters(String id)
	{
		this.parameters = id;
	}
	
	public void ustawSaldo(String saldo)
	{
		this.saldo = saldo;
	}
	
	public void ustawSrodki(String srodki)
	{
		this.srodki = srodki;
	}
	
	public String pobierzNazwe()
	{
		return nazwa;
	}
	
	public String pobierzSkroconaNazwe()
	{
		Matcher m = nazwaREGEX.matcher(nazwa);
		if(m.find()) return m.group(1).trim();
		else return nazwa;
	}
	
	public String pobierzParameters()
	{
		return parameters;
	}
	
	public String pobierzSaldo()
	{
		return saldo;
	}
	
	public String pobierzSrodki()
	{
		return srodki;
	}
}
