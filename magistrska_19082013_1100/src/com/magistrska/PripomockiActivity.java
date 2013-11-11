package com.magistrska;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class PripomockiActivity extends Activity implements OnClickListener
{
  	private Camera camera = null;
  	Parameters p;
  	
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pripomocki);

        View luckaBtn = findViewById(R.id.button1);
        luckaBtn.setOnClickListener(this);
        
        View kameraBtn = findViewById(R.id.button2);
        kameraBtn.setOnClickListener(this);
        
        View radarBtn = findViewById(R.id.button3);
        radarBtn.setOnClickListener(this);
        
        View callButton = findViewById(R.id.button4);
        callButton.setOnClickListener(this);     
    }
    
    public void onClick(View v) 
    {
    	switch(v.getId())
    	{
    		case R.id.button1:
    			lucka();
    			break;
    		case R.id.button2:
    			kamera();
    			break;
    		case R.id.button3:
    			radar();
    			break;
    		case R.id.button4:
    			phoneCall();
    			break;
    		default:
    			break;
    	}
    }
    
    public void lucka()
    {
    	if(camera == null)
    	{
    		camera = Camera.open(); //primerek razreda Camera za kontrolo nad vgrajeno kamero
    		p = camera.getParameters(); //najprej preberemo že nastavljene parametre
    		p.setFlashMode(Parameters.FLASH_MODE_TORCH); //spremenimo nastavitev bliskavice
    		camera.setParameters(p); //shranimo spremenjene parametre
    		Toast.makeText(getApplicationContext(),"luèka sveti...",Toast.LENGTH_LONG).show(); //prikaz obvestila
    	}
    	else
    	{
    		p.setFlashMode(Parameters.FLASH_MODE_OFF); //spremenimo parameter za bliskavico
    		camera.setParameters(p); //shranimo spremenjene parametre
    		camera.release(); //sprostimo dostop do nadzora nad kamero
    		camera = null; //odstranimo referenco
    		Toast.makeText(getApplicationContext(),"luèka ugasnjena!",Toast.LENGTH_LONG).show(); //prikazemo obvestilo
    	}
     }
    
    private void kamera()
    {
    	Intent i = new Intent("android.media.action.IMAGE_CAPTURE"); //dostop do privzete aplikacije za zajem fotografij
    	startActivity(i); //zagon aplikacije
    }
    
    public void radar()
    {
    	Intent intent = new Intent(); //posrednik med našo aplikacijo in spletnim brskalnikom

    	Uri uri = Uri.parse("http://www.arso.gov.si/vreme/napovedi%20in%20podatki/radar.gif"); //spletna povezava do radarske slike 

    	intent.setData(uri); //posredniku doloèimo cilj povezave v spletnem brskalniku
    	intent.setAction(android.content.Intent.ACTION_VIEW); //doloèimo akcijo, ki jo naj izvede

    	startActivity(intent); //in akcijo poženemo
    }
    
    private void phoneCall()
    {
    	String phoneCallUri = "tel:112"; //doloèimo telefonsko številko, ki jo želimo poklicati
    	Intent phoneCallIntent = new Intent(Intent.ACTION_CALL); //posredniku doloèimo akcijo
    	phoneCallIntent.setData(Uri.parse(phoneCallUri)); //nastavimo vse potrebne parametre
    	startActivity(phoneCallIntent); //zagon akcije
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) //funkcija, s katero doloèimo vsebino menija izbir
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pripomocki, menu);
        return true;
    }
    
	public boolean onOptionsItemSelected(MenuItem item) //funkcija za klic ostalih aktivnosti v naši aplikaciji
	{
		switch (item.getItemId()) 
		{
		    case R.id.tocka:
		    	startActivity(new Intent(this, TockaActivity.class));
		    	return true;
		    case R.id.osm:
		    	startActivity(new Intent(this, ZemljevidActivity.class));
		    	return true;
		    case R.id.vrhovi:
		    	startActivity(new Intent(this, VrhoviActivity.class));
		    	return true;
		    default:
		    return super.onOptionsItemSelected(item);
		}
	}
}
