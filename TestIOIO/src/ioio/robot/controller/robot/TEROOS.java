package ioio.robot.controller.robot;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.controller.motor.BlueArrowBA_TS;
import ioio.robot.controller.motor.HS322HD;
import ioio.robot.controller.motor.Motor;
import ioio.robot.util.Util;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.LinearLayout;

public class TEROOS implements Robot {
	private Util util;
	
	private static final double pi = Math.PI;
	private Motor[] motor;
	private double[] motorInitState = {0, 0, -pi*0.25, 0, 0, 0, 0};
	private int motorNum = motorInitState.length;
	private LinearLayout layout;
	
	/* �R���X�g���N�^ */
	public TEROOS(Util util) {
		super();
		this.util = util;
		init();
	}
	public TEROOS(Util util, double[] motorInitState) {
		super();
		this.util = util;
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
		motor[0] = new HS322HD(util, "��i�ӂ�j", motorInitState[0]);  // ��i�U��j
		motor[1] = new HS322HD(util, "��", motorInitState[1]);  // ��
		motor[2] = new BlueArrowBA_TS(util, "�܂Ԃ�", motorInitState[2]);  // �܂Ԃ�
		motor[3] = new HS322HD(util, "��i�X����j", motorInitState[3]);  // ��i�X����j
		motor[4] = new HS322HD(util, "��", motorInitState[4]);  // ������
		motor[5] = new HS322HD(util, "��i�����j", motorInitState[5]);  // ��i�����j
		motor[6] = new HS322HD(util, "<���g�p>", motorInitState[6]);  // <���g�p>	
	}
	
	/* ����p�l���𐶐����ĕԂ� */
	public LinearLayout getLayout(Context parent){
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
	@Override
	public void setSpeed(float speed) {
		// do nothing
	}
	@Override
	public void incCount() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void decCount() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		
	}
	
}
