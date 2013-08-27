package ioio.robot.controller.robot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.controller.motor.DCMotor;
import ioio.robot.controller.motor.Motor;
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
	private static double[] motorInitState = {0.5};  // 初期値
	private int motorNum = motorInitState.length;
	private LinearLayout layout;
	private ToggleButton autoButton;
    private ScheduledExecutorService ses = null;
	
	/** コンストラクタ **/
	public CrawlRobot(Util util) {
		this(util, motorInitState);
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
        autoButton.setEnabled(false);
        // モーターごとの操作パネルを登録
		for(int i=0; i<motorNum; i++){
	        layout.addView(motor[i].getOperationLayout(parent));
		}
		return layout;
	}

	/** ピンを開いて各モーターに対応させる **/
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException{
		int cnt = startPin;
        // ピンにモーターを対応させる
		for(int i=0; i<motorNum; i++){
			cnt += motor[i].openPin(ioio, cnt);
		}
		return cnt;
	}

	/** onにする **/
	public void activate() throws ConnectionLostException {
		for(Motor m : motor){
			m.activate();
		}
		util.setEnabled(autoButton, true);
	}
	/** offにする **/
	public void disactivate() throws ConnectionLostException {
		for(Motor m : motor){
			m.disactivate();
		}
		util.setEnabled(autoButton, false);
		setAuto(false);
	}
	/** 接続解除されたときの処理 **/
	public void disconnected() throws ConnectionLostException {
		for(Motor m : motor){
			m.disconnected();
		}
		util.setEnabled(autoButton, false);
	}
	

	/** 自動制御のタスク **/
	private int[] taskLoop = {0,1,0,1,0,0,1,1};
	private int taskCnt = 0;
    private final Runnable autoControllTask = new Runnable(){
        @Override
        public void run() {
        	Log.d("autoControll", "running...");
        	if(taskLoop[taskCnt] == 0)	goForward();
        	else						goBackForward();
        	
        	if(taskCnt==taskLoop.length-1)	taskCnt = 0;
        	else							taskCnt++;
        }
    };
	
	/** 全体を自動制御に切り替え **/
	public void setAuto(boolean tf){
		for(Motor m : motor){
			m.setIsAutoControlled(tf);
		}
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
		}
	}

	/** 進む **/
	public void goForward(){
		motor[0].changeState(0.8f);
	}
	/** 戻る **/
	public void goBackForward(){
		motor[0].changeState(-0.8f);
	}
	
	
	/** getter **/
	public double[] getMotorInitState() {
		return motorInitState;
	}
	
}
