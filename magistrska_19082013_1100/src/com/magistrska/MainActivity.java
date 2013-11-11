package com.magistrska;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity 
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); //doloèimo izgled aktivnosti
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) //funkcija za obliko in vsebino glavnega menija
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) //funkcija za priklic izbrane aktivnosti
	{
		switch (item.getItemId()) 
		{
			case R.id.nastavitve:
				startActivity(new Intent(this, MyPreferenceActivity.class));
				return true;
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
