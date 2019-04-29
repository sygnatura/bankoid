package pl.bankoid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.EditText;


public class SMSReceiver extends BroadcastReceiver {
	
	public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	Pattern daneHTML = Pattern.compile("operacji (\\d+) z dnia <nobr>(\\d{4})-(\\d{2})-(\\d{2})</nobr>");
	Pattern daneSMS = Pattern.compile("nr (\\d+) z dn. (\\d{2})-(\\d{2})-(\\d{4}).+?has.o: (\\d+) mBank.");
	EditText pole_haslo;
	String operacja, dzien, miesiac, rok;
	
	public SMSReceiver(String dane, EditText pole)
	{
		this.pole_haslo = pole;
		Matcher m = daneHTML.matcher(dane);
		if(m.find())
		{
			operacja = m.group(1);
			rok = m.group(2);
			miesiac = m.group(3);
			dzien = m.group(4);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
        Bundle bundleObj = intent.getExtras();        
        SmsMessage[] msgs = null;
        
        if (bundleObj != null)
        {
            Object[] pdus = (Object[]) bundleObj.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            msgs[0] = SmsMessage.createFromPdu((byte[])pdus[0]);
            
            if (msgs[0].getOriginatingAddress().compareTo("3388") == 0)
            {
            	Matcher m = daneSMS.matcher(msgs[0].getMessageBody().toString());
            	if(m.find() && operacja != null && dzien != null && miesiac != null && rok != null)
            	{
            		// jezeli to wlasciwy sms z dana operacja to wstaw haslo
            		if(m.group(1).equals(operacja) && m.group(2).equals(dzien) && m.group(3).equals(miesiac) && m.group(4).equals(rok))
            		try
            		{
            			pole_haslo.setText(m.group(5));
            		}catch(Exception e) {}
            	}
            	
            }
        }
	}

}