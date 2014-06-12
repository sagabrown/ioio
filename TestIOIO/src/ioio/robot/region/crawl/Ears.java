package ioio.robot.region.crawl;

import java.util.concurrent.Executors;

import ioio.robot.part.motor.SG90;
import ioio.robot.region.Region;
import ioio.robot.util.Util;
import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;

public class Ears extends Region {
	private final static String TAG = "Ears";

	private final static float EAR_FORWARD = 0.2f;
	private final static float EAR_INIT = 0.5f;
	private final static float EAR_BACKWARD = 0.9f;
	private final static float EAR_DD = 0.02f;
	private final static int EAR_SLEEP = 20;
	
	private final static double[] defaultMotorInitState = {0.0};
	private final static int motorNum = defaultMotorInitState.length;
	
	private SG90[] motor;
	private LinearLayout layout;

	/** コンストラクタ **/
	public Ears(Util util) {
		this(util, defaultMotorInitState);
	}

	/** 初期化つきコンストラクタ **/
	public Ears(Util util, double[] motorInitState) {
		this.util = util;
		motor = new SG90[motorNum];
		motor[0] = new SG90(util, "Ears", motorInitState[0]);
		part = motor;
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
	
	
	/** 耳を任意の角度に **/
	public void changeStateByRad(float rad) {
		motor[0].changeStateByRad(rad);
	}
	/** 耳を立てる **/
	public void forward() {
		motor[0].changeState(EAR_FORWARD);
	}
	/** 耳を伏せる **/
	public void backForward() {
		motor[0].changeState(EAR_BACKWARD);
	}
	/** 耳を初期位置に **/
	public void reset() {
		motor[0].changeState(EAR_INIT);
	}
	/** ゆっくり耳を立てる 
	 * @throws InterruptedException **/
	public void forwardSlowly() throws InterruptedException{
		float state = (float)motor[1].getState();
		for(float s=state; s>EAR_FORWARD; s-=EAR_DD){
			motor[0].changeState(s);
			Thread.sleep(EAR_SLEEP);
		}
	}
	/** ゆっくり耳を伏せる
	 * @throws InterruptedException **/
	public void backForwardSlowly() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s<EAR_BACKWARD; s+=EAR_DD){
			motor[0].changeState(s);
			Thread.sleep(EAR_SLEEP);
		}
	}
}
