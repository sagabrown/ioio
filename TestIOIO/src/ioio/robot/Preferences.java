package ioio.robot;

import android.preference.PreferenceActivity;
import android.util.Log;
import android.content.SharedPreferences;
import android.os.Bundle;

public class Preferences extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	    
	    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
	    updateSummary(sharedPreferences, getString(R.string.pref_ears));
	    updateSummary(sharedPreferences, getString(R.string.pref_distPerCycle));
	    updateSummary(sharedPreferences, getString(R.string.pref_numOfSlit));
	}

      
    @Override  
    protected void onResume() {  
        super.onResume();  
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);  
    }  
       
    @Override  
    protected void onPause() {  
        super.onPause();  
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);  
    }  
      
    // Ç±Ç±Ç≈ summary ÇìÆìIÇ…ïœçX  
    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {  
    	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    		String value;
    		try{
    			value = sharedPreferences.getString(key, "--");
    		}catch(ClassCastException e){
    			value = "";
    		}
           	findPreference(key).setSummary(value);
           	Log.i("Pref", key);
        }
    };  
    
    public void updateSummary(SharedPreferences sharedPreferences, String key){
    	findPreference(key).setSummary(sharedPreferences.getString(key, "--"));
    	Log.i("Pref", key);
    }

}