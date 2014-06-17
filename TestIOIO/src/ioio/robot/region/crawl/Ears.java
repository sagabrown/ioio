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

	/** �R���X�g���N�^ **/
	public Ears(Util util) {
		this(util, defaultMotorInitState);
	}

	/** ���������R���X�g���N�^ **/
	public Ears(Util util, double[] motorInitState) {
		this.util = util;
		motor = new SG90[motorNum];
		motor[0] = new SG90(util, "Ears", motorInitState[0]);
		part = motor;
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
	
	
	/** ����C�ӂ̊p�x�� **/
	public void changeStateByRad(float rad) {
		motor[0].changeStateByRad(rad);
	}
	/** ���𗧂Ă� **/
	public void forward() {
		motor[0].changeState(EAR_FORWARD);
	}
	/** ���𕚂��� **/
	public void backForward() {
		motor[0].changeState(EAR_BACKWARD);
	}
	/** ���������ʒu�� **/
	public void reset() {
		motor[0].changeState(EAR_INIT);
	}
	/** ������莨�𗧂Ă� 
	 * @throws InterruptedException **/
	public void forwardSlowly() throws InterruptedException{
		float state = (float)motor[1].getState();
		for(float s=state; s>EAR_FORWARD; s-=EAR_DD){
			motor[0].changeState(s);
			Thread.sleep(EAR_SLEEP);
		}
	}
	/** ������莨�𕚂���
	 * @throws InterruptedException **/
	public void backForwardSlowly() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s<EAR_BACKWARD; s+=EAR_DD){
			motor[0].changeState(s);
			Thread.sleep(EAR_SLEEP);
		}
	}
}
