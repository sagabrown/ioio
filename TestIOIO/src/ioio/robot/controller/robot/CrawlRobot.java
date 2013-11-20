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
import ioio.robot.controller.motor.SG90;
import ioio.robot.controller.motor.ServoMotor;
import ioio.robot.light.FullColorLED;
import ioio.robot.light.LED;
import ioio.robot.sensor.SensorTester;
import ioio.robot.sensor.SpeedMater;
import ioio.robot.util.Util;
import android.os.Handler;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

/**
pin 1, 2	: i2c�Z���T�ʐM
pin 3, 4	: DC���[�^
pin 5		: �T�[�{���[�^
pin 6		: �X�s�[�J�[
pin 7		: ��]������
pin 10-12	: LED
**/

public class CrawlRobot implements Robot {
	private Util util;
	private Motor[] motor;
	private FullColorLED[] led;
	private SpeedMater speedMater;
	private SensorTester sensor;
	private static double[] motorInitState = {0.5, 0.0};  // �����l
	private static float[] ledInitState = {0f};
	private int motorNum = motorInitState.length;
	private int ledNum = ledInitState.length;
	private int distPerCycle = 48;	// ���[�^�[1��]�Ői�ދ���[mm]
	private LinearLayout layout;
	private ToggleButton autoButton;
    private ScheduledExecutorService ses = null;
    private boolean isActive;
	
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

	/** �����ݒ� **/
	private void init(){
		motor = new Motor[motorNum];
		motor[0] = new DCMotor(util, "�����", motorInitState[0]);  // �����
		motor[1] = new SG90(util, "��", motorInitState[1]);	// ��
		for( Motor m : motor )	m.init();
		led = new FullColorLED[ledNum];
		led[0] = new FullColorLED(util, "��");
		for( FullColorLED l : led )	l.init();
		
		this.speedMater = new SpeedMater(util, distPerCycle);
		this.sensor = new SensorTester(util);
	}

	@Override
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
        // ���[�^�[���Ƃ̑���p�l����o�^
		for(int i=0; i<motorNum; i++){
	        layout.addView(motor[i].getOperationLayout(parent));
		}
        // LED���Ƃ̑���p�l����o�^
		for(int i=0; i<ledNum; i++){
	        layout.addView(led[i].getOperationLayout(parent));
		}
		// �X�s�[�h���[�^�̃p�l����o�^
		layout.addView(speedMater.getLayout(parent));
		// �Z���T�[�̃p�l����o�^
		layout.addView(sensor.getLayout(parent));
		
		return layout;
	}

	@Override
	/** �s�����J���Ċe���[�^�[�ɑΉ������� **/
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException, InterruptedException{
		int cnt = startPin;
		// 9���Z���T�̓��̓s��(pin1,2)
		sensor.openPins(ioio, 1, 2);
        // �s���Ƀ��[�^�[��Ή�������(pin3-5)
		cnt = 3;
		for(int i=0; i<motorNum; i++){
			cnt += motor[i].openPin(ioio, cnt);
		}
		// �X�s�[�J�[(pin6)
		//ioio.openPwmOutput(6, 500).setDutyCycle(0.5f);
		// �X�s�[�h���[�^�̓��̓s��(pin7)
		speedMater.openPins(ioio, 7);
		// ��(pin10-12)
		cnt = 10;
		for(int i=0; i<ledNum; i++){
			cnt += led[i].openPin(ioio, cnt);
		}
		
		return cnt;
	}

	@Override
	/** on�ɂ��� **/
	public void activate() throws ConnectionLostException {
		for(Motor m : motor)	m.activate();
		for(FullColorLED l : led)	l.activate();
		speedMater.activate();
		sensor.activate();
		isActive = true;
	}
	@Override
	/** off�ɂ��� **/
	public void disactivate() throws ConnectionLostException {
		for(Motor m : motor)	m.disactivate();
		for(FullColorLED l : led)	l.disactivate();
		speedMater.disactivate();
		sensor.disactivate();
		isActive = false;
	}
	@Override
	/** �ڑ��������ꂽ�Ƃ��̏��� **/
	public void disconnected() throws ConnectionLostException {
		for(Motor m : motor)	m.disconnected();
		for(FullColorLED l : led)	l.disconnected();
		speedMater.disconnected();
		sensor.disconnected();
		isActive = false;
	}
	

	/** ��������̃^�X�N **/
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
	
	/** �S�̂���������ɐ؂�ւ� **/
	public void setAuto(boolean tf){
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
			motor[0].changeState(0.0f);
		}
		for(Motor m : motor)	m.setIsAutoControlled(tf);
	}

	/** �i�� 
	 * @throws InterruptedException **/
	public void goForward() throws InterruptedException{
		float state = (float)motor[0].getState();
		float dd = 0.1f;
		for(float s=state; s<1.0; s+=dd){
			motor[0].changeState(s);
			Thread.sleep(10);
		}
	}
	/** �߂� 
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
