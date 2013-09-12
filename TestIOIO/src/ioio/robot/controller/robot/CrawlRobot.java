package ioio.robot.controller.robot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.controller.motor.DCMotor;
import ioio.robot.controller.motor.Motor;
import ioio.robot.sensor.SpeedMater;
import ioio.robot.util.Util;
import android.os.Handler;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

public class CrawlRobot implements Robot {
	private Util util;
	private Motor[] motor;
	private SpeedMater speedMater;
	private static double[] motorInitState = {0.5};  // 初期値
	private int motorNum = motorInitState.length;
	private int distPerCycle = 48;	// モーター1回転で進む距離[mm]
	private LinearLayout layout;
	private ToggleButton autoButton;
    private ScheduledExecutorService ses = null;
    private boolean isActive;
	
	/** コンストラクタ **/
	public CrawlRobot(Util util) {
		this(util, motorInitState);
	}
	/** 初期化つきコンストラクタ **/
	public CrawlRobot(Util util, double[] motorInitState) {
		super();
		this.util = util;
		this.speedMater = new SpeedMater(util, distPerCycle);
		int len = motorNum;
		if(motorInitState.length < motorNum)	len = motorInitState.length;
		for(int i=0; i<len; i++){
			this.motorInitState[i] = motorInitState[i];
		}
		init();
	}

	/** 初期速度を設定する **/
	private void init(){
		motor = new Motor[motorNum];
		motor[0] = new DCMotor(util, "くるま", motorInitState[0]);  // くるま
		for( Motor m : motor ){
			m.init();
		}
	}

	@Override
	/** ロボットの操作パネルを作って返す **/
	public LinearLayout getLayout(MainActivity parent){
        // 親のアクティビティに動的レイアウトを作成する
        layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        // オート切り替えのボタン
        autoButton = new ToggleButton(parent);
        autoButton.setTextOn("auto-controll");
        autoButton.setTextOff("manual-controll");
        autoButton.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setAuto(isChecked);
			}
        });
        layout.addView(autoButton);
        autoButton.setText("manual-controll");
        // モーターごとの操作パネルを登録
		for(int i=0; i<motorNum; i++){
	        layout.addView(motor[i].getOperationLayout(parent));
		}
		// スピードメータのパネルを登録
		layout.addView(speedMater.getLayout(parent));
		
		return layout;
	}

	@Override
	/** ピンを開いて各モーターに対応させる **/
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException{
		int cnt = startPin;
        // ピンにモーターを対応させる
		for(int i=0; i<motorNum; i++){
			cnt += motor[i].openPin(ioio, cnt);
		}
		// スピードメータの入力ピン
		speedMater.openPins(ioio, 4);
		
		return cnt;
	}

	@Override
	/** onにする **/
	public void activate() throws ConnectionLostException {
		for(Motor m : motor){
			m.activate();
		}
		speedMater.activate();
		isActive = true;
	}
	@Override
	/** offにする **/
	public void disactivate() throws ConnectionLostException {
		for(Motor m : motor){
			m.disactivate();
		}
		speedMater.disactivate();
		isActive = false;
	}
	@Override
	/** 接続解除されたときの処理 **/
	public void disconnected() throws ConnectionLostException {
		for(Motor m : motor){
			m.disconnected();
		}
		speedMater.disconnected();
		isActive = false;
	}
	

	/** 自動制御のタスク **/
	private int[] taskLoop = {0,1,0,1,0,0,1,1};
	private int taskCnt = 0;
    private final Runnable autoControllTask = new Runnable(){
        @Override
        public void run() {
        	if(!isActive)	return;
        	Log.d("autoControll", "running...");
			try {
	        	if(taskLoop[taskCnt] == 0)	goForward();
				else						goBackForward();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	
        	if(taskCnt==taskLoop.length-1)	taskCnt = 0;
        	else							taskCnt++;
        }
    };
	
	/** 全体を自動制御に切り替え **/
	public void setAuto(boolean tf){
		if(tf){
	        // タイマーを作成する
	        ses = Executors.newSingleThreadScheduledExecutor();
	        // 1000msごとにtaskを実行する
	        ses.scheduleAtFixedRate(autoControllTask, 0L, 1000L, TimeUnit.MILLISECONDS);
		}else{
			if(ses == null)	return;
			// タイマーを停止する
			ses.shutdown();
			ses = null;
			motor[0].changeState(0.0f);
		}
		for(Motor m : motor){
			m.setIsAutoControlled(tf);
		}
	}

	/** 進む 
	 * @throws InterruptedException **/
	public void goForward() throws InterruptedException{
		float state = (float)motor[0].getState();
		float dd = 0.1f;
		for(float s=state; s<1.0; s+=dd){
			motor[0].changeState(s);
			Thread.sleep(10);
		}
	}
	/** 戻る 
	 * @throws InterruptedException **/
	public void goBackForward() throws InterruptedException{
		float state = (float)motor[0].getState();
		float dd = 0.1f;
		for(float s=state; s>-1.0; s-=dd){
			motor[0].changeState(s);
			Thread.sleep(10);
		}
	}
	
	
	/** getter **/
	public double[] getMotorInitState() {
		return motorInitState;
	}
	
}
