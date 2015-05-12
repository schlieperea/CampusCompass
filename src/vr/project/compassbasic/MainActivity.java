package vr.project.compassbasic;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.ListIterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements SensorEventListener{
	
	private static SensorManager sensorManager;
	  private CompassView compass;
	  Sensor accelerometer;
	  Sensor magnetometer;
	  float azimuth;
	  Location current;
	  LocationManager locManager;
	  LinkedList<Location> locations;
	  String TAG;
	  

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TAG = getString(R.string.app_name);
		compass = new CompassView(this);
	    setContentView(compass);
	    locManager =  (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    if ( !locManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
	        buildAlertMessageNoGps();
	    }

	    LocationListener locListener = new MyLocationListener();
	    locManager.requestLocationUpdates(
	    LocationManager.GPS_PROVIDER, 5000, 10, locListener);

	    current = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

	    
	    sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
	    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    locations = new LinkedList<Location>();
	    setLocations(getApplicationContext());
	}
	
	  private void buildAlertMessageNoGps() {
		    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
		           .setCancelable(false)
		           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		               public void onClick(final DialogInterface dialog, final int id) {
		                   startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
		               }
		           })
		           .setNegativeButton("No", new DialogInterface.OnClickListener() {
		               public void onClick(final DialogInterface dialog, final int id) {
		                    dialog.cancel();
		               }
		           });
		    final AlertDialog alert = builder.create();
		    alert.show();
		}

	protected void onResume()
	{
		super.onResume();
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
	    sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}
	
	protected void onPause() {
	    super.onPause();
	    sensorManager.unregisterListener(this);
	  }
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
	 
	  float[] mGravity;
	  float[] mGeomagnetic;
	  public void onSensorChanged(SensorEvent event) {
	    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
	      mGravity = event.values;
	    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
	      mGeomagnetic = event.values;
	    if (mGravity != null && mGeomagnetic != null) {
	      float R[] = new float[9];
	      float I[] = new float[9];
	      boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
	      if (success) {
	        float orientation[] = new float[3];
	        SensorManager.getOrientation(R, orientation);
	        float negAzimuth = orientation[0];
	        if(negAzimuth>=0) {azimuth = negAzimuth;} else {azimuth = (float) (2*Math.PI + negAzimuth);}
	    
	      }
	      compass.invalidate();
	    }
	  }
	  
	  public Location fakeLocation(String name, double latitude, double longitude) {
		  Location r = new Location(name);
		  r.setLatitude(latitude);
		  r.setLongitude(longitude);
		  return r;
	  }
	  
	  public void setLocations(Context context) {
		  AssetManager am = context.getAssets();
		  try {
			  InputStreamReader isr = new InputStreamReader(am.open("latlong.txt"));
			  BufferedReader br = new BufferedReader(isr);
			  String line;
			  while((line = br.readLine()) != null) {
				  String[] tokens = line.split("\\s+",3);
				  Location temp = fakeLocation(tokens[0], Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
				  locations.add(temp);
			  }
		  } catch (IOException e) {
			  e.printStackTrace();
		  }
			  
	  }

	public class CompassView extends View {
		Paint p = new Paint();
		public CompassView (Context context)
		{
			super(context);
			p.setColor(Color.BLACK);
			p.setTextSize(100);	
		}
		
		protected void onDraw(Canvas canvas){
			
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			
			//canvas.drawText(azimuth +" ", width/2, height/2 + 100, p);
			ListIterator<Location> listIterator = locations.listIterator();
	        while (listIterator.hasNext()) {
	            Location temp = listIterator.next();
	            float bearing = current.bearingTo(temp);
	            //float distance = current.distanceTo(temp);
	            bearing = (float) Math.toRadians(bearing);
	            float diff = bearing-azimuth;
	            if(Math.abs(diff)<=.5) {
	            	
	            	//p.setTextSize(100 * (2000/distance));
	            	int x = (int) ((width/2 - 50) + width * (diff/.4));
	            	int y = (int) (height-600);
	            	canvas.drawText(temp.getProvider(), x, y, p);
	            }
	            	
	        }
			
			
			
		}
	}
	private class MyLocationListener implements LocationListener {

	    @Override
	    public void onLocationChanged(Location loc) {
	        current = loc;
	        compass.invalidate();
	        
	    }

	    @Override
	    public void onProviderDisabled(String provider) {}

	    @Override
	    public void onProviderEnabled(String provider) {}

	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras) {}
	}
}
