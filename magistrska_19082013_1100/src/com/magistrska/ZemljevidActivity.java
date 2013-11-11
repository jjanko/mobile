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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class ZemljevidActivity extends Activity 
{
	private SensorManager mSensorManager = null;
	
	//spremenljivke za kontrolo nad zemljevidom
	private MapView myOpenMapView;
	private MapController myMapController;
	MyLocationOverlay myLocationOverlay = null;
	LocationManager locationManager;
	List<String> tocke = new ArrayList<String>();
	ArrayList<OverlayItem> overlayItemArray;
	TextView lokacija;
	TextView r;
	
	//geografske koordinate trenutne lokacije
	ArrayList<OverlayItem> tocke_pot;
	double lat;
	double lon;
	double alt;
	DecimalFormat df = new DecimalFormat("#.####");
	
	WindowManager wm;
	
	//osnovni pregled zemljevida
    private int MAP_DEFAULT_ZOOM = 14; //privzeta stopnja pribli�evanja
    private double MAP_DEFAULT_LATITUDE = 46.401261; //privzeta lokacija
    private double MAP_DEFAULT_LONGITUDE = 16.275361;
    
    //spremenljivke za interpolacijo nadrmoske vi�ine
    String f;
	String mapa;
	double[][] visine = new double[10][10];
	double latD;
	double lonD;
	double x, y;
	int mestoVrstica;
	int mestoStolpec;
	String vrstica;
	BufferedReader br;
	String[] nv;
	double[] tmp_vis = new double[36];
	double[] distance = new double[36];
	double visina;
	double dis;
   
	//vodenje po poti
    String[] sb;
    ArrayList<GeoPoint> pot;
    double ciljLAT;
    double ciljLON;
    GeoPoint tmp;
    GeoPoint P;
    GeoPoint Q;
    GeoPoint W;
    GeoPoint T;
    
    //razdalja in smer med trenutno to�ko in to�ko na poti
    double razdalja = 0;
    double razdalja1 = 0;
    float napaka;
    double smer;
     
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osm); //izgled aktivnosti
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //ohrani buden ekran
        
        lokacija = (TextView) findViewById(R.id.lokacija); //gradniki znotraj strukture za izgled aktivnosti
        myOpenMapView = (MapView)findViewById(R.id.openmapview);
        r = (TextView) findViewById(R.id.razdalja);
        
        myOpenMapView.setBuiltInZoomControls(true); //na gradnik za prikaz zemljevida dodamo mo�nost za "zoomiranje"
        //myOpenMapView.setUseDataConnection(false);
        myMapController = myOpenMapView.getController(); //premikanje zemljevida
        myOpenMapView.getController().setZoom(MAP_DEFAULT_ZOOM); //privzeta stopnja pribli�evanja
        myOpenMapView.getController().setCenter(
            new GeoPoint(MAP_DEFAULT_LATITUDE, MAP_DEFAULT_LONGITUDE)); //privzeto obmo�je prikaza
        myOpenMapView.setTileSource(TileSourceFactory.MAPNIK); //vir za prikaz zemljevida
        
        //dodatna plast za prikaz trenutne lokacije
        overlayItemArray = new ArrayList<OverlayItem>();
        
        DefaultResourceProxyImpl defaultResourceProxyImpl = new DefaultResourceProxyImpl(this);
        MyItemizedIconOverlay myItemizedIconOverlay = 
        		new MyItemizedIconOverlay(overlayItemArray, null, defaultResourceProxyImpl);
        myOpenMapView.getOverlays().add(myItemizedIconOverlay); //plast dodamo na zemljevid

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); //nadzornik senzorja za lokacijo   
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); //ponudnik za pridobivanje lokacije
        
        if(lastLocation != null)
        {
        	updateLoc(lastLocation); //klic funkcije za posodabljanje lokacije
        }
        
        //na zemljevid dodamo skalo
        ScaleBarOverlay myScaleBarOverlay = new ScaleBarOverlay(this);
        myOpenMapView.getOverlays().add(myScaleBarOverlay);
        
        //plast za prikaz trenutne lokacije
        myLocationOverlay = new MyLocationOverlay(this, myOpenMapView);
        myOpenMapView.getOverlays().add(myLocationOverlay);
        myOpenMapView.postInvalidate(); //ko dodamo plast na zemljevid, posodobimo prikaz
        
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    }
    
    public void odpriDatoteko(double geos, double geod) //funkcija za odpiranje ustrezne datotek z vi�inami
    {
    	String gs = String.valueOf(geos); //pridobimo geografse koordinate
    	String gd = String.valueOf(geod);
    	String[] s = gs.split("[.]"); //vsako koordiato razbijemo na celi in decimalni del
    	String[] d = gd.split("[.]");
    	
    	if(Double.parseDouble(s[0]) < 10)
    	{
    		f = "n" + s[0] + "e00" + d[0] + ".txt"; //celi del dolo�a ime datoteke, ki jo je potrebno odpreti
    	}
    	f = "n" + s[0] + "e0" + d[0] + ".txt";
    	
        latD = Double.parseDouble(s[1]) / Math.pow(10, s[1].length()); //decimalni del pa dolo�a mesto v datoteki,
        lonD = Double.parseDouble(d[1]) / Math.pow(10, d[1].length()); //kjer pri�nemo brati vrednosti
        
        double tx = latD * 1201; //pomo� pri dolo�anju mesta pri�etka branja v datoteki
        String tmp = String.valueOf(tx);
        String[] ttx = tmp.split("[.]");
        x = tx - Integer.valueOf(ttx[0]);
        
        double ty = lonD * 1201;
        tmp = String.valueOf(ty);
        String[] tty = tmp.split("[.]");
        y = ty - Integer.valueOf(tty[0]);
        
        if(x < 0.5) //indeks za�etne vrstice zaokro�imo navzgor ali navzdol
        {
        	mestoVrstica = 1201 - (int)Math.floor(latD * 1201) - 1;
        }
        else
        {
        	mestoVrstica = 1201 - (int)Math.ceil(latD * 1201) - 1;
        }
        
        if(y < 0.5) //indeks za�etnega stolpca zaokro�imo levo ali desno
        {
        	mestoStolpec = 1201 - (int)Math.floor(lonD * 1201) - 1;
        }
        else
        {
        	mestoStolpec = 1201 - (int)Math.ceil(lonD * 1201) - 1;
        }
        
    	try
    	{
    		File root = Environment.getExternalStorageDirectory(); //mesto branja
    		mapa = root.toString() + "/planinec/"; //mapa za branje
            File f2 = new File(mapa, f); //datoteka za branje

    		br = new BufferedReader(new FileReader(f2));
    		
    		int vr = 0;
    		int st = 0;
    		int stVrstic = 0;
    		
    		for(int i = 0; i <= mestoVrstica+3; i++) //premaknemo se na ustrezno mesto v datoteki
    		{
                vrstica = br.readLine();
                stVrstic++;

                //preberemo samo potrebne vrednosti
                if (stVrstic == mestoVrstica + 3 || stVrstic == mestoVrstica + 2 || stVrstic == mestoVrstica + 1
                     || stVrstic == mestoVrstica || stVrstic == mestoVrstica - 1 || stVrstic == mestoVrstica - 2 || stVrstic == mestoVrstica - 3)
                {
                    nv = vrstica.split("[\\s]");
                    for (int j = mestoStolpec - 3; j <= mestoStolpec + 3; j++)
                    {
                        if (st + 1 == 7)
                        {
                            vr++;
                            st = 0;
                        }
                        visine[vr][st] = Double.parseDouble(nv[j]); //in z njimi napolnimo za�asno matriko vi�in
                        st++;
                    }
                }
    		}
    		br.close();
    	}
    	catch (Exception e) //�e pride do napake pri branju datoteke
    	{
    		Toast.makeText(getApplicationContext(), "napaka pri odpiranju! " + e, Toast.LENGTH_LONG).show();
    	}
    }
    
    public void spremeniPolozaj(double geos, double geod) //funkcija za spreminjanje mesta pri�etka branja v datoteki
    {
    	String gs = String.valueOf(geos);
    	String gd = String.valueOf(geod);
    	String[] s = gs.split("[.]");
    	String[] d = gd.split("[.]");
    	
    	String fx = f.substring(1, 3);
    	String fy = f.substring(5, 7);
    	
    	if(s[0] != fx || d[0] != fy) //novo datoteko odpremo samo po potrebi
    	{
    		if(Double.parseDouble(d[0]) < 10)
    		{
    			f = "n" + s[0] + "e00" + d[0] + ".txt";
    		}
    		f = "n" + s[0] + "e0" + d[0] + ".txt";
    	}
    	
        latD = Double.parseDouble(s[1]) / Math.pow(10, s[1].length());
        lonD = Double.parseDouble(d[1]) / Math.pow(10, d[1].length());
        
        double tx = latD * 1201; //pomo� pri dolo�anju mesta pri�etka branja v datoteki
        String tmp = String.valueOf(tx);
        String[] ttx = tmp.split("[.]");
        x = tx - Integer.valueOf(ttx[0]);
        
        double ty = lonD * 1201;
        tmp = String.valueOf(ty);
        String[] tty = tmp.split("[.]");
        y = ty - Integer.valueOf(tty[0]);
        
        if(x < 0.5)
        {
        	mestoVrstica = 1201 - (int)Math.floor(latD * 1201) - 1;
        }
        else
        {
        	mestoVrstica = 1201 - (int)Math.ceil(latD * 1201) - 1;
        }
        
        if(y < 0.5)
        {
        	mestoStolpec = 1201 - (int)Math.floor(lonD * 1201) - 1;
        }
        else
        {
        	mestoStolpec = 1201 - (int)Math.ceil(lonD * 1201) - 1;
        }
        
    	try
    	{
    		File root = Environment.getExternalStorageDirectory();
    		mapa = root.toString() + "/planinec/";
            File f2 = new File(mapa, f);

    		br = new BufferedReader(new FileReader(f2));
    		
    		int vr = 0;
    		int st = 0;
    		int stVrstic = 0;
    		
    		for(int i = 0; i <= mestoVrstica+3; i++)   
    		{
                vrstica = br.readLine();
                stVrstic++;

                if (stVrstic == mestoVrstica + 3 || stVrstic == mestoVrstica + 2 || stVrstic == mestoVrstica + 1
                     || stVrstic == mestoVrstica || stVrstic == mestoVrstica - 1 || stVrstic == mestoVrstica - 2 || stVrstic == mestoVrstica - 3)
                {
                    nv = vrstica.split("[\\s]");
                    for (int j = mestoStolpec - 3; j <= mestoStolpec + 3; j++)
                    {
                        if (st + 1 == 7)
                        {
                            vr++;
                            st = 0;
                        }
                        visine[vr][st] = Double.parseDouble(nv[j]);
                        st++;
                    }
                }
    		}
    		br.close();
    	}
    	catch (Exception e)
    	{
    		Toast.makeText(getApplicationContext(), "napaka pri odpiranju! " + e, Toast.LENGTH_LONG).show();
    	}
    }
     
    public void napolniPoljeRazdalj() //funkcija, s katero napolnimo polje razdalj med znanimi to�kami
    {
    	//venec1 - 4 sosednje
    	distance[0] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*y, 2));
    	distance[1] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*y, 2));
    	distance[2] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(1-y), 2));
    	distance[3] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(1-y), 2));

    	//venec2 - 12 sosednjih
    	distance[4] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(1+y), 2));
    	distance[5] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(1+y), 2));
    	distance[6] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(1+y), 2));
    	distance[7] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(1+y), 2));
    	distance[8] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*y, 2));
    	distance[9] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(1-y), 2));
    	distance[10] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(2-y), 2));
    	distance[11] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(2-y), 2));
    	distance[12] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(2-y), 2));
    	distance[13] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(2-y), 2));
    	distance[14] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(1-y), 2));
    	distance[15] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*y, 2));
    	
    	//venec3 - 20 sosednjih
    	distance[16] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(2+y), 2));
    	distance[17] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(2+y), 2));
    	distance[18] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(2+y), 2));
    	distance[19] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(2+y), 2));
    	distance[20] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(2+y), 2));
    	distance[21] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(2+y), 2));
    	distance[22] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(1+y), 2));
    	distance[23] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*y, 2));
    	distance[24] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(1-y), 2));
    	distance[25] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(2-y), 2));
    	distance[26] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(3-y), 2));
    	distance[27] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(3-y), 2));
    	distance[28] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(3-y), 2));
    	distance[29] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(3-y), 2));
    	distance[30] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(3-y), 2));
    	distance[31] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(3-y), 2));
    	distance[32] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(2-y), 2));
    	distance[33] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(1-y), 2));
    	distance[34] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*y, 2));
    	distance[35] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(1+y), 2));  	
    }
    
    public void napolniPoljeVisin() //funkcija, s katero napolnimo polje vi�in
    {
    	//venec1 - 4 sosednje
        tmp_vis[0] = visine[3][2];
        tmp_vis[1] = visine[3][3];
        tmp_vis[2] = visine[2][3];
        tmp_vis[3] = visine[2][2];
        
        //venec2 - 12 sosednjih
        tmp_vis[4] = visine[4][1];
        tmp_vis[5] = visine[4][2];
        tmp_vis[6] = visine[4][3];
        tmp_vis[7] = visine[4][4];
        tmp_vis[8] = visine[3][4];
        tmp_vis[9] = visine[2][4];
        tmp_vis[10] = visine[1][4];
        tmp_vis[11] = visine[1][3];
        tmp_vis[12] = visine[1][2];
        tmp_vis[13] = visine[1][1];
        tmp_vis[14] = visine[2][1];
        tmp_vis[15] = visine[3][1];
        
        //venec3 - 20 sosednjih
        tmp_vis[16] = visine[5][0];
        tmp_vis[17] = visine[5][1];
        tmp_vis[18] = visine[5][2];
        tmp_vis[19] = visine[5][3];
        tmp_vis[20] = visine[5][4];
        tmp_vis[21] = visine[5][5];
        tmp_vis[22] = visine[4][5];
        tmp_vis[23] = visine[3][5];
        tmp_vis[24] = visine[2][5];
        tmp_vis[25] = visine[1][5];
        tmp_vis[26] = visine[0][5];
        tmp_vis[27] = visine[0][4];
        tmp_vis[28] = visine[0][3];
        tmp_vis[29] = visine[0][2];
        tmp_vis[30] = visine[0][1];
        tmp_vis[31] = visine[0][0];
        tmp_vis[32] = visine[1][0];
        tmp_vis[33] = visine[2][0];
        tmp_vis[34] = visine[3][0];
        tmp_vis[35] = visine[4][0];
    }
    
    public double interpolacija() //funkcija za interpolacijo
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 16; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-1));
    		dis += (Math.pow(distance[i],-1));
    	}
    	visina = alt/dis;
    	
    	return visina;
    }
    
    private void updateLoc(Location loc) //funkcija, s katero pridobimo trenutno lokacijo iz sistema GPS
    {
    	lat = loc.getLatitude();
    	lon = loc.getLongitude();
    	alt = loc.getAltitude();
    	
    	napaka =  loc.getAccuracy(); //napaka pri dolo�anju lokacije iz sistema GPS
    	
    	GeoPoint locGeoPoint = new GeoPoint(lat, lon); //ustvarimo to�ko
    	myMapController.setCenter(locGeoPoint); //in z njo centriramo prikazano obmo�je na zemljevidu

    	lokacija.setText("lokacija: " + String.valueOf(df.format(lat)) + 
    					 ", " + String.valueOf(df.format(lon))); //prikaz koordinat trenutne lokacije
    	
    	tocke.add(lat + " " + lon + " " + alt); //vsako to�ko, na kateri se nahajamo dodamo na seznam
    	
    	myOpenMapView.invalidate(); //posodobimo prikaz na zemljevidu
    	
    }
    
    private LocationListener myLocationListener = new LocationListener()
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
    
    private SensorEventListener mSensorEventListenerOrientometer = new SensorEventListener() 
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy) 
		{
		}
		public void onSensorChanged(SensorEvent e) 
		{
			synchronized (this) 
			{					
				TextView t1 = (TextView) findViewById(R.id.kompas);
				t1.setText("Azimut: " + Float.toString(e.values[0]) + "�");
			}
		}
	};

    private class MyItemizedIconOverlay extends ItemizedIconOverlay<OverlayItem>
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
    			//iz to�ke, ki jo kliknemo na zemljevidu izra�unamo geografske koordinate
    			GeoPoint p = (GeoPoint) myOpenMapView.getProjection().fromPixels((int)event.getX(), (int)event.getY());
    			double gs = p.getLatitudeE6()/1E6; //geografska �irina
    			double gd = p.getLongitudeE6()/1E6; //geografska dol�ina
    			
    			if(f == null) //�e �e nismo odprli nobene datoteke
    			{
    				odpriDatoteko(gs, gd); //odpremo ustrezno datoteko; s parametri dolo�imo mesto branja 
    			}
    			spremeniPolozaj(gs, gd); //sicer ustrezno spremenimo mesto branja
    			napolniPoljeRazdalj(); //napolnimo polje razdalj med znanimi to�kami
    			napolniPoljeVisin(); //napolnimo polje vi�in
    			double v = interpolacija(); //interpolirana nadmorska vi�ina
    			
    			//uporabniku prika�emo okence z geografskimi koordinatami in nadmorsko vi�ino to�ke klika
    			Toast.makeText(getApplicationContext(), "lokacija: " + gs + ", " + gd + ", " + v, Toast.LENGTH_LONG).show();
    		}
    		return false;
    	}
    }
    
    private void shraniTocke() //funkcija za shranjevanje seznama to�k, na katerih smo se nahajali
    {
    	File logFile = new File(Environment.getExternalStorageDirectory() + "planinec/OsmShranjeneTocke.txt"); //datoteka
    	
    	if (!logFile.exists())
    	{
    		try
    		{
    			logFile.createNewFile(); //�e datoteka �e ne obstaja, jo ustvarimo
    		}  
    		catch (IOException e)
    		{
    			e.printStackTrace();
    		}
	   }
	   try
	   {
		   //shranjevanje seznama to�k
		   BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
		   for (int i = 0; i < tocke.size(); i++) //gremo �ez seznam
		   {
			   buf.append((char) tocke.indexOf(i)); //vsaki� shranimo novo to�ko
			   buf.newLine(); //in sko�imo v novo vrstico
		   }
		   buf.close(); //sprostimo dostop do datoteke
		   tocke.clear(); //po�istimo seznam to�k
		   Toast.makeText(getApplicationContext(), "zapisovanje uspe�no!", Toast.LENGTH_SHORT).show();	//obvestilo o uspe�nosti
	   }
	   catch (IOException e) //napaka pri shranjevanju
	   {
		   e.printStackTrace();
		   Toast.makeText(getApplicationContext(), "napaka pri zapisovanju " + e, Toast.LENGTH_SHORT).show();
	   }
    }
    
    private void preberiTocke() //funkcija za branje shranjenih to�k, ki jih uporabimo pri vodenju uporabnika
    {
    	String mapa = Environment.getExternalStorageDirectory().toString() + "/planinec/"; //mapa s podatki
    	String f = "tocke.txt"; //seznam to�k
    	File f2 = new File(mapa,f);
    	
    	tocke_pot = new ArrayList<OverlayItem>(); //dodatna plast na zemljevidu za prikaz vseh to�k
    	pot = new ArrayList<GeoPoint>(); //seznam to�k, za izra�un nadaljevanja poti
    	
		try 
    	{
			BufferedReader br = new BufferedReader(new FileReader(f2));
			String vrstica;
			P = new GeoPoint(lat,lon); //to�ka na kateri se trenutno nahajamo
			
			while((vrstica = br.readLine()) != null)
			{
				sb = vrstica.split("[\\s]");
				ciljLAT = Double.parseDouble(sb[0]);
				ciljLON = Double.parseDouble(sb[1]);
				
				Q = new GeoPoint(ciljLAT, ciljLON); //to�ka, ki jo dodamo na seznam vseh shranjenih to�k
				
				pot.add(Q); //ustvarimo seznam to�k
				tocke_pot.add(new OverlayItem("pot", "to�ka", Q)); //to�ko dodamo tudi na seznam za prikaz na zemljevidu
			}
			br.close();
			
			for(int i = 0; i < pot.size()-1; i++)
			{
				for(int j = 1; j < pot.size(); j++)
				{
					Q = pot.get(i);
					razdalja = P.distanceTo(Q); //najkraj�a pot med trenutno in prvo to�ko
					
					tmp = pot.get(j); //naslednja to�ka
					
					razdalja1 = P.distanceTo(tmp); //razdalja med trenutno in drugo to�ko 
					
					if(razdalja1 < razdalja) //dolo�imo trenutno najbli�jo to�ko in razdaljo do nje
					{
						Q = tmp;
						tmp = pot.get(j);
						razdalja = razdalja1;
					}
					
					W = tmp; //druga to�ka
				}
			}
			
			/*double normala = Math.sqrt(Math.pow((W.getLatitudeE6() - Q.getLatitudeE6()),2) + 
									  (Math.pow((W.getLongitudeE6() - Q.getLongitudeE6()),2)));
			
            /*razdalja1 = Math.abs((P.getLatitudeE6() - Q.getLatitudeE6()) * (W.getLongitudeE6() - Q.getLongitudeE6()) - 
            					 (P.getLongitudeE6() - Q.getLongitudeE6()) * (W.getLatitudeE6() - Q.getLatitudeE6())) / 
            					 normala; */
            
			//dolo�imo koordinate to�ke na polovici med prvo in drugo to�ko
			double razlikaX = Q.getLatitudeE6()/1E6 + (Math.abs(Q.getLatitudeE6()/1E6 - W.getLatitudeE6()/1E6))/2;
			double razlikaY = Q.getLongitudeE6()/1E6 + (Math.abs(Q.getLongitudeE6()/1E6 - W.getLongitudeE6()/1E6))/2;
			
			T = new GeoPoint(razlikaX,razlikaY); //ta to�ka pomeni pribli�no mesto pravokotne projekcije med trenutno to�ko in
												 //premico med prvo in drugo to�ko
			
			if(P.distanceTo(T) > napaka) //glede na oddaljenost trenutne to�ke od predvidene poti
			{
				razdalja = P.distanceTo(T); 
				smer = P.bearingTo(T);
/*				Log.i("tocka P:", P.getLatitudeE6() + ", " + P.getLongitudeE6());
				Log.i("tocka Q:", Q.getLatitudeE6() + ", " + Q.getLongitudeE6());
				Log.i("tocka W:", W.getLatitudeE6() + ", " + W.getLongitudeE6());
				Log.i("tocka T:", T.getLatitudeE6() + ", " + T.getLongitudeE6());*/
				
				r.setText(razdalja + "m" + ", " + "smer: " + smer);
				r.setTextColor(Color.RED); //ustrezno obarvamo tekst, ki prikazuje smer in oddaljenost do naslednje to�ke
			}
			else
			{
				razdalja = P.distanceTo(W);
				smer = P.bearingTo(W);
				r.setText(razdalja + "m" + ", " + "smer: " + smer);
				r.setTextColor(Color.GREEN);
			}
			
			ItemizedIconOverlay<OverlayItem> anotherItemizedIconOverlay = 
					new ItemizedIconOverlay<OverlayItem>(this, tocke_pot, null); //plast za to�ke predvidene poti
	        myOpenMapView.getOverlays().add(anotherItemizedIconOverlay); //plast dodamo na zemljevid
	        myOpenMapView.invalidate(); //po dodani plasti osve�imo prikaz zemljevida
		} 
    	catch (IOException e) //napaka pri branju to�k iz datoteke
		{
    		e.printStackTrace();
 		   Toast.makeText(getApplicationContext(), "napaka pri branju " + e, Toast.LENGTH_SHORT).show();
		}
		
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @SuppressWarnings("deprecation")
	@Override
    protected void onResume() 
    {
    	super.onResume();
    	mSensorManager.registerListener(mSensorEventListenerOrientometer, 
				 mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), 
				 SensorManager.SENSOR_DELAY_NORMAL);
    	
    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, myLocationListener); //aktiviramo zahtevo za posodabljanje lokacije
    	myLocationOverlay.enableCompass(); //omogo�imo interaktivnost na zemljevidu - prikaz in sledenje lokaciji, prikaz kompasa
    	myLocationOverlay.enableMyLocation();
    	myLocationOverlay.enableFollowLocation();
    }

    @Override
    protected void onPause() 
    {
    	super.onPause();
    	locationManager.removeUpdates(myLocationListener); //deaktiviramo zahtevo za posodabljanje lokacije
    	myLocationOverlay.disableCompass(); //onemogo�imo interaktivnost na zemljevidu - prikaz in sledenje lokaciji, prikaz kompasa
    	myLocationOverlay.disableMyLocation();
    	myLocationOverlay.disableFollowLocation();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) //funkcija za priklic izgleda in vsebine menija znotraj aktivnosti
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.osm, menu);
        return true;
    }
    
	public boolean onOptionsItemSelected(MenuItem item) //priklic ustreznih akcij
	{
		switch (item.getItemId()) 
		{
		    case R.id.shraniPot:
		    	shraniTocke();
		    	return true;
		    case R.id.uvoziPot:
		    	preberiTocke();
		    	return true;
		    case R.id.nastavitve:
				startActivity(new Intent(this, MyPreferenceActivity.class));
				return true;
		    case R.id.tocka:
		    	startActivity(new Intent(this, TockaActivity.class));
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
