package ioio.robot.region.crawl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import ioio.robot.region.Region;
import ioio.robot.region.crawl.sensor.SpeedMater;
import ioio.robot.util.Util;
import ioio.robot.part.motor.DCMotor;

public class Wheel extends Region {
	private final static String TAG = "Wheel";

	private final static float GO_FORWARD = 1.0f;
	private final static float GO_INIT = 0f;
	private final static float GO_BACKWARD = -1.0f;
	private final static float GO_DD = 0.1f;
	private final static int GO_SLEEP = 10;
	
	private final static double[] defaultMotorInitState = {0.5};
	private final static int motorNum = defaultMotorInitState.length;
	
	private DCMotor[] motor;
	private LinearLayout layout;

	/** �R���X�g���N�^ **/
	public Wheel(Util util) {
		this(util, defaultMotorInitState);
	}

	/** ���������R���X�g���N�^ **/
	public Wheel(Util util, double[] motorInitState) {
		this.util = util;
		motor = new DCMotor[motorNum];
		motor[0] = new DCMotor(util, "Legs", motorInitState[0]);
		part = motor;
	}

	/** �X�s�[�h���[�^�[�̓o�^ **/
	public void setSpeedMater(SpeedMater sm){
		motor[0].setSpeedMater(sm);
	}
	
	/** ���C�A�E�g�p�l����Ԃ� **/
	@Override
	public LinearLayout getLayout(Context context) {
		layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 1, 5, 1);
        // - ���[�^�[���Ƃ̑���p�l����o�^
		for(int i=0; i<motor.length; i++)	layout.addView(motor[i].getOperationLayout(context));
		return layout;
	}
	
	
	
	/** �i�� 
	 * @throws InterruptedException **/
	public void goForward() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s<GO_FORWARD; s+=GO_DD){
			motor[0].changeState(s);
			Thread.sleep(GO_SLEEP);
		}
		motor[0].changeState(GO_FORWARD);
	}
	/** �߂� 
	 * @throws InterruptedException **/
	public void goBackward() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s>GO_BACKWARD; s-=GO_DD){
			motor[0].changeState(s);
			Thread.sleep(GO_SLEEP);
		}
		motor[0].changeState(GO_BACKWARD);
	}
	/** ��~ **/
	public void stop(){
		motor[0].changeState(GO_INIT);
	}
}
