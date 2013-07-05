package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import android.widget.LinearLayout;

public class CrawlRobot implements Robot {
	private Motor[] motor;
	private double[] motorInitState = {0.0};  // �����l
	private int motorNum = motorInitState.length;
	private LinearLayout layout;
	
	/* �R���X�g���N�^ */
	public CrawlRobot() {
		super();
		init();
	}
	public CrawlRobot(double[] motorInitState) {
		super();
		int len = motorNum;
		if(motorInitState.length < motorNum)	len = motorInitState.length;
		for(int i=0; i<len; i++){
			this.motorInitState[i] = motorInitState[i];
		}
		init();
	}

	/* �����p�x��ݒ肷�� */
	private void init(){
		motor = new Motor[motorNum];
		motor[0] = new DCMotor(motorInitState[0], "�����");  // �����
		for( Motor m : motor ){
			m.init();
		}
		
	}
	
	public LinearLayout getLayout(MainActivity parent){
        /* �e�̃A�N�e�B�r�e�B�ɓ��I���C�A�E�g���쐬����@*/
        layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        /* ���[�^�[���Ƃ̑���p�l����o�^�@*/
		for(int i=0; i<motorNum; i++){
	        layout.addView(motor[i].getOperationLayout(parent));
		}
		return layout;
	}

	/* �s�����J���Ċe���[�^�[�ɑΉ������� */
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException{
		int cnt = startPin;
        // �s���Ƀ��[�^�[��Ή�������
		for(int i=0; i<motorNum; i++){
			cnt += motor[i].openPin(ioio, cnt);
		}
		return cnt;
	}

	/* on�ɂ��� */
	public void activate() throws ConnectionLostException {
		for(Motor m : motor){
			m.activate();
		}
	}
	/* off�ɂ��� */
	public void disactivate() throws ConnectionLostException {
		for(Motor m : motor){
			m.disactivate();
		}
	}
	
	public void disconnected() throws ConnectionLostException {
		for(Motor m : motor){
			m.disconnected();
		}
	}
	public double[] getMotorInitState() {
		return motorInitState;
	}
	
}
