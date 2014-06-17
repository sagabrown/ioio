package ioio.robot.controller.robot;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.controller.motor.DCMotor;
import ioio.robot.controller.motor.Motor;
import ioio.robot.controller.motor.SG90;
import ioio.robot.controller.motor.ServoMotor;
import ioio.robot.light.FullColorLED;
import ioio.robot.light.LED;
import ioio.robot.sensor.PoseAnalizer;
import ioio.robot.sensor.SensorTester;
import ioio.robot.sensor.SpeedMater;
import ioio.robot.sensor.TrailPoint;
import ioio.robot.sensor.TrailView;
import ioio.robot.util.Util;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
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

/**
pin 1, 2	: i2cセンサ通信
pin 3, 4	: DCモータ
pin 5		: サーボモータ
pin 6		: スピーカー
pin 7		: 回転数入力
pin 10-12	: LED
**/

public class CrawlRobot implements Robot {
	private final static String TAG = "CrawlRobot";
	private final static float DEG2RAD = (float)(Math.PI/180.0);
	
	private final static int DRIVE_TASK_SES = 0;
	private final static int EYE_TASK_SES = 1;
	private final static int EARS_TASK_SES = 2;

	private final static int AUTO_DRIVE_SES = 3;
	private final static int AUTO_EMOTION_SES = 4;
	private final static int SHOW_INFO_SES = 5;
	
	private final static int SES_NUM = 6;
	
	private Util util;
	private Motor[] motor;
	private FullColorLED[] led;
	private SpeedMater speedMater;
	private SensorTester sensor;
	private static double[] defaultMotorInitState = {0.5, 0.0};  // 初期値
	private double[] motorInitState = {0.5, 0.0};  // 初期値
	private static float[] ledInitState = {0f};
	private int motorNum = motorInitState.length;
	private int ledNum = ledInitState.length;
	private int distPerCycle = 48;	// モーター1回転で進む距離[mm]
	private LinearLayout layout;
	private LinearLayout modeSelectLayout, manualContollerLayout, sensorTextLayout, trailControllerLayout, trailViewLayout;
	private ToggleButton autoButton, autoEmoButton, showInfoButton;
	private Button backButton, forwardButton, stopButton;
	private Button[] emoButton;
    private ScheduledExecutorService[] ses;
    private boolean isActive, isAuto, isAutoEmo, showInfo;
	
	/** コンストラクタ **/
	public CrawlRobot(Util util) {
		this(util, defaultMotorInitState);
	}
	/** 初期化つきコンストラクタ **/
	public CrawlRobot(Util util, double[] motorInitState) {
		super();
		this.util = util;
		int len = motorNum;
		if(motorInitState.length < motorNum)	len = motorInitState.length;
		for(int i=0; i<len; i++){
			this.motorInitState[i] = motorInitState[i];
		}
		ses = new ScheduledExecutorService[SES_NUM];
		init();
	}

	/** 初期設定 **/
	private void init(){
		// モータ
		motor = new Motor[motorNum];
		motor[0] = new DCMotor(util, "くるま", motorInitState[0]);  // くるま
		motor[1] = new SG90(util, "耳", motorInitState[1]);	// 耳
		for( Motor m : motor )	m.init();
		// LED
		led = new FullColorLED[ledNum];
		led[0] = new FullColorLED(util, "目");
		for( FullColorLED l : led )	l.init();
		// スピードメータ
		this.speedMater = new SpeedMater(util, distPerCycle, this);
		((DCMotor) motor[0]).setSpeedMater(this.speedMater);
		// センサ
		this.sensor = new SensorTester(util, this);
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

        // 表示きりかえ+簡易操作のレイアウトを登録
        layout.addView(getShowModeLayout(parent));
        
        // マニュアル操作パネルを登録
        manualContollerLayout = new LinearLayout(parent);
        manualContollerLayout.setOrientation(LinearLayout.VERTICAL);
		manualContollerLayout.setVisibility(View.GONE);
        layout.addView(manualContollerLayout);
        // - emotionの操作パネルを登録
        manualContollerLayout.addView(getEmoOperationLayout(parent));
        // - モーターごとの操作パネルを登録
		for(int i=0; i<motorNum; i++)	manualContollerLayout.addView(motor[i].getOperationLayout(parent));
        // - LEDごとの操作パネルを登録
		for(int i=0; i<ledNum; i++)	manualContollerLayout.addView(led[i].getOperationLayout(parent));

        // trail controlパネルの登録
        trailControllerLayout = sensor.getTrailControllerLayout(parent);
        layout.addView(trailControllerLayout);
        
		// TrailViewとセンサ表示を重ねて表示
		FrameLayout sensorLayout = new FrameLayout(parent);
		layout.addView(sensorLayout);
        
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
        manualShowCheck.setChecked(false);
        layout.addView(manualShowCheck);

		// backボタン
		backButton = new Button(parent);
		backButton.setText("<");
		backButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					goBackward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		layout.addView(backButton);
		// stopボタン
		stopButton = new Button(parent);
		stopButton.setText("○");
		stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				stop();
			}
		});
		layout.addView(stopButton);
		// forwawdボタン
		forwardButton = new Button(parent);
		forwardButton.setText(">");
		forwardButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					goForward();
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
        autoButton = new ToggleButton(parent);
        autoButton.setTextOn("auto");
        autoButton.setTextOff("manual");
        autoButton.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)	showInfoButton.setChecked(false);
				setAuto(isChecked);
			}
        });
        autoButton.setText("manual");
        autoLayout.addView(autoButton,lp);
        // オートemotion切り替えのボタン
        autoEmoButton = new ToggleButton(parent);
        autoEmoButton.setTextOn("auto-emo");
        autoEmoButton.setTextOff("manual-emo");
        autoEmoButton.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)	showInfoButton.setChecked(false);
				setAutoEmo(isChecked);
			}
        });
        autoEmoButton.setText("manual-emo");
        autoLayout.addView(autoEmoButton,lp);
        // 判定結果提示切り替えのボタン
        showInfoButton = new ToggleButton(parent);
        showInfoButton.setTextOn("show-info");
        showInfoButton.setTextOff("hide-info");
        showInfoButton.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					autoButton.setChecked(false);
					autoEmoButton.setChecked(false);
				}
				setShowInfo(isChecked);
			}
        });
        showInfoButton.setText("hide-info");
        autoLayout.addView(showInfoButton,lp);
        
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
			sensor.openPins(ioio, 1, 2);
		} catch (IOException e) {
			e.printStackTrace();
		}
        // ピンにモーターを対応させる(pin3-5)
		cnt = 3;
		for(int i=0; i<motorNum; i++){
			cnt += motor[i].openPin(ioio, cnt);
		}
		// スピーカー(pin6)
		//ioio.openPwmOutput(6, 500).setDutyCycle(0.5f);
		// スピードメータの入力ピン(pin7)
		speedMater.openPins(ioio, 7);
		// 目(pin10-12)
		cnt = 10;
		for(int i=0; i<ledNum; i++){
			cnt += led[i].openPin(ioio, cnt);
		}
		
		return cnt;
	}

	@Override
	/** onにする **/
	public void activate() throws ConnectionLostException {
		for(Motor m : motor)	m.activate();
		for(FullColorLED l : led)	l.activate();
		speedMater.activate();
		sensor.activate();
		for(Button b : emoButton)	util.setEnabled(b, true);
		isActive = true;
	}
	@Override
	/** offにする **/
	public void disactivate() throws ConnectionLostException {
		for(Motor m : motor)	m.disactivate();
		for(FullColorLED l : led)	l.disactivate();
		speedMater.disactivate();
		sensor.disactivate();
		for(Button b : emoButton)	util.setEnabled(b, false);
		isActive = false;
	}
	@Override
	/** 接続解除されたときの処理 **/
	public void disconnected() throws ConnectionLostException {
		for(Motor m : motor)	m.disconnected();
		for(FullColorLED l : led)	l.disconnected();
		speedMater.disconnected();
		sensor.disconnected();
		for(Button b : emoButton)	util.setEnabled(b, false);
		isActive = false;
	}
	
	
	/** 全体を自動制御に切り替え **/
	public void setAuto(boolean tf){
		isAuto = tf;
		if(tf){
	        // タイマーを作成する
	        ses[AUTO_DRIVE_SES] = Executors.newSingleThreadScheduledExecutor();
	        // 500msごとにtaskを実行する
        	Log.i(TAG, "autoControllStarted");
        	ses[AUTO_DRIVE_SES].scheduleAtFixedRate(autoControllTestTask, 0L, 500L, TimeUnit.MILLISECONDS);
		}else{
			if(ses[AUTO_DRIVE_SES] == null)	return;
			// タイマーを停止する
        	Log.i(TAG, "autoControllEnd");
			ses[AUTO_DRIVE_SES].shutdown();
			motor[0].changeState(0.0f);
		}
		for(Motor m : motor)	m.setIsAutoControlled(tf);
	}
	/** emotion自動制御に切り替え **/
	public void setAutoEmo(boolean tf){
		isAutoEmo = tf;
		if(tf){
	        // タイマーを作成する
	        ses[AUTO_EMOTION_SES] = Executors.newSingleThreadScheduledExecutor();
	        // 2000msごとにtaskを実行する
	        Log.i(TAG, "randomEmotionStarted");
	        ses[AUTO_EMOTION_SES].scheduleAtFixedRate(randomEmotionTask, 0L, 2000L, TimeUnit.MILLISECONDS);
		}else{
			stand();
			if(ses == null)	return;
			// タイマーを停止する
	        Log.i(TAG, "randomEmotionEnd");
			ses[AUTO_EMOTION_SES].shutdown();
		}
		motor[1].setIsAutoControlled(tf);
	}
	/** 計測結果表示に切り替え **/
	public void setShowInfo(boolean tf){
		showInfo = tf;
		if(tf){
	        // タイマーを作成する
	        ses[SHOW_INFO_SES] = Executors.newSingleThreadScheduledExecutor();
	        // 100msごとにtaskを実行する
	        Log.i(TAG, "showInfoStarted");
	        ses[SHOW_INFO_SES].scheduleAtFixedRate(showInfoTask, 0L, 100L, TimeUnit.MILLISECONDS);
		}else{
			stand();
			if(ses == null)	return;
			// タイマーを停止する
	        Log.i(TAG, "showInfoEnd");
			ses[SHOW_INFO_SES].shutdown();
			// 目の点滅タスクは中止
			manageFlick(false);
		}
		motor[1].setIsAutoControlled(tf);
	}

	private final static float GO_FORWARD = 1.0f;
	private final static float GO_INIT = 0f;
	private final static float GO_BACKWARD = -1.0f;
	private final static float GO_DD = 0.1f;
	private final static int GO_SLEEP = 10;
	/** 進む 
	 * @throws InterruptedException **/
	public void goForward() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s<GO_FORWARD; s+=GO_DD){
			motor[0].changeState(s);
			Thread.sleep(GO_SLEEP);
		}
		motor[0].changeState(GO_FORWARD);
	}
	/** 戻る 
	 * @throws InterruptedException **/
	public void goBackward() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s>GO_BACKWARD; s-=GO_DD){
			motor[0].changeState(s);
			Thread.sleep(GO_SLEEP);
		}
		motor[0].changeState(GO_BACKWARD);
	}
	/** 停止 **/
	public void stop(){
		motor[0].changeState(GO_INIT);
	}

	private final static float EAR_FORWARD = 0.2f;
	private final static float EAR_INIT = 0.5f;
	private final static float EAR_BACKWARD = 0.9f;
	private final static float EAR_DD = 0.02f;
	private final static int EAR_SLEEP = 20;
	/** 耳を任意の角度に **/
	private void earsChangeStateByRad(float rad) {
		motor[1].changeStateByRad(rad);
	}
	/** 耳を立てる **/
	private void earsForward() {
		motor[1].changeState(EAR_FORWARD);
	}
	/** 耳を伏せる **/
	private void earsBackward() {
		motor[1].changeState(EAR_BACKWARD);
	}
	/** 耳を初期位置に **/
	private void earsReset() {
		motor[1].changeState(EAR_INIT);
	}
    /** ゆっくり耳を立てるtask **/
    private final Runnable earsForwardSlowlyTask = new Runnable(){
        @Override
        public void run() {
    		try {
    			float state = (float)motor[1].getState();
    			for(float s=state; s>EAR_FORWARD; s-=EAR_DD){
    				motor[1].changeState(s);
    				Thread.sleep(EAR_SLEEP);
    			}
			} catch (InterruptedException e) {e.printStackTrace();}
        }
    };
    /** ゆっくり耳を伏せるtask **/
    private final Runnable earsBackwardSlowlyTask = new Runnable(){
        @Override
        public void run() {
    		try {
    			float state = (float)motor[1].getState();
    			for(float s=state; s<EAR_BACKWARD; s+=EAR_DD){
    				motor[1].changeState(s);
    				Thread.sleep(EAR_SLEEP);
    			}
			} catch (InterruptedException e) {e.printStackTrace();}
        }
    };
    /** 耳をふるわせるtask **/
    private final Runnable swingEarsTask = new Runnable(){
        @Override
        public void run() {
    		try {
	    		earsForward();
				Thread.sleep(100);
	    		earsBackward();
	    		Thread.sleep(100);
	    		earsForward();
	    		Thread.sleep(100);
	    		earsBackward();
	    		Thread.sleep(100);
	    		earsReset();
			} catch (InterruptedException e) {e.printStackTrace();}
        }
    };
    /** 耳を繰り返しふるわせるtask **/
    private final Runnable swingEarsTask2 = new Runnable(){
    	private int[] taskLoopSwingFast = {1,0,1,0,1,0,1,0,1,0,1,0};
    	private int[] taskLoopSwingSlow = {1,1,0,0,1,1,0,0,1,1,0,0};
    	private int taskCnt = 0;
        @Override
        public void run() {
        	if(!isActive)	return;
        	Log.d("SwingEars", "running...");
			switch(taskLoopSwingFast[taskCnt]){
			case 0:		earsForward();		break;
			case 1:		earsBackward();		break;
			}
        	if(taskCnt==taskLoopSwingFast.length-1)	taskCnt = 0;
        	else									taskCnt++;
        }
    };
    

    /** 目の点滅タスク **/
    private final Runnable eyesFlickTask = new Runnable(){
    	private int[] taskLoopFlickFast = {1,0,1,0,1,0,1,0,1,0,1,0};
    	private int[] taskLoopFlickSlow = {1,1,0,0,1,1,0,0,1,1,0,0};
    	private int taskCnt = 0;
        @Override
        public void run() {
        	if(!isActive)	return;
        	Log.d("eyeFlick", "running...");
			switch(taskLoopFlickFast[taskCnt]){
			case 0:		led[0].setLuminous(0f);		break;
			case 1:		led[0].setLuminous(1f);		break;
			}
        	if(taskCnt==taskLoopFlickFast.length-1)	taskCnt = 0;
        	else									taskCnt++;
        }
    };
    

	/** 平常 **/
	public void stand(){
		led[0].green();
		earsReset();
	}
	/** 喜ぶ 
	 * @throws InterruptedException **/
	public void happy() throws InterruptedException{
		led[0].green();
		if( intrruptTask(EARS_TASK_SES) ){
			ses[EARS_TASK_SES] = Executors.newSingleThreadScheduledExecutor();
			ses[EARS_TASK_SES].schedule(swingEarsTask, 0, TimeUnit.MILLISECONDS);
		}
	}
	/** 怒る **/
	public void angry(){
		led[0].red();
		earsForward();
	}
	/** 悲しむ 
	 * @throws InterruptedException **/
	public void sad() throws InterruptedException{
		led[0].blue();
		if( intrruptTask(EARS_TASK_SES) ){
			ses[EARS_TASK_SES] = Executors.newSingleThreadScheduledExecutor();
			ses[EARS_TASK_SES].schedule(earsBackwardSlowlyTask, 0, TimeUnit.MILLISECONDS);
		}
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
	
	/** getter **/
	public double[] getMotorInitState() {
		return motorInitState;
	}
	
	


	/** 自動制御のタスク **/
    private final Runnable autoControllTestTask = new Runnable(){
    	private int[] taskLoopDrive = {1,1,1,1-1,-1,-1,-1,0,0,0,0};
    	private int[] taskLoopEars = {0,0,0,0,0,0,1,-1,1,-1,0,0};
    	private int taskCnt = 0;
        @Override
        public void run() {
        	if(!isActive)	return;
        	Log.d("autoControll", "running...");
			try {
	        	if(!isAutoEmo){
					switch(taskLoopEars[taskCnt]){
					case 1:
						if( intrruptTask(EARS_TASK_SES) ){
							ses[EARS_TASK_SES] = Executors.newSingleThreadScheduledExecutor();
							ses[EARS_TASK_SES].schedule(earsForwardSlowlyTask, 0, TimeUnit.MILLISECONDS);
						}
						break;
					case -1:
						if( intrruptTask(EARS_TASK_SES) ){
							ses[EARS_TASK_SES] = Executors.newSingleThreadScheduledExecutor();
							ses[EARS_TASK_SES].schedule(earsBackwardSlowlyTask, 0, TimeUnit.MILLISECONDS);
						}
						break;
					default:	earsReset();		break;
					}
	        	}
				switch(taskLoopDrive[taskCnt]){
				case 1:		goForward();		break;
				case -1:	goBackward();	break;
				default:	stop();				break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	
        	if(taskCnt==taskLoopDrive.length-1)	taskCnt = 0;
        	else								taskCnt++;
        }
    };
    
	/** ランダム感情表現のタスク **/
    private final Runnable randomEmotionTask = new Runnable(){
    	Random rand;
    	boolean inited = false;
        @Override
        public void run() {
        	if(!inited){
            	Log.d("randomEmotion", "init");
            	rand = new Random();
            	rand.setSeed(System.currentTimeMillis());
        		inited = true;
        	}
        	Log.d("randomEmotion", "running...");
        	if(!isActive)	return;
        	try{
	        	switch(rand.nextInt(4)){
	        	case 0:	stand();	break;
	        	case 1:	happy();	break;
	        	case 2:	angry();	break;
	        	case 3:	sad();		break;
	        	}
        	}catch(InterruptedException e){
        		e.printStackTrace();
        	}
        }
    };
    
	/** 計測結果提示のタスク **/
	boolean alreadyFlicking = false;
	boolean alreadySwinging = false;
    private final Runnable showInfoTask = new Runnable(){
        @Override
        public void run() {
        	//Log.d("showInfo", "running...");
        	if(!isActive)	return;
        	
        	float dif = sensor.getPitchDifference();
        	// 位置の違いを目で示す
        	switch(sensor.getNowTpType()){
        	case TrailPoint.NO_TYPE:
        		led[0].setColor(TrailView.NO_TYPE_COLOR);
        		break;
        	case TrailPoint.BACK:
        		led[0].setColor(TrailView.BACK_COLOR);
        		break;
        	case TrailPoint.SHOLDER:
        		led[0].setColor(TrailView.SHOLDER_COLOR);
        		break;
        	case TrailPoint.ARM:
        		led[0].setColor(TrailView.ARM_COLOR);
        		break;
        	}
        	// 判定結果を目の点滅と耳で示す
        	if(Math.abs(dif) < PoseAnalizer.THRESHOLD_BACK1){
            	manageFlick(false);
        		manageSwingEars(false);
        		earsChangeStateByRad(-dif);
        	}else{
            	manageFlick(true);
        		manageSwingEars(true);
        	}
        }
    };
	
	public void manageFlick(boolean isFlick){
		if(ses != null){
        	if(isFlick){
        		if(!alreadyFlicking){
					if( intrruptTask(EYE_TASK_SES) ){
						ses[EYE_TASK_SES] = Executors.newSingleThreadScheduledExecutor();
		    	        Log.i(TAG, "eyesFlickStarted");
		    	        ses[EYE_TASK_SES].scheduleAtFixedRate(eyesFlickTask, 0L, 200L, TimeUnit.MILLISECONDS);
					}
	    	        alreadyFlicking = true;
        		}
        	}else{
        		if(alreadyFlicking){
					if( intrruptTask(EYE_TASK_SES) ){
		    	        Log.i(TAG, "eyesFlickEnd");
        			}
	    	        alreadyFlicking = false;
	        		led[0].setLuminous(1f);
        		}
        	}
		}
	}
	public void manageSwingEars(boolean isSwing){
		if(ses != null){
        	if(isSwing){
        		if(!alreadySwinging){
					if( intrruptTask(EARS_TASK_SES) ){
						ses[EARS_TASK_SES] = Executors.newSingleThreadScheduledExecutor();
		    	        Log.i(TAG, "earsSwingStarted");
		    	        ses[EARS_TASK_SES].scheduleAtFixedRate(swingEarsTask2, 0L, 200L, TimeUnit.MILLISECONDS);
					}
					alreadySwinging = true;
        	}
        	}else{
        		if(alreadySwinging){
					if( intrruptTask(EARS_TASK_SES) ){
		    	        Log.i(TAG, "earsSwingEnd");
        			}
					alreadySwinging = false;
	        		earsReset();
        		}
        	}
		}
	}
    

	@Override
	public void onResume() {
		sensor.onResume();
	}
	@Override
	public void onPause() {

		sensor.onPause();
	}
	
	private boolean intrruptTask(int num){
		if(ses != null){
			if(ses[num] != null){
				if(!ses[num].isShutdown())	ses[num].shutdown();
				ses[num] = null;
			}
			return true;
		}else{
			return false;
		}
	}
	
}
