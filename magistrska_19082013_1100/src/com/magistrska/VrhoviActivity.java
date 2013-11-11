package com.magistrska;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class VrhoviActivity extends Activity implements OnClickListener
{
	//potrebne spremenljivke za kontrolo nad zemljevidom
	private MapView myOpenMapView;
	private MapController myMapController;
	MyLocationOverlay myLocationOverlay = null;
	ArrayList<OverlayItem> overlayItemArray;
	LocationManager glocManager;
	
	//potrebne spremenljivke za vodenje po poti
	ArrayList<OverlayItem> tocke_pot;
	String[] sb;
	String ime;
	String vrh;
	String seznam;
	double razdalja = 0.0;
	double smer = 0.0;
	TextView t;
	TextView lokacija;
	GeoPoint start;
	GeoPoint cilj;
	double ciljLAT;
	double ciljLON;
	double ciljALT;
	
	//geografske koordinate
	double lat;
	double lon;
	double alt;
	
	//zaokroževanje vrednosti
	String gs;
	String gd;
	String nv;
	DecimalFormat df = new DecimalFormat("#.####");
	
	EditText v;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peaks);
        
        myOpenMapView = (MapView)findViewById(R.id.peaksview); //dostop do strukture za izgled
        lokacija = (TextView) findViewById(R.id.lokacija); //gradnik za izpis lokacije
        
		myOpenMapView.setBuiltInZoomControls(true); //omogoèimo "zoomiranje" zemljevida
	    myMapController = myOpenMapView.getController(); //omogoèimo premikanje zemljevida
	    myOpenMapView.getController().setZoom(8); //privzeta stopnja "zoom-a"
	    myOpenMapView.setTileSource(TileSourceFactory.MAPNIK); //vir za zemljevide
	    
        overlayItemArray = new ArrayList<OverlayItem>(); //dodatna plast nad zemljevidi za prikaz lokacije
        
        DefaultResourceProxyImpl defaultResourceProxyImpl = new DefaultResourceProxyImpl(this);
        MyItemizedIconOverlay myItemizedIconOverlay = 
        		new MyItemizedIconOverlay(overlayItemArray, null, defaultResourceProxyImpl); //klic razreda za doloèanje informacij na plasti za prikaz lokacije
        myOpenMapView.getOverlays().add(myItemizedIconOverlay);

        glocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); //kontrola nad senzorjem za pridobivanje lokacije       
        Location lastLocation = glocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); //ponudnik za doloèanje lokacije
        
        if(lastLocation != null) //klic funkcije za posodobitev trenutne lokacije
        {
        	updateLoc(lastLocation);
        }
        
        //dodajanje skale na gradnik za prikaz zemljevida
        ScaleBarOverlay myScaleBarOverlay = new ScaleBarOverlay(this);
        myOpenMapView.getOverlays().add(myScaleBarOverlay);
        
        //plast za prikaz trenutne lokacije
        myLocationOverlay = new MyLocationOverlay(this, myOpenMapView);
        myOpenMapView.getOverlays().add(myLocationOverlay);
        myOpenMapView.postInvalidate();
        
        View shraniButton = findViewById(R.id.btn_shraniVrh);
        shraniButton.setOnClickListener(this);
    }
    
    private void updateLoc(Location loc)
    {
    	lat = loc.getLatitude(); //geografske koordinate in nadmorska višina iz sistema GPS
    	lon = loc.getLongitude();
    	alt = loc.getAltitude();
    	
    	GeoPoint locGeoPoint = new GeoPoint(lat, lon); //trenutna lokacija
    	myMapController.setCenter(locGeoPoint); //na katero centriramo prikaz zemljevida

    	lokacija.setText("lokacija: " + String.valueOf(df.format(lat)) + 
    					 ", " + String.valueOf(df.format(lon))); //izpis trenutne lokacije
    	
    	myOpenMapView.invalidate(); //ob vsaki spremembi trenutne lokacije, posodobimo prikaz na zemljevidu
    	
    	preberiTocke(); //branje shranjenih toèk za vodenje po izbrani poti
    }
    
    private LocationListener gpsLocationListener = new LocationListener() //"listener" za spreminjanje trenutne lokacije
    {
    	@Override
    	public void onLocationChanged(Location location) 
    	{
    		updateLoc(location);
    	}

    	@Override
    	public void onProviderDisabled(String provider) 
    	{
    	}

    	@Override
    	public void onProviderEnabled(String provider) 
    	{
    	}

    	@Override
    	public void onStatusChanged(String provider, int status, Bundle extras) 
    	{
    	}
    };

    private class MyItemizedIconOverlay extends ItemizedIconOverlay<OverlayItem> //razred za prikaz informacij na plasti za prikaz trenutne lokacije
    {
    	public MyItemizedIconOverlay(
    			List<OverlayItem> pList,
    			org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener<OverlayItem> pOnItemGestureListener,
    			ResourceProxy pResourceProxy) 
    	{
    		super(pList, pOnItemGestureListener, pResourceProxy);
    	}

    	@Override
    	public void draw(Canvas canvas, MapView mapview, boolean arg2) 
    	{
    		super.draw(canvas, mapview, arg2);
   
    		if(!overlayItemArray.isEmpty())
    		{
    			//overlayItemArray have only ONE element only, so I hard code to get(0)
    			GeoPoint in = overlayItemArray.get(0).getPoint();
    
    			Point out = new Point();
    			mapview.getProjection().toPixels(in, out);
    			
    			Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.pushpin);
    			canvas.drawBitmap(bm, 
    					out.x - bm.getWidth()/2,  //shift the bitmap center
    					out.y - bm.getHeight()/2,  //shift the bitmap center
    					null);
    		}
    	}

    	@Override
    	public boolean onSingleTapUp(MotionEvent event, MapView mapView) 
    	{
    		if(event.getAction() == 1)
    		{
    			GeoPoint p = (GeoPoint) myOpenMapView.getProjection().fromPixels((int)event.getX(), (int)event.getY());
    			
    			Toast.makeText(getApplicationContext(), "lokacija: " + p.getLatitudeE6()/1E6 + ", " 
    																+ p.getLongitudeE6()/1E6 +
    																+ p.getAltitude(), Toast.LENGTH_LONG).show();
    		}
    		return false;
    	}
    }

    private void preberiTocke()
    {
    	String mapa = Environment.getExternalStorageDirectory().toString() + "/planinec/"; //mapa s podatki
    	String f = "vrhovi.txt"; //seznam toèk
    	File f2 = new File(mapa,f);
    	
    	tocke_pot = new ArrayList<OverlayItem>(); //za toèke ustvarimo dodatno plast nad zemljevidom
    	
		try 
    	{
			BufferedReader br = new BufferedReader(new FileReader(f2));
			String vrstica;

			while(br.readLine() != null) //podatki za posamezno toèko so zapisani vsak v svoji vrstici
			{
				vrstica = br.readLine(); //preberemo vrstico
				sb = vrstica.split("[\\s]"); //jo razbijemo na posamezne vrednosti
				ime = sb[0]; //vsaka vrstica se zaène z imenom vrha-toèke
				
				ciljLAT = Double.parseDouble(sb[1]); //geografska širina toèke
				ciljLON = Double.parseDouble(sb[2]); //geografska dolžina toèke
				ciljALT = Double.parseDouble(sb[3]); //nadmorska višina toèke
				
				cilj = new GeoPoint(ciljLAT,ciljLON); //toèka, ki jo prikažemo na zemljevidu
				tocke_pot.add(new OverlayItem("tocka", sb[0], cilj)); //ustvarjeno toèko dodamo na seznam za prikaz shranjenih vrhov
			}
			br.close();
			
			/*t = (TextView)findViewById(R.id.vrhovi);
			t.setText(seznam); */
			
			ItemizedIconOverlay<OverlayItem> anotherItemizedIconOverlay = 
					new ItemizedIconOverlay<OverlayItem>(this, tocke_pot, null);
	        myOpenMapView.getOverlays().add(anotherItemizedIconOverlay); //ustvarimo novo plast za prikaz shranjenih toèk
	        myOpenMapView.invalidate(); //posodobimo prikaz na zemljevidu
		} 
    	catch (IOException e) //izjema, ki jo skušamo ujeti èe pride do napake pri branju iz datoteke
		{
    		e.printStackTrace();
 		   Toast.makeText(getApplicationContext(), "napaka pri branju " + e, Toast.LENGTH_SHORT).show();
		}
    }
    
    private void shraniVrh()
    {
    	String mapa = Environment.getExternalStorageDirectory().toString() + "/planinec/"; //mapa s podatki
    	String f = "vrhovi.txt"; //seznam toèk
    	File f2 = new File(mapa,f);
    	
    	if (!f2.exists())
    	{
    		try
    		{
    			f2.createNewFile(); //èe datoteka še ne obstaja, jo ustvarimo
    		} 
    		catch (IOException e)
    		{
    			e.printStackTrace();
    		}
	   }
	   try
	   {
		   //zapišemo toèko v datoteko - vsako toèko v svojo vrstico
		   BufferedWriter buf = new BufferedWriter(new FileWriter(f2, true));
		   
		   v = (EditText) findViewById(R.id.editText1); //gradnik za vnos imena toèke
		   ime = v.getText().toString() + " " + lat + " " + lon + " " + alt; //podatek za shranjevanje
		   buf.append("\n" + ime); //zaradi preglednosti in branja iz datoteke moramo vsakiè najprej skoèiti v novo vrstico

		   buf.close(); //ko konèamo z zapisovanjem, moramo prekiniti dostop do datoteke
		   
		   Toast.makeText(getApplicationContext(), "shranjevanje uspešno!", Toast.LENGTH_LONG).show(); //prikaz obvestila o uspešnosti
		   
	   }
	   catch (IOException e)
	   {
		   e.printStackTrace();
		   Toast.makeText(getApplicationContext(), "napaka pri zapisovanju " + e, Toast.LENGTH_SHORT).show(); //èe se zgodi napaka pri zapisovanju, jo izpišemo
	   }
	   v.setText("");
    }
    
    public void onClick(View v) //klik na gumb v aplikaciji
    {
    	switch(v.getId())
    	{
    		case R.id.btn_shraniVrh: //uporabnik ima na razpolago zgolj gumb, s katerim poklièe funkcijo shranjevanja toèke
    			shraniVrh();
    			break;
    		default:
    			break;
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) //klic funkcije za doloèanje izgleda glavnega menija aplikacije
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.vrhovi, menu);
        return true;
    }
    
	public boolean onOptionsItemSelected(MenuItem item) //vsebina menija in klic ustreznih funkcij
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
		    default:
		    return super.onOptionsItemSelected(item);
		}
	}
    
    protected void onResume() 
	{
		super.onResume();
		glocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 5000, 0, gpsLocationListener); //vklop zahteve za posodabljanje lokacije
    	myLocationOverlay.enableCompass(); //omogoèimo ustrezno interaktivnost na zemljevidu - kompas, sledenje in prika lokacije
    	myLocationOverlay.enableMyLocation();
    	myLocationOverlay.enableFollowLocation();
    }
	
	@Override
	protected void onPause()
	{
		super.onPause();
		glocManager.removeUpdates(gpsLocationListener); //izklopimo zahtevo za posodabljanje lokacije
    	myLocationOverlay.disableCompass(); //odstranimo interaktivnost na zemljevidu - prikaz in sledenje lokaciji, kompas
    	myLocationOverlay.disableMyLocation();
    	myLocationOverlay.disableFollowLocation();
    }
}
