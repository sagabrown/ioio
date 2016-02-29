package ioio.robot.robot;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.momot.MainActivity;
import ioio.momot.R;
import ioio.robot.mode.crawl.AutoEmoMode;
import ioio.robot.mode.crawl.InteractionMode;
import ioio.robot.mode.crawl.PointOutMode;
import ioio.robot.mode.crawl.ShowInfoMode;
import ioio.robot.mode.crawl.TestMode;
import ioio.robot.part.light.FullColorLED;
import ioio.robot.part.light.LED;
import ioio.robot.part.motor.DCMotor;
import ioio.robot.part.motor.Motor;
import ioio.robot.part.motor.SG90;
import ioio.robot.part.motor.ServoMotor;
import ioio.robot.part.sensor.TouchSensor;
import ioio.robot.region.crawl.Ears;
import ioio.robot.region.crawl.Eyes;
import ioio.robot.region.crawl.Wheel;
import ioio.robot.region.crawl.sensor.SensorTester;
import ioio.robot.region.crawl.sensor.SpeedMater;
import ioio.robot.region.crawl.sensor.TrailPoint;
import ioio.robot.region.crawl.sensor.TrailView;
import ioio.robot.util.Util;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;


public class CrawlRobot implements Robot {
	private final static String TAG = "CrawlRobot";
	private Util util;
	private SharedPreferences sharedPreferences;
	
	// settingで読み込むもの
	private boolean usingMode = true;
	private boolean gettingTrail = true;
	
	private final static String FOR_TEXT = "for";
	private final static String BACK_TEXT = "back";
	private final static String STOP_TEXT = "○";
	
	public Wheel wheel;
	public Ears ears;
	public Eyes eyes;
	public TouchSensor[] touchSensor;

	/**
	pin 1, 2	: i2cセンサ通信
	pin 4		: 回転数入力
	pin 5, 6	: DCモータ
	pin 7		: サーボモータ
	pin 9		: スピーカー?
	pin 12-14	: LED
	pin 10, 11	: リミットスイッチ
	**/
	private final static int speedMaterPinNum = 4;
	private final static int sensorPinNum = 1;
	private final static int[][] wheelPinNums = {{5, 6}};
	private final static int[][] earsPinNums = {{7}};
	private final static int[][] eyesPinNums = {{12,13,14}};
	private final static int[][] touchSensorPinNum = {{10}, {11}};
	
	private SpeedMater speedMater;
	public SensorTester sensor;
    // 基本的には設定から読み込む
	private int distPerCycle = 48;	// モーター1回転で進む距離[mm]
	private int slitNum = 4;
	
	private TestMode testMode;
	private AutoEmoMode autoEmoMode;
	private ShowInfoMode showInfoMode;
	private PointOutMode pointOutMode;
	private InteractionMode interactionMode;
	
	private LinearLayout layout;
	private LinearLayout modeSelectLayout, manualContollerLayout, sensorTextLayout, trailControllerLayout, trailViewLayout;
	private FrameLayout sensorLayout;
	private ToggleButton autoButton, autoEmoButton, showInfoButton, pointOutButton, interactionButton;
	private Button backButton, forwardButton, stopButton;
	private Button[] emoButton;
    private boolean isActive, isAuto;
	
	/** コンストラクタ **/
	public CrawlRobot(Util util, SharedPreferences sharedPreferences) {
		super();
		this.util = util;
		this.sharedPreferences = sharedPreferences;
		
		wheel = new Wheel(util, this);
		ears = new Ears(util);
		eyes = new Eyes(util);
		speedMater = new SpeedMater(util, distPerCycle, slitNum, this);
		sensor = new SensorTester(util, this);
		touchSensor = new TouchSensor[2];
		touchSensor[0] = new TouchSensor(util, "head");
		touchSensor[1] = new TouchSensor(util, "back");

		testMode = new TestMode();
		autoEmoMode = new AutoEmoMode();
		showInfoMode = new ShowInfoMode();
		pointOutMode = new PointOutMode();
		interactionMode = new InteractionMode();
		init();
	}

	/** 初期設定 **/
	private void init(){
		testMode.setParams(util, this);
		autoEmoMode.setParams(util, this);
		showInfoMode.setParams(util, this);
		pointOutMode.setParams(util, this);
		interactionMode.setParams(util, this);
		wheel.init();
		ears.init();
		eyes.init();
		// スピードメータのセット
		wheel.setSpeedMater(speedMater);
		// センサ
		sensor.initAccels();
		sensor.incCount();
		for(TouchSensor t : touchSensor)	t.init();
	}

	@Override
	/** ロボットの操作パネルを作って返す **/
	public LinearLayout getLayout(Context parent){
        // 親のアクティビティに動的レイアウトを作成する
        layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        // オート操作のレイアウトを登録
        modeSelectLayout = getAutoLayout(parent);
        layout.addView(modeSelectLayout);
        
        // マニュアル操作パネルを作成
        manualContollerLayout = new LinearLayout(parent);
        manualContollerLayout.setOrientation(LinearLayout.VERTICAL);
        manualContollerLayout.setBackgroundColor(Color.DKGRAY);
		manualContollerLayout.setVisibility(View.GONE);
        // - くるまの操作パネルを登録
        manualContollerLayout.addView(wheel.getLayout(parent), LayoutParams.FILL_PARENT);
        // - 耳の操作パネルを登録
        manualContollerLayout.addView(ears.getLayout(parent));
        // - 目の操作パネルを登録
        manualContollerLayout.addView(eyes.getLayout(parent));

        // emotionの操作パネルを登録
        layout.addView(getEmoOperationLayout(parent));
        // 表示きりかえ+簡易操作のレイアウトを登録
        layout.addView(getShowModeLayout(parent));
        // マニュアル操作パネルを登録
        layout.addView(manualContollerLayout);

        // trail controlパネルの登録
        trailControllerLayout = sensor.getTrailControllerLayout(parent);
        layout.addView(trailControllerLayout);
        
		// TrailViewとセンサ表示を重ねて表示
		sensorLayout = new FrameLayout(parent);
        
			// TrailView表示のパネルを登録
	        trailViewLayout = sensor.getTrailViewLayout(parent);
	        sensorLayout.addView(trailViewLayout);
	        
			// センサーテキスト表示のパネルを登録
			sensorTextLayout = new LinearLayout(parent);
			sensorTextLayout.setOrientation(LinearLayout.VERTICAL);
			sensorTextLayout.setPadding(10, 5, 10, 0);
			sensorLayout.addView(sensorTextLayout);
			// - スピードメータのパネルを登録
			sensorTextLayout.addView(speedMater.getLayout(parent));
			// - 9軸センサーのパネルを登録
			sensorTextLayout.addView(sensor.getTextLayout(parent));
		
		layout.addView(sensorLayout);
		
		return layout;
	}
	


	/** 表示きりかえ＋簡易操作パネルを生成して返す **/
	public LinearLayout getShowModeLayout(Context parent){
		LinearLayout layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        // manual操作パネル表示切り替え
        CheckBox manualShowCheck = new CheckBox(parent);
        manualShowCheck.setText("controller  ");
        manualShowCheck.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					manualContollerLayout.setVisibility(View.VISIBLE);
				}else{
					manualContollerLayout.setVisibility(View.GONE);
				}
			}
        });
        manualShowCheck.setBackgroundColor(Color.DKGRAY);
        layout.addView(manualShowCheck);
        manualShowCheck.setChecked(false);

		// backボタン
		backButton = new Button(parent);
		backButton.setText(BACK_TEXT);
		backButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					wheel.goBackward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		layout.addView(backButton);
		// stopボタン
		stopButton = new Button(parent);
		stopButton.setText(STOP_TEXT);
		stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				wheel.stop();
			}
		});
		layout.addView(stopButton);
		// forwardボタン
		forwardButton = new Button(parent);
		forwardButton.setText(FOR_TEXT);
		forwardButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					wheel.goForward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		layout.addView(forwardButton);
        
		return layout;
	}

	/** auto操作パネルを生成して返す **/
	public LinearLayout getAutoLayout(Context parent){
		LinearLayout autoLayout = new LinearLayout(parent);
        autoLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
		
        autoLayout.setWeightSum(3);

        // オート切り替えのボタン
        autoButton = testMode.getOnOffButton(parent);
        autoLayout.addView(autoButton,lp);
        // オートemotion切り替えのボタン
        autoEmoButton = autoEmoMode.getOnOffButton(parent);
        autoLayout.addView(autoEmoButton,lp);
        // 判定結果提示切り替えのボタン
        showInfoButton = showInfoMode.getOnOffButton(parent);
        //autoLayout.addView(showInfoButton,lp);
        // 指摘モード切り替えのボタン
        pointOutButton = pointOutMode.getOnOffButton(parent);
        autoLayout.addView(pointOutButton,lp);
        // インタラクションモード切り替えのボタン
        interactionButton = interactionMode.getOnOffButton(parent);
        autoLayout.addView(interactionButton,lp);
        
		return autoLayout;
	}

	/** emotion操作パネルを生成して返す **/
	public LinearLayout getEmoOperationLayout(Context parent){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
		LinearLayout emoLayout = new LinearLayout(parent);
        emoLayout.setOrientation(LinearLayout.HORIZONTAL);
        emoLayout.setWeightSum(4);
        
		emoButton = new Button[4];
		for(int i=0; i<emoButton.length; i++){
			emoButton[i] = new Button(parent);
			emoLayout.addView(emoButton[i], lp);
		}
		emoButton[0].setText("nomal");
        emoButton[0].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				stand();
			}
        });
		emoButton[1].setText("happy");
		emoButton[1].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					happy();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
        });
		emoButton[2].setText("angry");
		emoButton[2].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				angry();
			}
        });
		emoButton[3].setText("sad");
		emoButton[3].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					sad();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
        });
		for(Button b : emoButton)	util.setEnabled(b, false);
        
		return emoLayout;
	}
	

	@Override
	/** ピンを開いて各モーターに対応させる **/
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException, InterruptedException{
		int cnt = startPin;
		// 9軸センサの入力ピン(pin1,2)
		try {
			sensor.openPins(ioio, new int[]{sensorPinNum});
		} catch (IOException e) {
			e.printStackTrace();
		}
        // ピンにモーターを対応させる
		wheel.openPins(ioio, wheelPinNums);
		ears.openPins(ioio, earsPinNums);
		// スピーカー
		//ioio.openPwmOutput(7, 500).setDutyCycle(0.5f);
		// スピードメータの入力ピン
		speedMater.openPins(ioio, speedMaterPinNum);
		// 目
		eyes.openPins(ioio, eyesPinNums);
		// タッチセンサ
		touchSensor[0].openPins(ioio, touchSensorPinNum[0]);
		touchSensor[1].openPins(ioio, touchSensorPinNum[1]);
		
		return cnt;
	}

	@Override
	/** onにする **/
	public void activate() throws ConnectionLostException {
		wheel.activate();
		ears.activate();
		eyes.activate();
		speedMater.activate();
		//sensor.activate();
		for(TouchSensor t : touchSensor)	t.activate();
		for(Button b : emoButton)	util.setEnabled(b, true);
		isActive = true;
	}
	@Override
	/** offにする **/
	public void disactivate() throws ConnectionLostException {
		wheel.disactivate();
		ears.disactivate();
		eyes.disactivate();
		speedMater.disactivate();
		sensor.disactivate();
		for(TouchSensor t : touchSensor)	t.disactivate();
		for(Button b : emoButton)	util.setEnabled(b, false);
		isActive = false;
	}
	@Override
	/** 接続解除されたときの処理 **/
	public void disconnected() throws ConnectionLostException {
		wheel.disconnected();
		ears.disconnected();
		eyes.disconnected();
		speedMater.disconnected();
		sensor.disconnected();
		for(TouchSensor t : touchSensor)	t.disconnected();
		for(Button b : emoButton)	util.setEnabled(b, false);
		isActive = false;
	}


	/** 平常 **/
	public void stand(){
		float[] pink = new float[]{1.0f, 0.4f, 0.4f};
		
		eyes.setColor(pink);

		try {
			ears.backwardSlowly();
			Thread.sleep(1000);
			ears.swing();
			Thread.sleep(1000);
			ears.backwardSlowly();
			Thread.sleep(1000);
			ears.swing();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		eyes.green();
		ears.reset();
		*/
	}
	/** 喜ぶ 
	 * @throws InterruptedException **/
	public void happy() throws InterruptedException{
		eyes.green();
		ears.swing();
	}
	/** 怒る **/
	public void angry(){
		eyes.red();
		ears.forward();
	}
	/** 悲しむ 
	 * @throws InterruptedException **/
	public void sad() throws InterruptedException{
		eyes.blue();
		ears.backwardSlowly();
	}
	

	/** 回転数更新のときの処理 **/
	public void incCount(){
		sensor.incCount();
	}
	public void decCount(){
		sensor.decCount();
	}
	/** センサーテスタにスピードメータの値を伝える **/
	@Override
	public void setSpeed(float speed) {
		sensor.setSpeed(speed);
	}
	
	
	/** 設定の反映 **/
	private void applySettings(){
		// 自動制御モードを使うか
		usingMode = sharedPreferences.getBoolean("modeSelect", true);
		if(usingMode)	modeSelectLayout.setVisibility(View.VISIBLE);
		else			modeSelectLayout.setVisibility(View.GONE);
		
		// 経路情報を使うか
		gettingTrail = sharedPreferences.getBoolean("trailSelect", true);
        if(gettingTrail){
        	trailControllerLayout.setVisibility(View.VISIBLE);
        	sensorLayout.setVisibility(View.VISIBLE);
        }else{
        	trailControllerLayout.setVisibility(View.GONE);
        	sensorLayout.setVisibility(View.GONE);
        }
        
        // 一回転で進む距離とスリットの数
        try{
        	speedMater.setDistPerCycle(sharedPreferences.getInt("distPerCycle", distPerCycle));
        	speedMater.setSlitNum(sharedPreferences.getInt("slitNum", slitNum));
        }catch(ClassCastException e){
        	e.printStackTrace();
        }
	}

	@Override
	public void onResume() {
		applySettings();
		sensor.onResume();
	}
	@Override
	public void onPause() {
		sensor.onPause();
		testMode.onPause();
		pointOutMode.onPause();
		showInfoMode.onPause();
		autoEmoMode.onPause();
		interactionMode.onPause();
	}
}
