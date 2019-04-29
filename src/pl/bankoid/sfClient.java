package pl.bankoid;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class sfClient{
	  private static sfClient instance;
	  protected DefaultHttpClient httpClient;
	  /**
	   * constructor
	   */
	  private sfClient(){
		  // Set the timeout in milliseconds until a connection is established. 
		  int timeoutConnection = 15000; 
		  // Set the default socket timeout (SO_TIMEOUT)
		  // in milliseconds which is the timeout for waiting for data. 
		  int timeoutSocket = 15000; 

		  HttpParams httpParameters = new BasicHttpParams();

		  HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		  HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		  HttpProtocolParams.setContentCharset(httpParameters, "UTF-8");
		  ConnManagerParams.setMaxTotalConnections(httpParameters, 20);

		  SchemeRegistry schemeRegistry = new SchemeRegistry();
		  schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		  schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

		  final ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParameters, schemeRegistry); 

		  this.httpClient = new DefaultHttpClient(cm, httpParameters); 
		  final HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(3, true);
		  httpClient.setHttpRequestRetryHandler(retryHandler);

	  }
	  /**
	   * instance accessor
	   */
	  public static sfClient getInstance(){
	    if(null == instance){
	      instance = new sfClient();
	    }
	    return instance;
	  }
	  /**
	   * creating new request
	   */
	  public sfRequest createRequest(){
	    sfRequest request = new sfRequest(this.httpClient);
	    return request;
	  }

	}