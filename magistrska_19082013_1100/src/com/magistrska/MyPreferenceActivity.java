package com.magistrska;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MyPreferenceActivity extends PreferenceActivity
{
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.nastavitve); //funkcija za prikaz elementov menija
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) //funkcija za priklic izgleda in vsebine menija znotraj aktivnosti
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nastavitve, menu);
        return true;
    }
    
	public boolean onOptionsItemSelected(MenuItem item) //priklic ustreznih akcij
	{
		switch (item.getItemId()) 
		{
		    case R.id.tocka:
		    	startActivity(new Intent(this, TockaActivity.class));
		    	return true;
		    case R.id.osm:
		    	startActivity(new Intent(this, ZemljevidActivity.class));
		    	return true;
		    case R.id.pripomocki:
		    	startActivity(new Intent(this, PripomockiActivity.class));
		    	return true;
		    case R.id.vrhovi:
		    	startActivity(new Intent(this, VrhoviActivity.class));
		    	return true;
		    default:
		    return super.onOptionsItemSelected(item);
		}
	}
}