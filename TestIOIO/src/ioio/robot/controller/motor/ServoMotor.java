package ioio.robot.controller.motor;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.util.Util;
import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** ���[�^�[�̃X�y�b�N�E�ݒ肩��f���[�e�B�[����v�Z����N���X **/
public abstract class ServoMotor implements Motor {
	private Util util;
	
	protected static final int pinNum = 1;	// �K�v�ȃs���̐�
	protected PwmOutput pin;				// �Ή����Ă���s��
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected double minTheta;				// ���[�^�[�̍ŏ���]�p�x. rad
	protected double maxTheta;				// ���[�^�[�̍ő��]�p�x. rad
	protected double minThetaLimit;				// ���[�^�[�̍ŏ�������]�p�x. rad
	protected double maxThetaLimit;				// ���[�^�[�̍ő吧����]�p�x. rad
	protected int minPulseRanging;			// ���ȗ̈�ōŏ��̃p���X��. ��sec
	protected int maxPulseRanging;			// ���ȗ̈�ōő�̃p���X��. ��sec
	protected int freq;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
	protected double minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
	protected double maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	
	private float initState;	// �������. 0~1
	private float state;  		// ���݂̏��. 0~1
	private boolean isActive;
	private boolean isAutoControlled;
	
	private String name;
	private LinearLayout layout;
	private SeekBar seekBar;
	private TextView label;


	/** �R���X�g���N�^ **/
	public ServoMotor(Util util, String name, double theta0) {  // �����p�x���󂯎��
		this.util = util;
		setSpec();
		this.initState = (float)thetaToRatio(theta0);
		this.name = name;
		init();
	}
	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h����) **/
	public abstract void setSpec();
	/*
	maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
	minTheta = -Math.PI;				// ���[�^�[�̍ŏ���]�p�x. rad
	maxTheta = Math.PI;					// ���[�^�[�̍ő��]�p�x. rad
	minThetaLimit = -Math.PI;				// ���[�^�[�̍ŏ�������]�p�x. rad
	maxThetaLimit = Math.PI;				// ���[�^�[�̍ő吧����]�p�x. rad
	minPulseRanging = 1;				// ���ȗ̈�ōŏ��̃p���X��. ��sec
	maxPulseRanging = 1000;				// ���ȗ̈�ōő�̃p���X��. ��sec
	freq = 1000;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
	minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
	maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	*/
	
	/** ������ **/
	public void init(){
		state = initState;
		isActive = false;
	}
	
	/** ����p�l���𐶐����ĕԂ� **/
	public LinearLayout getOperationLayout(Context parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 1, 10, 1);
		
		label = new TextView(parent);
		util.setText(label, name+"("+radToDeg(minThetaLimit)+" ~ "+radToDeg(maxThetaLimit)+")"+": "+radToDeg(ratioToTheta(state)));
		
        /* �V�[�N�o�[���쐬���ēo�^�@*/
		seekBar = new SeekBar(parent);
		
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onStartTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				changeState( (float) (double)progress / seekBar.getMax() );
			}
		});
		util.setProgress(seekBar, (int)(getState() * seekBar.getMax()));
		util.setEnabled(seekBar, false);
		
		layout.addView(label);
	    layout.addView(seekBar);
        
		return layout;
	}
	
	/** pwm�l��ς��� **/
	private void changeDuty(){
		if(isActive && pin!=null){
			try {
				pin.setDutyCycle((float)ratioToDuty(state));
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** state��ύX���� **/
	public void changeState(float state){
		this.state = state;
		changeDuty();	// pwm�l�̕ύX
		util.setText(label, name+"("+radToDeg(minThetaLimit)+" ~ "+radToDeg(maxThetaLimit)+")"+": "+radToDeg(ratioToTheta(state)));
		//if(isAutoControlled)	seekBar.setProgress((int)((state+1.0)*seekBar.getMax()*0.5));
	}
	/** rad����state��ύX���� **/
	public void changeStateByRad(float rad){
		changeState((float)thetaToRatio(rad));
	}
	
	public void activate() throws ConnectionLostException {
		isActive = true;
		changeDuty();
		util.setEnabled(seekBar, true);
	}
	public void disactivate() throws ConnectionLostException {
		//if(pin!=null)	pin.setDutyCycle((float) ratioToDuty(initState));
		if(pin!=null)	pin.setDutyCycle(0);
		isActive = false;
		util.setEnabled(seekBar, false);
	}
	public void disconnected() throws ConnectionLostException {
		pin = null;
		isActive = false;
		util.setEnabled(seekBar, false);
	}
	
	/** �󂯎�����ԍ�����pinNum�Ԃ�̃s�����J���đΉ��Â���(�J�����s���̐���Ԃ�) **/
	public int openPin(IOIO ioio, int num) throws ConnectionLostException{
		pin = ioio.openPwmOutput(num, getFreq());
		changeDuty();
		return pinNum;
	}
	
	/** �����������p�x(rad)�ɑ΂���, �f���[�e�B�[���Ԃ� **/
	public double thetaToDuty(double theta){
		return ratioToDuty(thetaToRatio(theta));
	}

	/** �V�[�N�o�[�̔䗦(0~1)�ɑ΂���, �f���[�e�B�[���Ԃ� **/
	public double ratioToDuty(double ratio){
		ratio = thetaToRatio(ratioToTheta(ratio));
		if( ratio < 0){
			return minDuty;
		}else if( 1 < ratio ){
			return maxDuty;
		}else{
			return minDuty + (maxDuty-minDuty) * ratio;
		}
	}
	
	/** �p�x(rad)��䗦(0~1)�ɕϊ� **/
	private double thetaToRatio(double theta){
		if(theta < minThetaLimit){
			return minThetaLimit;
		}else if(theta > maxThetaLimit){
			return maxThetaLimit;
		}else{
			return (theta-minTheta) / (maxTheta-minTheta);
		}
	}
	
	/** �䗦(0~1)���p�x(rad)�ɕϊ� **/
	private double ratioToTheta(double ratio){
		if( ratio < 0){
			return minThetaLimit;
		}else if( 1 < ratio ){
			return maxThetaLimit;
		}else{
			return minThetaLimit + ratio * (maxThetaLimit-minThetaLimit);
		}
	}
	
	/** rad��dig�ɕϊ� **/
	private int radToDeg(double rad){
		return (int) (rad / Math.PI * 180);
	}
	
	/** Getter **/
	public int getFreq(){
		return freq;
	}
	public double getMinTheta() {
		return minThetaLimit;
	}
	public double getMaxTheta() {
		return maxThetaLimit;
	}
	public double getState() {
		return state;
	}
	public int getPinNum() {
		return pinNum;
	}

	/** Setter **/
	public void setIsAutoControlled(boolean isAutoControlled){
		this.isAutoControlled = isAutoControlled;
	}
}
