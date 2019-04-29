package pl.bankoid;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;


public class Ustawienia extends PreferenceActivity {

	 public static final String PREFS_NAME = "pref";
	 
	 @Override
     protected void onCreate(Bundle savedInstanceState)
	 {
             super.onCreate(savedInstanceState);
             getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
             addPreferencesFromResource(R.xml.ustawienia);

             Preference pref_sms = (Preference) findPreference("pref_sms");
           	 pref_sms.setEnabled(true);
           	 pref_sms.setSummary("");
     }
	 

}
