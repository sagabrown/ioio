package ioio.robot.sensor;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.sensor.shocksensor.ShockEvent;
import ioio.robot.sensor.shocksensor.ShockSensor;
import ioio.robot.sensor.shocksensor.ShockSensorListener;
import ioio.robot.util.Util;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SensorTester {
	Util util;
	Activity parent;
	public final static String TAG = "SensorTester";
	protected final static float RAD2DEG = (float)(180/Math.PI);
    private ScheduledExecutorService[] ses = null;
	
    private ShockSensor shockSensor;
    private ArduinoSensor sensorModule;
	
	float[] rotationMatrix = new float[9];
	float[] gravity = new float[3];
	float[] gyro = new float[3];
	float[] geomagnetic = new float[3];
	float[] attitude = new float[3];
	
	LinearLayout layout;
	
	TextView azimuthText;
	TextView pitchText;
	TextView rollText;

	TextView valueX;
	TextView valueY;
	TextView valueZ;
	
	ToggleButton logging;
	TextView shockSensorLabel;
	private int shockCount;
	
	TrailView trailView;
	SeekBar seekBarX, seekBarY, seekBarZ;
	SeekBar seekBarLaX, seekBarLaY, seekBarLaZ;
	
	private boolean isActive;
	
	public SensorTester(Util util){
		this.util = util;
		sensorModule = new ArduinoSensor();
	}

	private final Runnable task = new Runnable(){
		public void run() {
		    Log.d(TAG, "running...");
			
			try {
				getData();
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			float azimuth = attitude[2];
			float pitch = attitude[0];
			float roll = attitude[1];
			
		    // 生データをテキスト表示
			util.setText(azimuthText, Float.toString(azimuth));
			util.setText(pitchText, Float.toString(pitch));
			util.setText(rollText, Float.toString(roll));
	
			// 直交座標に変換
			float x1, y1, z1;
			float x2, y2, z2;
			double theta, phi;
			// 前
			theta = Math.PI*0.5 - pitch;
			phi = -azimuth;
			x1 = (float)(Math.sin(theta) * Math.cos(phi));
			y1 = (float)(Math.sin(theta) * Math.sin(phi));
			z1 = (float)(Math.cos(theta));
			util.setText(valueX, String.format("%.3f", x1));
			util.setText(valueY, String.format("%.3f", y1));
			util.setText(valueZ, String.format("%.3f", z1));
			// 右
			theta = Math.PI*0.5 + roll;
			phi = -Math.PI*0.5 - azimuth;
			x2 = (float)(Math.sin(theta) * Math.cos(phi));
			y2 = (float)(Math.sin(theta) * Math.sin(phi));
			z2 = (float)(Math.cos(theta));
			
			// Trailに情報追加
			if(logging.isChecked())	trailView.addTp(x1,y1,z1,x2,y2,z2);

			// 現在の情報
			trailView.setNowData(azimuth, pitch, roll,
					x1,y1,z1,x2,y2,z2);
		}
	};
	
	public LinearLayout getLayout(Context context) {
		trailView = new TrailView(context);

		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		addViews(context, layout);
		layout.addView(trailView);
		
		// TrailViewの操作パネル
		makeLookSeekBar(context, layout, seekBarX, 100, 0, "from:X");
		makeLookSeekBar(context, layout, seekBarY, 100, 1, "from:Y");
		makeLookSeekBar(context, layout, seekBarZ, 100, 2, "from:Z");
		makeLookSeekBar(context, layout, seekBarLaX, 100, 3, "  at:X");
		makeLookSeekBar(context, layout, seekBarLaY, 100, 4, "  at:Y");
		makeLookSeekBar(context, layout, seekBarLaZ, 100, 5, "  at:Z");
		
		//initSensor();
		
		return layout;
	}
	
	
	protected void addViews(Context context, LinearLayout layout){
		/*
		azimuthText = (TextView)findViewById(R.id.azimuth);
		pitchText = (TextView)findViewById(R.id.pitch);
		rollText = (TextView)findViewById(R.id.roll);
		valueX = (TextView)findViewById(R.id.valueX);
		valueY = (TextView)findViewById(R.id.valueY);
		valueZ = (TextView)findViewById(R.id.valueZ);
		logging = (ToggleButton)findViewById(R.id.logging);
		*/

		// 結果の数値を表示するtextview
		LinearLayout tables = new LinearLayout(context);
		tables.setOrientation(LinearLayout.HORIZONTAL);
		
		TableLayout tl1 = new TableLayout(context);
		TableRow[] trs1 = new TableRow[3];
		for(int i=0; i<trs1.length; i++){
			trs1[i] = new TableRow(context);
			tl1.addView(trs1[i]);
		}
		azimuthText = new TextView(context);
		azimuthText.setText("--");
		trs1[0].addView(makeTextView(context, "azimuth"));
		trs1[0].addView(azimuthText);
		pitchText = new TextView(context);
		pitchText.setText("--");
		trs1[1].addView(makeTextView(context, "pitch"));
		trs1[1].addView(pitchText);
		rollText = new TextView(context);
		rollText.setText("--");
		trs1[2].addView(makeTextView(context, "roll"));
		trs1[2].addView(rollText);

		tables.addView(tl1);
		
		TableLayout tl2 = new TableLayout(context);
		TableRow[] trs2 = new TableRow[3];
		for(int i=0; i<trs2.length; i++){
			trs2[i] = new TableRow(context);
			tl2.addView(trs2[i]);
		}
		valueX = new TextView(context);
		valueX.setText("--");
		trs2[0].addView(makeTextView(context, "x"));
		trs2[0].addView(valueX);
		valueY = new TextView(context);
		valueY.setText("--");
		trs2[1].addView(makeTextView(context, "y"));
		trs2[1].addView(valueY);
		valueZ = new TextView(context);
		valueZ.setText("--");
		trs2[2].addView(makeTextView(context, "z"));
		trs2[2].addView(valueZ);
		
		tables.addView(tl2);
		
		layout.addView(tables);
		
		// 記録する・しないを切り替えるtogglebutton
		logging = new ToggleButton(context);
		logging.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)	trailView.onResume();
				else			trailView.onPause();
			}
		});
		layout.addView(logging);
		// ショックセンサーの反応を表示するtextView
		shockSensorLabel = new TextView(context);
		layout.addView(shockSensorLabel);
	}
	
	private TextView makeTextView(Context context, String text){
		TextView tv = new TextView(context);
		tv.setText(text);
		return tv;
	}
	
	
	protected void makeLookSeekBar(Context context, LinearLayout layout, SeekBar sb, final int max, final int tag, String name){
		sb = new SeekBar(context);
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
		
		TextView label = new TextView(context);
		label.setText(name);
		LinearLayout ll = new LinearLayout(context);
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
	
	
	public void openPins(IOIO ioio, int pinNum1, int pinNum2) throws ConnectionLostException, InterruptedException{
		sensorModule.init(ioio, pinNum1, pinNum2);
	}

	public void activate() throws ConnectionLostException {
		Log.i(TAG, "activate");
		isActive = true;
		sensorModule.activate();
        // タイマーを作成する
		ses = new ScheduledExecutorService[1];
		for(int i=0; i<ses.length; i++){
			ses[i] = Executors.newSingleThreadScheduledExecutor();
		}
        // 100msごとにtaskを実行する
        ses[0].scheduleAtFixedRate(task, 0L, 100L, TimeUnit.MILLISECONDS);
	}
	
	
	public void disactivate() throws ConnectionLostException {
		isActive = false;
		sensorModule.disactivate();
		// タイマーを停止する
		if(ses == null)	return;
		for(ScheduledExecutorService s : ses){
			if(s != null){
				s.shutdown();
				s = null;
			}
		}
	}
	public void disconnected() throws ConnectionLostException {
		isActive = false;
		disactivate();
		sensorModule.disconnected();
	}
	
	
	private void getData() throws ConnectionLostException, InterruptedException, IOException{
		sensorModule.getData(attitude);

		/*
		int[] int_gravity = new int[3];
		int[] int_gyro = new int[3];
		int[] int_geomagnetic = new int[3];
		sensorModule.getMotion9(int_gravity, int_gyro, int_geomagnetic);
		
		for(int i=0; i<3; i++){
			gravity[i] = (float)int_gravity[i];
			gyro[i] = (float)int_gyro[i];
			geomagnetic[i] = (float)int_geomagnetic[i];
		}
		if(geomagnetic != null && gravity != null){
			SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
			SensorManager.getOrientation(rotationMatrix, attitude);
		}
		*/
	}
	
}