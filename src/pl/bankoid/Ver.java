package pl.bankoid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;

public class Ver {

		public static boolean nowa_wersja = false;
		public static String wersja;
	
		public static String pobierzID(Context context)
		{
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String id = tm.getDeviceId();
			if(id == null) return "";
			else return id;
		}
		
		public static String pobierzSimID(Context context)
		{
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String id = tm.getSimSerialNumber();
			if(id == null) return "";
			else return id;
		}
		
		public static String encrypt(String seed, String cleartext) throws Exception {
			byte[] rawKey = getRawKey(seed.getBytes());
			byte[] result = encrypt(rawKey, cleartext.getBytes());
			String hex = toHex(result);
			return Base64.encodeBytes(hex.getBytes());
		}
		
		public static String decrypt(String seed, String encrypted) throws Exception {
			// base64
			byte[] enc64 = Base64.decode(encrypted);
			String dec64 = new String();
			for (int i = 0; i < enc64.length; i++) {
			    dec64 += (char)enc64[i];
			}
		
			byte[] rawKey = getRawKey(seed.getBytes());
			byte[] enc = toByte(dec64);
			byte[] result = decrypt(rawKey, enc);
			return new String(result);
		}
		
		private static byte[] getRawKey(byte[] seed) throws Exception {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			SecureRandom sr = SecureRandom.getInstance( "SHA1PRNG", "Crypto" );
			sr.setSeed(seed);
		    kgen.init(128, sr); // 192 and 256 bits may not be available
		    SecretKey skey = kgen.generateKey();
		    byte[] raw = skey.getEncoded();
		    return raw;
		}

		
		private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
		    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");
		    cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		    byte[] encrypted = cipher.doFinal(clear);
			return encrypted;
		}
		
		private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
		    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");
		    cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		    byte[] decrypted = cipher.doFinal(encrypted);
			return decrypted;
		}
		
		public static String toHex(String txt) {
			return toHex(txt.getBytes());
		}
		public static String fromHex(String hex) {
			return new String(toByte(hex));
		}
		
		public static byte[] toByte(String hexString) {
			int len = hexString.length()/2;
			byte[] result = new byte[len];
			for (int i = 0; i < len; i++)
				result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
			return result;
		}
		
		public static String toHex(byte[] buf) {
			if (buf == null)
				return "";
			StringBuffer result = new StringBuffer(2*buf.length);
			for (int i = 0; i < buf.length; i++) {
				appendHex(result, buf[i]);
			}
			return result.toString();
		}
		private final static String HEX = "0123456789ABCDEF";
		private static void appendHex(StringBuffer sb, byte b) {
			sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
		}
		
		public static String md5(String dane) {  
		    try {  
		        // Create MD5 Hash  
		        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
		        digest.update(dane.getBytes());
		        byte messageDigest[] = digest.digest();
		 
		        // Create Hex String
		        StringBuffer hexString = new StringBuffer();
		        for (int i = 0; i < messageDigest.length; i++) {
		            String h = Integer.toHexString(0xFF & messageDigest[i]);
		            while (h.length() < 2)
		                h = "0" + h;
		            hexString.append(h);
		        }
		        return hexString.toString();
		 
		    } catch (Exception e)
		    {}
		    return "";
		}
	
		public static String decryptOnline(String dane, String klucz, Context context) throws Exception
		{
			// define keys
			if(klucz == null) klucz = Ver.pobierzID(context) + " ";
			
			String iv = new String("tayuqavEp45aster");
			// base64 decoding
			int i = 0;
			String enc64 = dane;
			byte[] enc64bytes = enc64.getBytes();
			byte[] dec64bytes;
			dec64bytes = Base64.decode(enc64bytes);
			// because the toString method does not work you have to cast each array element separately
			/*String dec64 = new String();
			for (i = 0; i < dec64bytes.length; i++) {
			    dec64 += (char)dec64bytes[i];
			}*/
			// set decrypt params
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			SecretKeySpec keySpec = new SecretKeySpec(klucz.getBytes(), "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
			ByteArrayInputStream fis = new ByteArrayInputStream(dec64bytes);
			CipherInputStream cis = new CipherInputStream(fis, cipher);
			ByteArrayOutputStream fos = new ByteArrayOutputStream();
			// decrypting
			byte[] b = new byte[8];
			while ((i = cis.read(b)) != -1) {
			    fos.write(b, 0, i);
			}
			fos.flush();
			fos.close();
			cis.close();
			fis.close();
			  
			return fos.toString().trim();
		}
		
		public static String pobierzWersje(Context context)
		{
			PackageInfo pInfo;
			String versionInfo = "";
			try {
				pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
				versionInfo = pInfo.versionName;
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return versionInfo;
		}
		
		// czy pokazywac reklamy
		public static boolean pokazReklamy(Context context)
		{
	    	SharedPreferences prefs = context.getSharedPreferences(Ustawienia.PREFS_NAME, 0);
	        SharedPreferences.Editor edytor = prefs.edit();
	        boolean reklamy = true;
			Pattern daneREGEX = Pattern.compile("^([0-9]{0,15})(\\d\\.\\d{2})$");
			
			String kluczPref = prefs.getString("klucz", null);
			
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, 5000); 
			HttpConnectionParams.setSoTimeout(params, 5000); 

			String rezultat = null;
			String dane = null;
			String klucz = (context.getResources().getString(R.string.dialog_logowanie)).substring(0, 16);
			String url = "";
			try {

				url = decryptOnline("pzOyEqUQg4dTANwxB+YEsCcDtFuwJNzQTl3A/zXoedxfjsb65MCsDgGxXw2Agpao", klucz, context);

		    	sfRequest request = sfClient.getInstance().createRequest();
			    request.setUrl(url);
			    request.setMethod("POST");
			    request.addParam("id", Ver.pobierzID(context));
			    request.execute();
			    
			    dane = request.getResult();
				//}while(rezultat.equals("") && rezultat.startsWith("timeout") && licznik++ < 3);
		    
				rezultat = decryptOnline(dane, null, context);
			} catch (Exception e) {
				rezultat = "";
				reklamy = true;
			}
			
			// szukanie wzorca
			Matcher m = daneREGEX.matcher(rezultat);
			if(m.find())
			{
				String imei = m.group(1);
				wersja = m.group(2);
				
				// sprawdzenie czy jest nowa wersja na serwerze
				if(wersja != null && wersja.equals(Ver.pobierzWersje(context)) == false) nowa_wersja = true;
				else nowa_wersja = false;
				
				// jezeli imei sa takie same to nie pokazuj reklam i zachowaj klucz
				if(imei != null && imei.equals(Ver.pobierzID(context)))
				{
					edytor.putString("klucz", dane);
					edytor.commit();
					reklamy = false;
				}
			}
			// jezeli blad polaczenia a klucz jest zapisany
			else if(kluczPref != null)
			{
				try {
					rezultat = decryptOnline(kluczPref, null, context);
				} catch (Exception e) {
					reklamy = true;
				}
				
				m = daneREGEX.matcher(rezultat);
				if(m.find())
				{
					String imei = m.group(1);
					wersja = m.group(2);
					
					// sprawdzenie czy jest nowa wersja na serwerze
					if(wersja != null && wersja.equals(Ver.pobierzWersje(context)) == false) nowa_wersja = true;
					else nowa_wersja = false;
					
					// jezeli imei sa takie same to nie pokazuj reklam i zachowaj klucz
					if(imei != null && imei.equals(Ver.pobierzID(context)))	reklamy = false;
				}
			}
			// jezeli pokazywac reklamy usun ustawienia o kluczu
			if(kluczPref != null && reklamy)
			{
				edytor.remove("klucz");
				edytor.commit();
			}
			
			return reklamy;
		}
}
