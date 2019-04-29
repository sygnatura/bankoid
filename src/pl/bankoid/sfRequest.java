package pl.bankoid;

//IO
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import android.graphics.drawable.Drawable;
import android.os.Environment;


public class sfRequest{
  protected String url;
  protected String method;
  protected List<NameValuePair> params;
  protected HttpGet getRequest;
  protected HttpPost postRequest;
  protected HttpResponse response;
  protected String result;
  protected DefaultHttpClient httpClient;
  protected ArrayList<Header> naglowki;
 
  /**
   * constructor
   */
  public sfRequest(DefaultHttpClient client){
    this.httpClient = client;
    this.params = new ArrayList<NameValuePair>(2);
    this.naglowki = new ArrayList<Header>();
  }
  public void setUrl(String url){
    this.url = url;
  }
  public String getUrl(){
    return this.url;
  }
  public void setMethod(String method){
    this.method = method;
  }
  public String getMethod(){
    return this.method;
  }
  /**
   * Adding parameter
   * @param key
   * @param value
   */
  public void addParam(String key, String value){
    this.params.add(new BasicNameValuePair(key, value));
    
  }
  
  public void dodajNaglowek(String klucz, String wartosc)
  {
	  naglowki.add(new BasicHeader(klucz, wartosc));
  }

  public void czyscNaglowki()
  {
	  naglowki.clear();
  }
  
  public Header[] pobierzNaglowki()
  {
	  int ilosc = naglowki.size();
	  if(ilosc > 0)
	  {
		  Header[] temp = new Header[ilosc];
		  
		  for(int i = 0; i <ilosc; i++)
		  {
			  temp[i] = naglowki.get(i);
		  }
		  return temp;
	  }
	  return null;
		  
  }
  /**
   * getting response text
   * @return String response
   */
  public String getResult(){
	  if(this.result == null) return "";
	  else return this.result;
  }
  
  public String getResult(String encoder)
  {
	  try {
		return URLDecoder.decode(result, encoder);
	} catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return result;
  }
  /**
   * executing request
   */
  public void execute(String dane)
  {
    try {
        if(this.method.compareToIgnoreCase("GET") == 0)
        {
        	this.getRequest = new HttpGet(this.getUrl());
        	// zastepuje naglowki zdefiniowanymi
        	if(naglowki.size() > 0) this.getRequest.setHeaders(this.pobierzNaglowki());
        	this.httpClient.removeRequestInterceptorByClass(org.apache.http.protocol.RequestExpectContinue.class);
        	this.getRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; pl; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13");
        	this.getRequest.addHeader("Accept-Encoding", "gzip,deflate");
        	
        	this.response = this.httpClient.execute(this.getRequest);
      }else if(this.method.compareToIgnoreCase("POST") == 0 || this.method.compareToIgnoreCase("PUT") == 0 || this.method.compareToIgnoreCase("DELETE") == 0)
      {
        this.postRequest = new HttpPost(this.getUrl());
        // zastepuje naglowki zdefiniowanymi
        if(naglowki.size() > 0) this.postRequest.setHeaders(this.pobierzNaglowki());
        // usuniecie naglowka Expect
        this.httpClient.removeRequestInterceptorByClass(org.apache.http.protocol.RequestExpectContinue.class);
        this.postRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; pl; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13");
        this.postRequest.addHeader("Accept-Encoding", "gzip,deflate");

        if(dane != null)
        {
        	StringEntity post = new StringEntity(dane);
        	this.postRequest.setEntity(post);
        }
        else this.postRequest.setEntity(new UrlEncodedFormEntity(this.params, "iso-8859-2"));
        this.response = this.httpClient.execute(this.postRequest);
      }

        HttpEntity entity = response.getEntity();

      
      if(entity != null)
      {
        InputStream inputStream = entity.getContent();

        // sprawdzenie czy zawartosc jest skompresowana
        Header contentEncoding = response.getFirstHeader("Content-Encoding");
        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) { 
        	inputStream = new GZIPInputStream(inputStream); 
        } 
		else if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("deflate")) {
		 	//Log.v("deflate", "deflate");
		  	inputStream = new InflaterInputStream(inputStream, new Inflater(true));
		}
        
        this.result = convertStreamToString(inputStream);
      }
    } catch (ClientProtocolException e) {
        this.result = "timeout";

    } catch (IOException e) {
        this.result = "timeout";
    }
	catch (IllegalArgumentException e)
	{
		//this.result = "dns";
		this.result = "IllegalArgumentException:" + e.getMessage();
	}
	catch (IllegalStateException e)
	{
		//this.result = "dns";
		this.result = "IllegalStateException:" + e.getMessage();
	}
	finally
	{
		if(getUrl().startsWith("https://www.mbank.com.pl")) Bankoid.bledy.diagnozujBlad(this.result);
	}
  }
  
  public void execute()
  {
	  execute(null);
  }
  
  
  public String pobierzPlik()
  {
    try {
        if(this.method.compareToIgnoreCase("GET") == 0)
        {
        	this.getRequest = new HttpGet(this.getUrl());
        	// zastepuje naglowki zdefiniowanymi
        	if(naglowki.size() > 0) this.getRequest.setHeaders(this.pobierzNaglowki());
        	this.httpClient.removeRequestInterceptorByClass(org.apache.http.protocol.RequestExpectContinue.class);
        	this.getRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; pl; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13");
        	this.getRequest.addHeader("Accept-Encoding", "gzip,deflate");
        	
        	this.response = this.httpClient.execute(this.getRequest);
      }else if(this.method.compareToIgnoreCase("POST") == 0 || this.method.compareToIgnoreCase("PUT") == 0 || this.method.compareToIgnoreCase("DELETE") == 0)
      {
        this.postRequest = new HttpPost(this.getUrl());
        // zastepuje naglowki zdefiniowanymi
        if(naglowki.size() > 0) this.postRequest.setHeaders(this.pobierzNaglowki());
        // usuniecie naglowka Expect
        this.httpClient.removeRequestInterceptorByClass(org.apache.http.protocol.RequestExpectContinue.class);
        this.postRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; pl; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13");
        this.postRequest.addHeader("Accept-Encoding", "gzip,deflate");

        this.postRequest.setEntity(new UrlEncodedFormEntity(this.params, "iso-8859-2"));
  
        this.response = this.httpClient.execute(this.postRequest);
      }
        
        Header nazwaPliku = response.getFirstHeader("Content-Disposition");
        HttpEntity entity = response.getEntity();
        if(nazwaPliku != null)
        {
            Pattern p = Pattern.compile("filename=(.+)");
            Matcher m = p.matcher(nazwaPliku.getValue());

            if (m.find()) {

    		    String nazwa_pliku = Environment.getExternalStorageDirectory().toString() + "/" +m.group(1);
    		    
    		    if(entity != null)
    		    {
    			    FileOutputStream fos = new FileOutputStream(nazwa_pliku);

    			    entity.writeTo(fos);
    			    
                    fos.close();
                    this.result = nazwa_pliku;
                    return "Plik został zapisany w: " + nazwa_pliku;
    		    }
            }
            return null;
        }
        this.result = convertStreamToString(entity.getContent());

        if(result.contains(Bankoid.context.getString(R.string.blad_pdf))) Bankoid.bledy.ustawBlad(R.string.blad_pdf_tytul, R.string.blad_pdf, Bledy.INFO);
        return null;

    } catch (ClientProtocolException e){
        this.result = "timeout";
        return null;

    } catch (IOException e) {
        this.result = "timeout";
		return null;
    }
	catch (IllegalArgumentException e)
	{
		//this.result = "dns";
		this.result = "IllegalArgumentException:" + e.getMessage();
		return null;
	}
	catch (IllegalStateException e)
	{
		//this.result = "dns";
		this.result = "IllegalStateException:" + e.getMessage();
		return null;
	}
	finally
	{
		if(getUrl().startsWith("https://www.mbank.com.pl")) Bankoid.bledy.diagnozujBlad(this.result);
	}
  }
  /**
   * converting stream reader to string
   * @return String
   */
  private static String convertStreamToString(InputStream is) {
	 
    BufferedReader reader = null;
	try {
		reader = new BufferedReader(new InputStreamReader(is, "iso-8859-2"));
	} catch (UnsupportedEncodingException e1) {
		reader = new BufferedReader(new InputStreamReader(is));
	}
	
    StringBuilder stringBuilder = new StringBuilder();
 
    String line = null;
    try
    {
      while ((line = reader.readLine()) != null)
      {
    	  stringBuilder.append(line + "\n");
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    finally
    {
      try
      {
        is.close();
        reader.close();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
	
    return stringBuilder.toString();
    
  }
  
  public Drawable LoadImageFromWebOperations(String url)  
  {  
	try	
	{  
	
		InputStream is = (InputStream) new URL(url).getContent();  
		Drawable d = Drawable.createFromStream(is, "src name");  
		return d;  
		
	}
	catch (Exception e)
	{  
		//Log.v("Exc=", e.toString());  
		return null;  
	
	}  
	
  }  

}