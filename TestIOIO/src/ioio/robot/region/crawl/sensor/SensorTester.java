package ioio.robot.region.crawl.sensor;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.part.sensor.ArduinoSensor;
import ioio.robot.robot.CrawlRobot;
import ioio.robot.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import trash.shocksensor.ShockEvent;
import trash.shocksensor.ShockSensor;
import trash.shocksensor.ShockSensorListener;
import android.content.Context;
import android.graphics.Color;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SensorTester {
	Util util;
	CrawlRobot parent;
	public final static String TAG = "SensorTester";
	protected final static float RAD2DEG = (float)(180/Math.PI);
	protected final static float DEG2RAD = (float)(Math.PI/180.0);
    private ScheduledExecutorService[] ses = null;
    
    private final static boolean saveAccelsLog = false;
	
    private ShockSensor shockSensor;
    private ArduinoSensor sensorModule;
    private PoseAnalizer poseAnalizer;
    private AccelAnalizer accelAnalizer;
	
	float[] rotationMatrix = new float[9];
	float[] gravity = new float[3];
	float[] gyro = new float[3];
	float[] geomagnetic = new float[3];
	float[] attitude = new float[3];
	int[] accel = new int[3];
	float x1, y1, z1;
	float x2, y2, z2;
	float azimuth,pitch,roll;	// rad
	TrailPoint nowTp;

    private ArrayList<Integer[]> accelsList;
	
	TextView azimuthText;
	TextView pitchText;
	TextView rollText;

	TextView valueX;
	TextView valueY;
	TextView valueZ;
	
	//ToggleButton logging;
	private ToggleButton accel_log;
	Button startButton, stopButton, clearButton, setButton;
	TextView infoLabel, shockSensorLabel;
	private boolean trailFixed, isLogging, accel_logging;
	private int cycleCount;
	private int shockCount;
	
	TrailView trailView;
	AccelView accelView;
	
	private boolean isActive;
	private boolean sensorInited;

	private static final long SAMPLING_INTERVAL = 100; // ms
	private static final long SAMPLING_FREQ = 1000 / SAMPLING_INTERVAL; // Hz
	
	
	public SensorTester(Util util, CrawlRobot parent){
		this.util = util;
		this.parent = parent;
	    poseAnalizer = new PoseAnalizer();
	    accelAnalizer = new AccelAnalizer();
		sensorModule = new ArduinoSensor();
		initInfo();
		isLogging = false;
		accelsList = new ArrayList<Integer[]>();
	}
	
	private void initInfo(){
		nowTp = null;
	}

	private final Runnable task = new Runnable(){
		public void run() {
			boolean hasData = false;
		    //Log.d(TAG, "running...");
			
			if(!sensorInited)	return;
			
			try {
				hasData = getData();
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(!hasData)	return;
			
			azimuth = attitude[2] * DEG2RAD;
			pitch = attitude[0] * DEG2RAD;
			roll = attitude[1] * DEG2RAD;
			
		    // 生データをテキスト表示
			util.setText(azimuthText, Float.toString(attitude[2]));
			util.setText(pitchText, Float.toString(attitude[0]));
			util.setText(rollText, Float.toString(attitude[1]));
	
			// 直交座標に変換
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
			theta = Math.PI*0.5 - roll;
			phi = -Math.PI*0.5 - azimuth;
			x2 = (float)(Math.sin(theta) * Math.cos(phi));
			y2 = (float)(Math.sin(theta) * Math.sin(phi));
			z2 = (float)(Math.cos(theta));

			// 現在の情報
			trailView.setNowData(attitude[2], attitude[0], attitude[1],x1,y1,z1,x2,y2,z2);
			nowTp = trailView.getNowTp();
			
			// 加速度の表示
			float accelNorm = (float) Math.sqrt(accel[0]*accel[0] + accel[1]*accel[1] + accel[2]*accel[2]);
			accelView.addAccelNorm(accelNorm);
			util.setText(valueX, String.format("%d", accel[0]));
			util.setText(valueY, String.format("%d", accel[1]));
			util.setText(valueZ, String.format("%d", accel[2]));
			if(accel_logging)	addAccels(accel);
			
			// 判定
			util.setText(infoLabel,
					poseAnalizer.getPoseInfo(getNowTpType(), getPitchDifference())
					+" / "+ accelAnalizer.getAccelInfo(getNowTpType(), accelView.getPeak(), accelView.getPeakVal(), accelView.getRange()));
		}
	};

	
	public LinearLayout getTextLayout(Context context) {
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		// 結果の数値を表示するtextview
		LinearLayout tables = new LinearLayout(context);
		tables.setOrientation(LinearLayout.HORIZONTAL);

        TableLayout.LayoutParams lp = new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams lp2 = new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
        lp2.weight = 1;
        tables.setWeightSum(2);
		
		TableLayout tl1 = new TableLayout(context);
		tl1.setWeightSum(3);
		TableRow[] trs1 = new TableRow[3];
		for(int i=0; i<trs1.length; i++){
			trs1[i] = new TableRow(context);
			trs1[i].setWeightSum(2);
			tl1.addView(trs1[i], lp);
		}
		azimuthText = new TextView(context);
		azimuthText.setText("--");
		trs1[0].addView(makeTextView(context, "azimuth"), lp2);
		trs1[0].addView(azimuthText, lp2);
		pitchText = new TextView(context);
		pitchText.setText("--");
		trs1[1].addView(makeTextView(context, "pitch"), lp2);
		trs1[1].addView(pitchText, lp2);
		rollText = new TextView(context);
		rollText.setText("--");
		trs1[2].addView(makeTextView(context, "roll"), lp2);
		trs1[2].addView(rollText, lp2);

		tables.addView(tl1, lp);
		
		TableLayout tl2 = new TableLayout(context);
		tl2.setWeightSum(3);
		TableRow[] trs2 = new TableRow[3];
		for(int i=0; i<trs2.length; i++){
			trs2[i] = new TableRow(context);
			trs2[i].setWeightSum(2);
			tl2.addView(trs2[i], lp);
		}
		valueX = new TextView(context);
		valueX.setText("--");
		trs2[0].addView(makeTextView(context, "x"), lp2);
		trs2[0].addView(valueX, lp2);
		valueY = new TextView(context);
		valueY.setText("--");
		trs2[1].addView(makeTextView(context, "y"), lp2);
		trs2[1].addView(valueY, lp2);
		valueZ = new TextView(context);
		valueZ.setText("--");
		trs2[2].addView(makeTextView(context, "z"), lp2);
		trs2[2].addView(valueZ, lp2);
		
		tables.addView(tl2, lp);
		
		layout.addView(tables);

		// 判定結果を表示するtextView
		infoLabel = new TextView(context);
		infoLabel.setTextColor(Color.GRAY);
		layout.addView(infoLabel);
		
		// ショックセンサーの反応を表示するtextView
		shockSensorLabel = new TextView(context);
		//layout.addView(shockSensorLabel);
		
		return layout;
	}
	
	public LinearLayout getTrailViewLayout(Context context) {
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 2, 5, 2);
        
		trailView = new TrailView(context);
		layout.addView(trailView);

		accelView = new AccelView(context, SAMPLING_FREQ);
		layout.addView(accelView);
		
		// TrailViewの操作パネル
		layout.addView(trailView.getLayout(context));
		
		//initSensor();
		
		return layout;
	}
	
	public LinearLayout getTrailControllerLayout(final Context context){
		LinearLayout buttonLayout = new LinearLayout(context);
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
		buttonLayout.setPadding(0, 1, 0, 1);
		/*
		// 記録する・しないを切り替えるtogglebutton
		logging = new ToggleButton(context);
		logging.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)	trailView.onResume();
				else			trailView.onPause();
			}
		});
		buttonLayout.addView(logging);
		*/
		// startボタン
		startButton = new Button(context);
		startButton.setText("start");
		startButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				//trailView.onResume();
				isLogging = true;
				try {
					parent.wheel.goForward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		});
		buttonLayout.addView(startButton);
		// stopボタン
		stopButton = new Button(context);
		stopButton.setText("stop");
		stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				//trailView.onPause();
				isLogging = false;
				parent.wheel.stop();
			}
		});
		buttonLayout.addView(stopButton);
		stopButton.setEnabled(false);
		// setボタン
		setButton = new Button(context);
		setButton.setText("set");
		setButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				stopButton.performClick();
				trailView.setTrail();
				trailFixed = true;
				startButton.setEnabled(false);
			}
		});
		buttonLayout.addView(setButton);
		// clearボタン
		clearButton = new Button(context);
		clearButton.setText("clear");
		clearButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				trailView.clearTrail();
				trailFixed = false;
				initInfo();
				startButton.setEnabled(true);
				cycleCount = 0;
			}
		});
		buttonLayout.addView(clearButton);
		
		// ログ出力ボタン
		accel_log = new ToggleButton(context);
		accel_log.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					initAccels();
					accel_logging = true;
				}else{
					accel_logging = false;
					flushAccels(context);
				}
			}
		});
		buttonLayout.addView(accel_log);
		if(!saveAccelsLog)	accel_log.setVisibility(View.GONE);
		
		return buttonLayout;
	}
	
	
	private TextView makeTextView(Context context, String text){
		TextView tv = new TextView(context);
		tv.setText(text);
		return tv;
	}
	
	
	
	public void openPins(IOIO ioio, int pinNum1, int pinNum2) throws ConnectionLostException, InterruptedException, IOException{
		sensorModule.openPins(ioio, pinNum1, pinNum2);
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
		sensorInited = true;
        // 100msごとにtaskを実行する
        ses[0].scheduleAtFixedRate(task, 0L, SAMPLING_INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	
	public void disactivate() throws ConnectionLostException {
		Log.i(TAG, "disactivate");
		isActive = false;
		sensorInited = false;
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

	public void incCount(){
		if(isLogging){
			cycleCount++;
			addTp(cycleCount, true);
		}else if(trailFixed){
			if(cycleCount==trailView.getTpLength())	return;
			cycleCount++;
			trailView.selectTp(cycleCount);
		}
	}
	public void decCount(){
		if(isLogging){
			if(cycleCount!=0){
				cycleCount--;
				addTp(cycleCount, false);
			}else{
				addTp(-1, false);
			}
		}else if(trailFixed){
			if(cycleCount==0)	return;
			cycleCount--;
			trailView.selectTp(cycleCount);
		}
	}
	public void addTp(int count, boolean forward){
		// Trailに情報追加
		trailView.addTp(count, forward, x1,y1,z1,x2,y2,z2,azimuth,pitch,roll);
	}
	
	private boolean getData() throws ConnectionLostException, InterruptedException, IOException{
		boolean success = sensorModule.getData(attitude, accel);
		return success;
	}
	

	public void initAccels(){
    	synchronized (accelsList) {
    		accelsList.clear();
    	}
	}
    public void addAccels(int[] accel){
    	Integer[] accels = {accel[0], accel[1], accel[2]};
    	synchronized (accelsList) {
    		accelsList.add(accels);
    	}
    }
    public void flushAccels(Context context){
    	StringBuilder sb = new StringBuilder();
    	synchronized (accelsList) {
        	for(Integer[] a : accelsList){
        		sb.append(a[0]+" "+a[1]+" "+a[2]+"\n");
        	}
		}
    	String text = sb.toString();
    	Time time = new Time("Asia/Tokyo");
    	time.setToNow();
    	
    	String fname = "accels/accels_"+time.year+"_"+(time.month+1)+"_"+time.monthDay+"_"
    					+time.hour+"_"+time.minute+"_"+time.second;
    	util.saveText(context, fname, text);
    	initAccels();
    }
    
	
	public void setSpeed(float speed){
		// do nothing;
	}

	public int getNowTpType() {
		return nowTp.type;
	}
	public int getNowTpIndex() {
		return cycleCount;
	}
	public float getPitchDifference(){
		float dif = getPitch()-nowTp.pitch;
		//Log.i(TAG, "now pitch: " + getPitch() + ", tp pitch: " + nowTp.pitch);
		while(dif>Math.PI)	dif -= 2*Math.PI;
		while(dif<-Math.PI)	dif += 2*Math.PI;
		return dif;
	}
	public float getAzimuth(){
		return azimuth;
	}
	public float getPitch(){
		return pitch;
	}
	public float getRoll(){
		return roll;
	}

	
	public void onResume(){
		accelView.onResume();
		/*
        // debug
		ses = new ScheduledExecutorService[1];
		for(int i=0; i<ses.length; i++)	ses[i] = Executors.newSingleThreadScheduledExecutor();
		sensorInited = true;
        ses[0].scheduleAtFixedRate(testTask, 0L, SAMPLING_INTERVAL, TimeUnit.MILLISECONDS);
        */
	}
	
	public void onPause(){
		accelView.onPause();
	}

	/** debug用 **/
	private final Runnable testTask = new Runnable(){
		public void run(){
			// 判定
			util.setText(infoLabel,
					//poseAnalizer.getPoseInfo(getNowTpType(), getPitchDifference())
					//+" / "+ 
					accelAnalizer.getAccelInfo(TrailPoint.SHOLDER, accelView.getPeak(), accelView.getPeakVal(), accelView.getRange()));
		}
	};

}