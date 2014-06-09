package ioio.robot.region.crawl.sensor;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.part.sensor.ArduinoSensor;
import ioio.robot.robot.CrawlRobot;
import ioio.robot.util.Util;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import trash.shocksensor.ShockEvent;
import trash.shocksensor.ShockSensor;
import trash.shocksensor.ShockSensorListener;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SensorTester {
	Util util;
	CrawlRobot parent;
	public final static String TAG = "SensorTester";
	protected final static float RAD2DEG = (float)(180/Math.PI);
	protected final static float DEG2RAD = (float)(Math.PI/180.0);
    private ScheduledExecutorService[] ses = null;
	
    private ShockSensor shockSensor;
    private ArduinoSensor sensorModule;
	
	float[] rotationMatrix = new float[9];
	float[] gravity = new float[3];
	float[] gyro = new float[3];
	float[] geomagnetic = new float[3];
	float[] attitude = new float[3];
	float x1, y1, z1;
	float x2, y2, z2;
	float azimuth,pitch,roll;	// rad
	TrailPoint nowTp;
	
	TextView azimuthText;
	TextView pitchText;
	TextView rollText;

	TextView valueX;
	TextView valueY;
	TextView valueZ;
	
	//ToggleButton logging;
	Button startButton, stopButton, clearButton, setButton;
	TextView infoLabel, shockSensorLabel;
	private boolean trailFixed, isLogging;
	private int cycleCount;
	private int shockCount;
	
	TrailView trailView;
	
	private boolean isActive;
	private boolean sensorInited;

	private static final float THRESHOLD_BACK1 = 30 * DEG2RAD;
	private static final float THRESHOLD_BACK2 = 70 * DEG2RAD;
	private static final float THRESHOLD_ARM1 = 30 * DEG2RAD;
	private static final float THRESHOLD_ARM2 = 60 * DEG2RAD;
	
	
	public SensorTester(Util util, CrawlRobot parent){
		this.util = util;
		this.parent = parent;
		sensorModule = new ArduinoSensor();
		initInfo();
		isLogging = false;
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
			
		    // ���f�[�^���e�L�X�g�\��
			util.setText(azimuthText, Float.toString(attitude[2]));
			util.setText(pitchText, Float.toString(attitude[0]));
			util.setText(rollText, Float.toString(attitude[1]));
	
			// �������W�ɕϊ�
			double theta, phi;
			// �O
			theta = Math.PI*0.5 - pitch;
			phi = -azimuth;
			x1 = (float)(Math.sin(theta) * Math.cos(phi));
			y1 = (float)(Math.sin(theta) * Math.sin(phi));
			z1 = (float)(Math.cos(theta));
			util.setText(valueX, String.format("%.3f", x1));
			util.setText(valueY, String.format("%.3f", y1));
			util.setText(valueZ, String.format("%.3f", z1));
			// �E
			theta = Math.PI*0.5 - roll;
			phi = -Math.PI*0.5 - azimuth;
			x2 = (float)(Math.sin(theta) * Math.cos(phi));
			y2 = (float)(Math.sin(theta) * Math.sin(phi));
			z2 = (float)(Math.cos(theta));

			// ���݂̏��
			trailView.setNowData(attitude[2], attitude[0], attitude[1],x1,y1,z1,x2,y2,z2);
			nowTp = trailView.getNowTp();
			
			// ����
			float dif = getPitchDifference();
        	switch(getNowTpType()){
        	case TrailPoint.NO_TYPE:
        		util.setText(infoLabel, "");
        		break;
        	case TrailPoint.BACK:
        	case TrailPoint.SHOLDER:
        		if(dif < -THRESHOLD_BACK2)			util.setText(infoLabel, "���Ԃ�");
        		else if(dif < -THRESHOLD_BACK1)		util.setText(infoLabel, "�O�X�p��");
        		else if(dif > THRESHOLD_BACK2)		util.setText(infoLabel, "����");
        		else if(dif > THRESHOLD_BACK1)		util.setText(infoLabel, "��X�p��");
        		else								util.setText(infoLabel, "����"+dif);
        		break;
        	case TrailPoint.ARM:
        		if(dif < -THRESHOLD_ARM2)			util.setText(infoLabel, "�r�������Ă���");
        		else if(dif < -THRESHOLD_ARM1)		util.setText(infoLabel, "�r����������");
        		else if(dif > THRESHOLD_ARM2)		util.setText(infoLabel, "�r���グ�Ă���");
        		else if(dif > THRESHOLD_ARM1)		util.setText(infoLabel, "�r���グ����");
        		else								util.setText(infoLabel, "�ӂ�"+dif);
        		break;
        	}
		}
	};

	
	public LinearLayout getTextLayout(Context context) {
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		// ���ʂ̐��l��\������textview
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

		// ���茋�ʂ�\������textView
		infoLabel = new TextView(context);
		infoLabel.setTextColor(Color.GRAY);
		layout.addView(infoLabel);
		
		// �V���b�N�Z���T�[�̔�����\������textView
		shockSensorLabel = new TextView(context);
		//layout.addView(shockSensorLabel);
		
		return layout;
	}
	
	public LinearLayout getTrailViewLayout(Context context) {
		trailView = new TrailView(context);

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 2, 5, 2);
		layout.addView(trailView);
		
		// TrailView�̑���p�l��
		layout.addView(trailView.getLayout(context));
		
		//initSensor();
		
		return layout;
	}
	
	public LinearLayout getTrailControllerLayout(Context context){
		LinearLayout buttonLayout = new LinearLayout(context);
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
		buttonLayout.setPadding(0, 1, 0, 1);
		/*
		// �L�^����E���Ȃ���؂�ւ���togglebutton
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
		// start�{�^��
		startButton = new Button(context);
		startButton.setText("start");
		startButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				trailView.onResume();
				isLogging = true;
				try {
					parent.goForward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		});
		buttonLayout.addView(startButton);
		// stop�{�^��
		stopButton = new Button(context);
		stopButton.setText("stop");
		stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				trailView.onPause();
				isLogging = false;
				parent.stop();
			}
		});
		buttonLayout.addView(stopButton);
		stopButton.setEnabled(false);
		// set�{�^��
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
		// clear�{�^��
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
		
		return buttonLayout;
	}
	
	
	private TextView makeTextView(Context context, String text){
		TextView tv = new TextView(context);
		tv.setText(text);
		return tv;
	}
	
	
	
	public void openPins(IOIO ioio, int pinNum1, int pinNum2) throws ConnectionLostException, InterruptedException, IOException{
		sensorModule.init(ioio, pinNum1, pinNum2);
	}

	public void activate() throws ConnectionLostException {
		Log.i(TAG, "activate");
		isActive = true;
		sensorModule.activate();
        // �^�C�}�[���쐬����
		ses = new ScheduledExecutorService[1];
		for(int i=0; i<ses.length; i++){
			ses[i] = Executors.newSingleThreadScheduledExecutor();
		}
		sensorInited = true;
        // 100ms���Ƃ�task�����s����
        ses[0].scheduleAtFixedRate(task, 0L, 300L, TimeUnit.MILLISECONDS);
	}
	
	
	public void disactivate() throws ConnectionLostException {
		Log.i(TAG, "disactivate");
		isActive = false;
		sensorInited = false;
		try {
			sensorModule.disactivate();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// �^�C�}�[���~����
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
		try {
			sensorModule.disconnected();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		// Trail�ɏ��ǉ�
		trailView.addTp(count, forward, x1,y1,z1,x2,y2,z2,azimuth,pitch,roll);
	}
	
	private boolean getData() throws ConnectionLostException, InterruptedException, IOException{
		boolean success = sensorModule.getData(attitude);
		return success;
	}
	
	public void setSpeed(float speed){
		// do nothing;
	}

	public int getNowTpType() {
		return nowTp.type;
	}
	public float getPitchDifference(){
		float dif = getPitch()-nowTp.pitch;
		while(dif>Math.PI)	dif -= Math.PI;
		while(dif<-Math.PI)	dif += Math.PI;
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
	
}