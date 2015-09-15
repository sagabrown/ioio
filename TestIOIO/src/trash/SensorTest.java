package trash;

import ioio.momot.R;
import ioio.robot.region.crawl.sensor.TrailView;
import ioio.robot.util.Util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import trash.shocksensor.ShockEvent;
import trash.shocksensor.ShockSensor;
import trash.shocksensor.ShockSensorListener;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Button;

public class SensorTest extends Activity implements SensorEventListener, ShockSensorListener {
	Util util;
	Activity parent;
	public final static String TAG = "SensorTest";
	protected final static float RAD2DEG = (float)(180/Math.PI);
    private ScheduledExecutorService ses = null;
	
    private ShockSensor shockSensor;
	private SensorManager sensorManager;
	
	float[] rotationMatrix = new float[9];
	float[] gravity = new float[3];
	float[] geomagnetic = new float[3];
	float[] attitude = new float[3];
	float x1, y1, z1;
	float x2, y2, z2;
	
	LinearLayout layout;
	
	TextView azimuthText;
	TextView pitchText;
	TextView rollText;

	TextView valueX;
	TextView valueY;
	TextView valueZ;

	ToggleButton logging;
	Button clearButton, setButton;
	TextView shockSensorLabel;
	private boolean trailFixed;
	private int shockCount;
	
	TrailView trailView;
	SeekBar seekBarX, seekBarY, seekBarZ;
	SeekBar seekBarLaX, seekBarLaY, seekBarLaZ;
	

	private final Runnable task = new Runnable(){
		public void run() {
		    //Log.d(TAG, "running...");
			
		    // 生データをテキスト表示
			util.setText(azimuthText, Integer.toString((int)(attitude[0] * RAD2DEG)));
			util.setText(pitchText, Integer.toString((int)(attitude[1] * RAD2DEG)));
			util.setText(rollText, Integer.toString((int)(attitude[2] * RAD2DEG)));
	
			// 直交座標に変換
			double theta, phi;
			// 前
			theta = Math.PI*0.5 + attitude[1];
			phi = - attitude[0];
			x1 = (float)(Math.sin(theta) * Math.cos(phi));
			y1 = (float)(Math.sin(theta) * Math.sin(phi));
			z1 = (float)(Math.cos(theta));
			util.setText(valueX, String.format("%.3f", x1));
			util.setText(valueY, String.format("%.3f", y1));
			util.setText(valueZ, String.format("%.3f", z1));
			// 右
			theta = Math.PI*0.5 + attitude[2];
			phi = -Math.PI*0.5 - attitude[0];
			x2 = (float)(Math.sin(theta) * Math.cos(phi));
			y2 = (float)(Math.sin(theta) * Math.sin(phi));
			z2 = (float)(Math.cos(theta));

			// 現在の情報
			trailView.setNowData(attitude[0] * RAD2DEG, attitude[1] * RAD2DEG, attitude[2] * RAD2DEG,
					x1,y1,z1,x2,y2,z2);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		util = new Util(new Handler());
		trailView = new TrailView(this);
		trailFixed = false;

		setContentView(R.layout.sensor_view);
		layout = (LinearLayout) findViewById(R.id.sensorLayout);
		layout.addView(trailView);
        
		findViews();
		
		// TrailViewの操作パネル
		makeLookSeekBar(seekBarX, 100, 0, "from:X");
		makeLookSeekBar(seekBarY, 100, 1, "from:Y");
		makeLookSeekBar(seekBarZ, 100, 2, "from:Z");
		makeLookSeekBar(seekBarLaX, 100, 3, "  at:X");
		makeLookSeekBar(seekBarLaY, 100, 4, "  at:Y");
		makeLookSeekBar(seekBarLaZ, 100, 5, "  at:Z");
		
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
		clearButton = (Button)findViewById(R.id.clear);
		clearButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				trailView.clearTrail();
				trailFixed = false;
				logging.setChecked(false);
				logging.setClickable(true);
			}
		});
		setButton = (Button)findViewById(R.id.set);
		setButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				trailView.setTrail();
				trailFixed = true;
				logging.setChecked(false);
				logging.setClickable(false);
			}
		});
		shockSensorLabel = (TextView)findViewById(R.id.shockSensorLabel);
		
	}
	
	protected void makeLookSeekBar(SeekBar sb, final int max, final int tag, String name){
		sb = new SeekBar(this);
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onStartTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				switch(tag){
				case 0: trailView.setLookX((float)(progress-max*0.5)); break;
				case 1: trailView.setLookY((float)(progress-max*0.5)); break;
				case 2: trailView.setLookZ((float)(progress-max*0.5)); break;
				case 3: trailView.setLaX((float)(progress-max*0.5)); break;
				case 4: trailView.setLaY((float)(progress-max*0.5)); break;
				case 5: trailView.setLaZ((float)(progress-max*0.5)); break;
				}
			}
		});
		sb.setMax(max);
		sb.setProgress(max/2);
		
		TextView label = new TextView(this);
		label.setText(name);
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setWeightSum(10);
		LayoutParams lp1 = new LinearLayout.LayoutParams(
	                    LinearLayout.LayoutParams.FILL_PARENT,
	                    LinearLayout.LayoutParams.WRAP_CONTENT);
		lp1.weight = 8;
		ll.addView(label, lp1);
		LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
		lp2.weight = 2;
		ll.addView(sb, lp2);
		layout.addView(ll);
	}
	
	protected void initSensor(){
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		shockSensor = new ShockSensor();
		shockSensor.addShockSensorListener(this);
	}
	
	public void addTp(int count){
		// Trailに情報追加
		if(logging.isChecked())	trailView.addTp(count, true, x1,y1,z1,x2,y2,z2,attitude[0],attitude[1],attitude[2]);
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
			SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
			SensorManager.getOrientation(rotationMatrix, attitude);
		}
		
		shockSensor.input(event.values, System.nanoTime());
	}

	@Override
	public void onShocked(ShockEvent event) {
		shockCount++;
		//Log.d(TAG, "Shock!!!!!");
		util.setText(shockSensorLabel, "Shock!!!"+shockCount);
	}
}