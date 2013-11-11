package com.magistrska;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class TockaActivity extends Activity implements OnClickListener, OnSeekBarChangeListener
{ 
	//spremenljivke za sledenje trenutni lokaciji
	LocationManager locationManager;
	LocationListener locationListener;
	private SensorManager mSensorManager = null;
	WindowManager wm;
	private List<String> tocke = new ArrayList<String>();
	
	//geografske koordinate toèke
	double lat;
	double lon;
	double alt;
	
	//prikaz vrednosti
	TextView elev;
	TextView a;
	TextView in;
	
	//interpolacija nadmorske višine - mesto v datoteki
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
	
	//interpolacija nadmorske višine - polje razdalj in višin
	double[] tmp_vis = new double[36];
	double[] distance = new double[36];
	
	//izraèun interpolirane vrednosti
	double visina;
	double dis;
	
	//zaokroževanje vrednosti
	DecimalFormat df = new DecimalFormat("#.####");
	double v2;
	
	//doloèanje zanesljivosti sistema GPS
	SeekBar s = null;
	SeekBar s1 = null;
	TextView t = null;
	TextView t1 = null;
	double gpsAlt;
	double intAlt;
	double pAlt;
	int n;
	int n1;
	int alfa;
	int beta;
	double zaupanje;
	
	//dostop do nastavitev
	SharedPreferences pref;
	String interval;
	int i;
	int stVencev;
	int param_r;
	String metoda;
	
	protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_altitude); //dostop do strukture za izgled aktivnosti
        pref = PreferenceManager.getDefaultSharedPreferences(this); //dostop do nastavitev
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //ohrani buden ekran
        
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); //nadzor nad senzorjem za pridobivanje lokacije
        locationListener = new locListener(); //"listener" za spreminjanje lokacije"

        interval = pref.getString("interval_lokacija", "n/a"); //èasovni interval posodabljanja lokacije
        i = Integer.parseInt(interval)*1000;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, i, 30, locationListener); //vklop zahteve za posodabljanje lokacije
        
        String v = pref.getString("venec", "n/a");
        String r = pref.getString("r", "n/a");
        
        stVencev = Integer.parseInt(v); //vrednosti parametrov za interpolacijo
        param_r = Integer.parseInt(r);
        metoda = pref.getString("metoda", "n/a");
        
        in = (TextView) findViewById(R.id.interpolacija); //interakcija z uporabnikom
        View elevButton = findViewById(R.id.btnElev);
        elevButton.setOnClickListener(this);
        
        s = (SeekBar)findViewById(R.id.seekBar1);
        s1 = (SeekBar) findViewById(R.id.seekBar2);
        t = (TextView)findViewById(R.id.zaupanjeGPS);
        t1 = (TextView) findViewById(R.id.zaupanjeZrak);
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        s.setOnSeekBarChangeListener(this);
        s1.setOnSeekBarChangeListener(this);
    }
    
    public void odpriDatoteko()
    {
    	String gs = String.valueOf(lat); //geografske koordinate trenutne lokacije
    	String gd = String.valueOf(lon);
    	String[] s = gs.split("[.]"); //koordinate razbijemo na celi in decimalni del
    	String[] d = gd.split("[.]");
    	
    	if(Double.parseDouble(s[0]) < 10) //èe je geografska širina manjša od 10
    	{
    		f = "n" + s[0] + "e00" + d[0] + ".txt"; //s celim delom doloèimo ime datoteke z nadmorskimi višinami
    	}
    	f = "n" + s[0] + "e0" + d[0] + ".txt";
    	
        latD = Double.parseDouble(s[1]) / Math.pow(10, s[1].length()); //decimalni del koordinate
        lonD = Double.parseDouble(d[1]) / Math.pow(10, d[1].length());
        
        double tx = latD * 1201; //pomoè pri doloèanju mesta prièetka branja v datoteki
        String tmp = String.valueOf(tx);
        String[] ttx = tmp.split("[.]");
        x = tx - Integer.valueOf(ttx[0]);
        
        double ty = lonD * 1201;
        tmp = String.valueOf(ty);
        String[] tty = tmp.split("[.]");
        y = ty - Integer.valueOf(tty[0]);
        
        if(x < 0.5) //indeks zaèetne vrstice ustrezno zaokrožimo navzgor ali navzdol
        {
        	mestoVrstica = 1201 - (int)Math.floor(latD * 1201) - 1;
        }
        else
        {
        	mestoVrstica = 1201 - (int)Math.ceil(latD * 1201) - 1;
        }
        
        if(y < 0.5) //indeks zaèetnega stolpca ustrezno zaokrožimo levo ali densno
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
            File f2 = new File(mapa, f); //datoteka v mapi

    		br = new BufferedReader(new FileReader(f2)); //"bralec" po datoteki
    		
    		int vr = 0;
    		int st = 0;
    		int stVrstic = 0;
    		
    		for(int i = 0; i <= mestoVrstica+3; i++) //premik v datoteki na pravo mesto
    		{
                vrstica = br.readLine(); //preberemo vrstico
                stVrstic++;

                //in poberemo samo vrednosti, ki jih potrebujemo
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
                        visine[vr][st] = Double.parseDouble(nv[j]); //vrednosti shranimo v zaèasno matriko višin 
                        st++;
                    }
                }
    		}
    		br.close();
    	}
    	catch (Exception e) //èe pride do napake pri branju datoteke
    	{
    		Toast.makeText(getApplicationContext(), "napaka pri odpiranju! " + e, Toast.LENGTH_LONG).show();
    	}
    }
    
    public void spremeniPolozaj() //funkcija za spreminjanje mesta branja vrednosti v datoteki
    {
    	String gs = String.valueOf(lat);
    	String gd = String.valueOf(lon);
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
        
        double tx = latD * 1201; //pomoè pri doloèanju mesta prièetka branja v datoteki
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
        
    	try //branje iskanih vrednosti
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
    	catch (Exception e) //èe pride do napake pri odpiranju datoteke
    	{
    		Toast.makeText(getApplicationContext(), "napaka pri odpiranju! " + e, Toast.LENGTH_LONG).show();
    	}
    }
     
    public void napolniPoljeRazdalj() //funkcija s katero napolnimo polje razdalj med znanimi toèkami
    {
    	//venec1 - 4 sosednje
    	distance[0] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*y, 2)); //levo spodaj
    	distance[1] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*y, 2)); //desno spodaj
    	distance[2] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(1-y), 2)); //desno zgoraj
    	distance[3] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(1-y), 2)); //levo zgoraj

    	//venec2 - 12 sosednjih
    	distance[4] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(1+y), 2)); //spodnja vrstica
    	distance[5] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(1+y), 2));
    	distance[6] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(1+y), 2));
    	distance[7] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(1+y), 2));
    	distance[8] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*y, 2)); //desni stolpec
    	distance[9] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(1-y), 2));
    	distance[10] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(2-y), 2)); //zgornja vrstica
    	distance[11] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(2-y), 2));
    	distance[12] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(2-y), 2));
    	distance[13] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(2-y), 2));
    	distance[14] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(1-y), 2)); //levi stolpec
    	distance[15] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*y, 2));
    	
    	//venec3 - 20 sosednjih
    	distance[16] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(2+y), 2)); //spodnja vrstica
    	distance[17] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(2+y), 2));
    	distance[18] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(2+y), 2));
    	distance[19] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(2+y), 2));
    	distance[20] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(2+y), 2));
    	distance[21] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(2+y), 2));
    	distance[22] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(1+y), 2)); //desni stolpec
    	distance[23] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*y, 2));
    	distance[24] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(1-y), 2));
    	distance[25] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(2-y), 2));
    	distance[26] = Math.sqrt(Math.pow(90*(3-x),2) + Math.pow(90*(3-y), 2)); //zgornja vrstica
    	distance[27] = Math.sqrt(Math.pow(90*(2-x),2) + Math.pow(90*(3-y), 2));
    	distance[28] = Math.sqrt(Math.pow(90*(1-x),2) + Math.pow(90*(3-y), 2));
    	distance[29] = Math.sqrt(Math.pow(90*x,2) + Math.pow(90*(3-y), 2));
    	distance[30] = Math.sqrt(Math.pow(90*(1+x),2) + Math.pow(90*(3-y), 2));
    	distance[31] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(3-y), 2));
    	distance[32] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(2-y), 2)); //levi stolpec
    	distance[33] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(1-y), 2));
    	distance[34] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*y, 2));
    	distance[35] = Math.sqrt(Math.pow(90*(2+x),2) + Math.pow(90*(1+y), 2));  	
    }
    
    public void napolniPoljeVisin() //funkcija s katero napolnimo polje nadmorskih višin
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
    
    public void metodePopravka1() //navadno povpreèenje s 4 sosednjimi toèkami
    {    	   	
    	for(int i = 0; i < 4; i++)
    	{
    		alt += tmp_vis[i];
    	}
    	visina = alt/8;
    	String v = String.valueOf(df.format(visina));
    	
    	in.setText("visina-interp: " + v);
    }
    
    //metoda IDW - venec1, r=0..3
    public void metodePopravka2()
    {  	
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 4; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],0));
    		dis += (Math.pow(distance[i],0));
    	}
    	visina = alt/dis;
    	String v = String.valueOf(df.format(visina));
    	
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka3()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 4; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-1)); 
    		dis += (Math.pow(distance[i],-1));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(df.format(visina));
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka4()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 4; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-2)); 
    		dis += (Math.pow(distance[i],-2));
    	}
    	visina = alt/dis;

    	String v = String.valueOf(df.format(visina));
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka5()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 4; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-3)); 
    		dis += (Math.pow(distance[i],-3));
    	}
    	
    	visina = alt/dis;
    	String v = String.valueOf(df.format(visina));
    	
    	in.setText("visina-interp: " + v);
    }
    
    //metoda IDW - venec2, r=0..3
    public void metodePopravka6()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 16; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],0));
    		dis += (Math.pow(distance[i],0));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(df.format(visina));
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka7()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 16; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-1));
    		dis += (Math.pow(distance[i],-1));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(df.format(visina));
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka8()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 16; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-2));
    		dis += (Math.pow(distance[i],-2));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(df.format(visina));
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka9()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 16; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-3));
    		dis += (Math.pow(distance[i],-3));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(df.format(visina));
    	in.setText("visina-interp: " + v);
    }
    
    //metoda IDW - venc3, r=0..3
    public void metodePopravka10()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 36; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],0));
    		dis += (Math.pow(distance[i],0));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(visina);
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka11()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 36; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-1));
    		dis += (Math.pow(distance[i],-1));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(visina);
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka12()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 36; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-2));
    		dis += (Math.pow(distance[i],-2));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(visina);
    	in.setText("visina-interp: " + v);
    }
    
    public void metodePopravka13()
    {
    	alt = 0;
    	dis = 0;
    	for(int i = 0; i < 36; i++)
    	{
    		alt += (tmp_vis[i]*Math.pow(distance[i],-3));
    		dis += (Math.pow(distance[i],-3));
    	}
    	visina = alt/dis;
    	
    	String v = String.valueOf(visina);
    	in.setText("visina-interp: " + v);
    }
    
    //hermitova interpolacija
    public void HermiteInterpolacija()
    {
    	double[] tocke = new double[25];

        for (int i = 0; i < 25; i++) //pripravimo si toèke za interpolacijo
        {
            tocke[i] = tmp_vis[i];
        }

        double[] d = new double[25]; //vektor rezultatov pri odvajanju
        double[] w = new double[24]; //uteži
        double[] diff = new double[25]; //razlike med toèkami

        for (int i = 0; i < 24; i++)
        {
            diff[i] = (tocke[i + 1] - tocke[i]); //izraèun razlik
        }

        for (int i = 1; i < 24; i++)
        {
            w[i] = Math.abs(diff[i] - diff[i - 1]); //izraèun uteži
        }

        for (int i = 2; i < 23; i++)
        {
            if (Math.abs(w[i - 1]) + Math.abs(w[i + 1]) != 0)
            {
                d[i] = (w[i + 1] * diff[i - 1] + w[i - 1] * diff[i]) / (w[i + 1] + w[i - 1]); //izraèun odvodov
            }
            else
            {
                d[i] = 0;
            }
        }

        d[0] = diffthreepoint(0, 0, tocke[0], 1, tocke[1],2, tocke[2]); //odvodi 3 toèk, kot pomoè pri definiciji krivulje
        d[1] = diffthreepoint(1, 0, tocke[0], 1, tocke[1],2, tocke[2]);
        d[23] = diffthreepoint(23, 21, tocke[21], 22, tocke[22], 23, tocke[23]);
        d[24] = diffthreepoint(24, 22, tocke[22], 23, tocke[23], 24, tocke[24]);

        double tmpVsota = 0.0;
        for (int i = 0; i < 24; i++)
        {
            tmpVsota += d[i];
        }
        
        in.setText("hermite: " + String.valueOf(df.format(Math.abs(tmpVsota))) + "\n"); //prikaz osnovnega izraèuna
        in.setText("hermite (1.5x): " + String.valueOf(df.format(Math.abs(tmpVsota*1.5)))); //osnovni izraèun, poveèan za faktor 1.5
    }
    
    //funkcija za izraèun odvodov
    private static double diffthreepoint(double t, double x0, double f0, double x1, double f1, double x2, double f2)
    {
        double result = 0;
        double a = 0;
        double b = 0;

        t = t - x0; //priprava èlenov
        x1 = x1 - x0;
        x2 = x2 - x0;
        a = (f2 - f0 - x2 / x1 * (f1 - f0)) / (Math.pow(x2, 2) - x1 * x2); //izraèun odvodov
        b = (f1 - f0 - a * Math.pow(x1, 2)) / x1;
        result = 2 * a * t + b; //rezultat

        return result;
    }
    
    //"listener" za posodabljanje trenutne lokacije
	public class locListener implements LocationListener
	{
		public void onLocationChanged(Location loc)
		{
			elev = (TextView) findViewById(R.id.elevText);
	    	TextView a = (TextView) findViewById(R.id.GPSaltitude);
	        lat = loc.getLatitude();
	        lon = loc.getLongitude();
	        alt = loc.getAltitude();
	        
	        String tmp = String.valueOf(df.format(alt));
	        String nv = tmp.replace(",", ".");
	        gpsAlt = Double.valueOf(nv);
	        
	        a.setText("višina - GPS: " + gpsAlt);
	        elev.setText("Lat: " + lat  + ", Lon: " + lon);
	        tocke.add(lat + ", " + lon + ", " + alt + "\n");
	        
	        if(f == null)
	        {
	        	odpriDatoteko();
	        }
	        spremeniPolozaj();
	        napolniPoljeRazdalj();
	        napolniPoljeVisin();
	        metodePopravka7();
	        
	        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		public void onProviderDisabled(String provider)
		{
			Toast.makeText( getApplicationContext(),"Gps Disabled",Toast.LENGTH_SHORT ).show();
		}
	
		public void onProviderEnabled(String provider)
		{
			Toast.makeText( getApplicationContext(),"Gps Enabled",Toast.LENGTH_SHORT).show();
		}
		
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
		}
	}
	
	private SensorEventListener p = new SensorEventListener() //funkcija za izraèun nadmorske višine s pomoèjo barometra
    {	
    	float value = 0;
    	
		@Override
		public void onSensorChanged(SensorEvent event) 
		{
			synchronized (this) 
			{
			    value = event.values[0]; //vrednost zraènega pritiska iz barometra

				float osnova_pritisk = 1013.25f; //zraèni pritisk ob morski gladini
		    	float nadmVisina = (osnova_pritisk - value) * 8; //približen preraèun nadmorske višine
		    	
		    	TextView t4 = (TextView) findViewById(R.id.visinaZrak); //prikaz preraèunane nadmorske višine
				t4.setText("višina - zrak: " + nadmVisina);
		        
		        pAlt = Double.parseDouble(String.valueOf(nadmVisina)); //za uporabo v enaèbi zanesljivosti sistema GPS
			}
		}
		
		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) 
		{	
		}
	};
	
	public void btnClick() //klik na gumb "interp"
    {
    	if(f == null) //klic ustrezne funkcije za branje višin iz datoteke
    	{
    		odpriDatoteko();
    	}
    	else
    	{
    		spremeniPolozaj();
    	}
    	
        napolniPoljeRazdalj(); //klic funkcije, s katero napolnimo polje razdalj med toèkami z znano višino
        napolniPoljeVisin(); //klic funkcije, s katero napolnimo polje višin
        
        Toast.makeText(getApplicationContext(), "st vencev: " + stVencev + ", " + 
        				"r: " + param_r + "metoda: " + metoda, Toast.LENGTH_LONG).show(); //prikaz nastavljenih parametrov za interpolacijo
        	
        if(stVencev == 1 && param_r == 0 && metoda == "idw") //klic ustrezne metode na osnovi nastavljenih parametrov
        {
        	metodePopravka2();
        }
        else if(stVencev == 1 && param_r == 1 && metoda == "idw")
        {
        	metodePopravka3();
        }
        else if(stVencev == 1 && param_r == 2 && metoda == "idw")
        {
        	metodePopravka4();
        }
        else if(stVencev == 1 && param_r == 3 && metoda == "idw")
        {
        	metodePopravka5();
        }
        else if(stVencev == 2 && param_r == 0 && metoda == "idw")
        {
        	metodePopravka6();
        }
        else if(stVencev == 2 && param_r == 1 && metoda == "idw")
        {
        	metodePopravka7();
        }
        else if(stVencev == 2 && param_r == 2 && metoda == "idw")
        {
        	metodePopravka8();
        }
        else if(stVencev == 2 && param_r == 3 && metoda == "idw")
        {
        	metodePopravka9();
        }
        else if(stVencev == 3 && param_r == 0 && metoda == "idw")
        {
        	metodePopravka10();
        }
        else if(stVencev == 3 && param_r == 1 && metoda == "idw")
        {
        	metodePopravka11();
        }
        else if(stVencev == 3 && param_r == 2 && metoda == "idw")
        {
        	metodePopravka12();
        }
        else if(stVencev == 3 && param_r == 3 && metoda == "idw")
        {
        	metodePopravka13();
        }
        else
        {
        	HermiteInterpolacija();
        }
        
        metodePopravka1();
        String tmp = String.valueOf(df.format(visina)); //za uporabo v enaèbi doloèanja zanesljivosti sistema GPS
        String vf = tmp.replace(",", ".");
        
        intAlt = Double.parseDouble(vf);
    }
    
    public void onClick(View v) //akcija, ki se zgodi ob kliku na gumb
    {
    	switch(v.getId())
    	{
    		case R.id.btnElev:
    			btnClick(); //poklièemo ustrezno funkcijo
    			break;
    		default:
    			break;
    	}
    }
    
	private void shraniTocke() //funkcija za shranjevanje seznama toèk, na katerih smo se nahajali
    {
		String mapa = Environment.getExternalStorageDirectory().toString() + "/planinec/"; //mapa s podatki
    	String f = "LokacijaShranjeneTocke.txt"; //seznam toèk
    	File f2 = new File(mapa,f); //datoteka
    	
    	if (!f2.exists())
    	{
    		try
    		{
    			f2.createNewFile(); //èe datoteka še ne obstaja, jo ustvarimo
    		} 
    		catch (IOException e)
    		{
    			Toast.makeText(getApplicationContext(), "napaka pri ustvarjanju datoteke" + e, Toast.LENGTH_LONG).show();
    		}
	   }
	   try
	   {
		   //zapisovanje seznama toèk
		   BufferedWriter buf = new BufferedWriter(new FileWriter(f2, true)); 
		   for (int i = 0; i < tocke.size(); i++) //gremo èez seznam toèk
		   {
			   buf.append((char) tocke.indexOf(i)); //in vsako toèko shranimo v svojo vrstico
			   buf.newLine();
		   }
		   buf.close(); //sprostimo dostop do datoteke
		   tocke.clear(); //po koncanem zapisovanju, pocistimo seznam da naredimo prostor za nove tocke
		   Toast.makeText(getApplicationContext(), "zapisovanje uspešno!", Toast.LENGTH_SHORT).show(); //obvestilo o uspešnosti
	   }
	   catch (IOException e) //napaka pri shranjevanju
	   {
		   e.printStackTrace();
		   Toast.makeText(getApplicationContext(), "napaka pri zapisovanju " + e, Toast.LENGTH_SHORT).show();
	   }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) //funkcija za prikaz menija znotraj aktivnosti
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lokacija, menu);
        return true;
    }
    
	public boolean onOptionsItemSelected(MenuItem item) //funkcija za priklic ustreznih funkcij, doloèenih v vsebini menija
	{
		switch (item.getItemId()) 
		{
	    	case R.id.shraniPot:
	    		shraniTocke();
	    		return true;
	    	case R.id.nastavitve:
				startActivity(new Intent(this, MyPreferenceActivity.class));
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

	@Override
	public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) 
	{
		switch(seekBar.getId())
		{
			case R.id.seekBar1:
			{
				n = progress;
				break;
			}
			case R.id.seekBar2:
			{
				n1 = progress;
				break;
			}
		}
		//n = progress; //nastavljena vrednost na skali
	}

	@Override
	public void onStartTrackingTouch(android.widget.SeekBar seekBar) 
	{
	}

	@Override
	public void onStopTrackingTouch(android.widget.SeekBar seekBar) 
	{
		//alfa = (100-n)/2; //polovica razlike med 1 in odstotkom zanesljivosti sistema GPS
		alfa = ((100-n)*n1)/100;
		beta = (100-n-alfa)/100;
		
		if(n1 != 0)
		{
			n = n*(100-n1)/100;
		}
		
		zaupanje = (n*gpsAlt + alfa*pAlt + beta*intAlt)/100; //izraèun vrednosti
		
		t.setText("zanesljivost GPS:" + n + "%"); //prikaz nastavljene vrednosti zanesljivosti GPS
		t1.setText("zanesljivost barometra: " + n1 + "%" + "\n" + 
					"nadm.visina - zaupanje: " + zaupanje); //prikaz nastavljene vrednosti zanesljivosti barometra
															//in preraèunana nadmorska višina
	}
	
	protected void onResume() 
	{
		super.onResume();
		mSensorManager.registerListener(p, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), 
				SensorManager.SENSOR_DELAY_NORMAL); //aktiviramo nadzornika nad barometrom
		locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, i, 30, locationListener); //aktiviranje zahteve za posodabljanje lokacije
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		locationManager.removeUpdates(locationListener); //deaktiviranje zahteve za posodabljanje lokacije
	} 
}
