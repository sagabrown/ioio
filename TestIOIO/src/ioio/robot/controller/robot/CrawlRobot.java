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
	private static double[] motorInitState = {0.5};  // �����l
	private int motorNum = motorInitState.length;
	private LinearLayout layout;
	private ToggleButton autoButton;
    private ScheduledExecutorService ses = null;
	
	/** �R���X�g���N�^ **/
	public CrawlRobot(Util util) {
		this(util, motorInitState);
	}
	/** ���������R���X�g���N�^ **/
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

	/** �������x��ݒ肷�� **/
	private void init(){
		motor = new Motor[motorNum];
		motor[0] = new DCMotor(util, "�����", motorInitState[0]);  // �����
		for( Motor m : motor ){
			m.init();
		}
	}
	
	/** ���{�b�g�̑���p�l��������ĕԂ� **/
	public LinearLayout getLayout(MainActivity parent){
        // �e�̃A�N�e�B�r�e�B�ɓ��I���C�A�E�g���쐬����
        layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        // �I�[�g�؂�ւ��̃{�^��
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
        // ���[�^�[���Ƃ̑���p�l����o�^
		for(int i=0; i<motorNum; i++){
	        layout.addView(motor[i].getOperationLayout(parent));
		}
		return layout;
	}

	/** �s�����J���Ċe���[�^�[�ɑΉ������� **/
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException{
		int cnt = startPin;
        // �s���Ƀ��[�^�[��Ή�������
		for(int i=0; i<motorNum; i++){
			cnt += motor[i].openPin(ioio, cnt);
		}
		return cnt;
	}

	/** on�ɂ��� **/
	public void activate() throws ConnectionLostException {
		for(Motor m : motor){
			m.activate();
		}
		util.setEnabled(autoButton, true);
	}
	/** off�ɂ��� **/
	public void disactivate() throws ConnectionLostException {
		for(Motor m : motor){
			m.disactivate();
		}
		util.setEnabled(autoButton, false);
		setAuto(false);
	}
	/** �ڑ��������ꂽ�Ƃ��̏��� **/
	public void disconnected() throws ConnectionLostException {
		for(Motor m : motor){
			m.disconnected();
		}
		util.setEnabled(autoButton, false);
	}
	

	/** ��������̃^�X�N **/
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
	
	/** �S�̂���������ɐ؂�ւ� **/
	public void setAuto(boolean tf){
		for(Motor m : motor){
			m.setIsAutoControlled(tf);
		}
		if(tf){
	        // �^�C�}�[���쐬����
	        ses = Executors.newSingleThreadScheduledExecutor();
	        // 1000ms���Ƃ�task�����s����
	        ses.scheduleAtFixedRate(autoControllTask, 0L, 1000L, TimeUnit.MILLISECONDS);
		}else{
			if(ses == null)	return;
			// �^�C�}�[���~����
			ses.shutdown();
			ses = null;
		}
	}

	/** �i�� **/
	public void goForward(){
		motor[0].changeState(0.8f);
	}
	/** �߂� **/
	public void goBackForward(){
		motor[0].changeState(-0.8f);
	}
	
	
	/** getter **/
	public double[] getMotorInitState() {
		return motorInitState;
	}
	
}
