package ioio.robot.region.crawl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.region.Region;
import ioio.robot.region.crawl.sensor.SpeedMater;
import ioio.robot.robot.Robot;
import ioio.robot.util.Util;
import ioio.robot.part.motor.DCMotor;

public class Wheel extends Region {
	private final static String TAG = "Wheel";
	private final static boolean DEBUG_SPEEDMATER = true;
	private final static long DEBUG_INTERVAL = 300;
    private ScheduledExecutorService sesForDebug;

	private final static float GO_FORWARD = 1.0f;
	private final static float GO_INIT = 0f;
	private final static float GO_BACKWARD = -1.0f;
	private final static float GO_DD = 0.1f;
	private final static int GO_SLEEP = 10;
	
	private final static double[] defaultMotorInitState = {0.5};
	private final static int motorNum = defaultMotorInitState.length;
	
	private DCMotor[] motor;
	private LinearLayout layout;
	private Robot parent;

	/** コンストラクタ **/
	public Wheel(Util util, Robot parent) {
		this(util, parent, defaultMotorInitState);
	}

	/** 初期化つきコンストラクタ **/
	public Wheel(Util util, Robot parent, double[] motorInitState) {
		this.util = util;
		motor = new DCMotor[motorNum];
		motor[0] = new DCMotor(util, "Legs", motorInitState[0]);
		part = motor;
		this.parent = parent;
	}

	/** スピードメーターの登録 **/
	public void setSpeedMater(SpeedMater sm){
		motor[0].setSpeedMater(sm);
	}
	
	/** レイアウトパネルを返す **/
	@Override
	public LinearLayout getLayout(Context context) {
		layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 1, 5, 1);
        // - モーターごとの操作パネルを登録
		for(int i=0; i<motor.length; i++)	layout.addView(motor[i].getOperationLayout(context));
		return layout;
	}
	

	private final Runnable debugTask = new Runnable(){
		public void run() {
			double state = motor[0].getState();
			if(state > 0.9){
				parent.incCount();
			}else if(state < -0.9){
				parent.decCount();
			}
		}
	};
	
	@Override
	public void activate() throws ConnectionLostException {
		super.activate();
        // タイマーを作成する
		sesForDebug = Executors.newSingleThreadScheduledExecutor();
		sesForDebug.scheduleAtFixedRate(debugTask, 0L, DEBUG_INTERVAL, TimeUnit.MILLISECONDS);
	}

	@Override
	public void disactivate() throws ConnectionLostException {
		super.disactivate();
		sesForDebug = null;
	}
	

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
}
