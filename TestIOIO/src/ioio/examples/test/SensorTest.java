package ioio.examples.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SensorTest extends Activity implements SensorEventListener {

	public final static String TAG = "SensorTest";
	protected final static double RAD2DEG = 180/Math.PI;
    private ScheduledExecutorService ses = null;
	
	SensorManager sensorManager;
	
	float[] rotationMatrix = new float[9];
	float[] gravity = new float[3];
	float[] geomagnetic = new float[3];
	float[] attitude = new float[3];
	float r = 1;
	float x, y, z;
	
	LinearLayout layout;
	
	TextView azimuthText;
	TextView pitchText;
	TextView rollText;

	TextView valueX;
	TextView valueY;
	TextView valueZ;
	
	ToggleButton logging;
	
	Util util;
	Thread thread;
	
	TrailView trailView;
	
	private final Runnable task = new Runnable(){
		public void run() {
		    //Log.d(TAG, "running...");
		    
			util.setText(azimuthText, Integer.toString((int)(attitude[0] * RAD2DEG)));
			util.setText(pitchText, Integer.toString((int)(attitude[1] * RAD2DEG)));
			util.setText(rollText, Integer.toString((int)(attitude[2] * RAD2DEG)));
	
			// 直交座標に変換
			double theta, phi;
			theta = Math.PI*0.5 + attitude[1];
			phi = - attitude[0];
			x = (float)(r * Math.sin(theta) * Math.cos(phi));
			y = (float)(r * Math.sin(theta) * Math.sin(phi));
			z = (float)(r * Math.cos(theta));
			util.setText(valueX, String.format("%.3f", x));
			util.setText(valueY, String.format("%.3f", y));
			util.setText(valueZ, String.format("%.3f", z));
			
			// Trailに情報追加
			if(logging.isChecked())	trailView.addTp(x,y,z);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		util = new Util(new Handler());
		trailView = new TrailView(this);

		setContentView(R.layout.sensor_view);
		layout = (LinearLayout) findViewById(R.id.sensorLayout);
		layout.addView(trailView);
        
		findViews();
		initSensor();
	}
	
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onResume!!");
		sensorManager.registerListener(
			this,
			sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
			SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(
			this,
			sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
			SensorManager.SENSOR_DELAY_GAME);
		thread = new Thread(task);
		thread.start();

        // タイマーを作成する
        ses = Executors.newSingleThreadScheduledExecutor();
        // 100msごとにRunnableの処理を実行する
        ses.scheduleAtFixedRate(task, 0L, 100L, TimeUnit.MILLISECONDS);
	}
	
	public void onPause(){
		super.onPause();
		sensorManager.unregisterListener(this);

        // タイマーを停止する
        ses.shutdown();
        ses = null;
	}
	
	protected void findViews(){
		azimuthText = (TextView)findViewById(R.id.azimuth);
		pitchText = (TextView)findViewById(R.id.pitch);
		rollText = (TextView)findViewById(R.id.roll);
		valueX = (TextView)findViewById(R.id.valueX);
		valueY = (TextView)findViewById(R.id.valueY);
		valueZ = (TextView)findViewById(R.id.valueZ);
		logging = (ToggleButton)findViewById(R.id.logging);
		logging.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)	trailView.onResume();
				else			trailView.onPause();
			}
		});
	}
	
	protected void initSensor(){
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		switch(event.sensor.getType()){
		case Sensor.TYPE_MAGNETIC_FIELD:
			geomagnetic = event.values.clone();
			break;
		case Sensor.TYPE_ACCELEROMETER:
			gravity = event.values.clone();
			break;
		}

		if(geomagnetic != null && gravity != null){
			
			SensorManager.getRotationMatrix(
				rotationMatrix, null, 
				gravity, geomagnetic);
			
			SensorManager.getOrientation(
				rotationMatrix, 
				attitude);
			
		}
	}
}