package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** ���[�^�[�̃X�y�b�N�E�ݒ肩��f���[�e�B�[����v�Z����N���X **/
public abstract class ServoMotor implements Motor {
	protected static final int pinNum = 1;	// �K�v�ȃs���̐�
	protected PwmOutput pin;				// �Ή����Ă���s��
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected double minTheta;				// ���[�^�[�̍ŏ���]�p�x. rad
	protected double maxTheta;				// ���[�^�[�̍ő��]�p�x. rad
	protected int minPulseRanging;			// ���ȗ̈�ōŏ��̃p���X��. ��sec
	protected int maxPulseRanging;			// ���ȗ̈�ōő�̃p���X��. ��sec
	protected int freq;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
	protected double minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
	protected double maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	
	private float initState;	// �������. 0~1
	private float state;  		// ���݂̏��. 0~1
	private boolean isActive;
	
	private String name;
	private LinearLayout layout;
	private SeekBar seekBar;
	private TextView label;

	/** �R���X�g���N�^(�I�[�o�[���C�h����) **/
	public ServoMotor(double theta0, String name) {  // �����p�x���󂯎��
		setSpec();
		this.initState = (float)thetaToRatio(theta0);
		this.name = name;
	}
	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h����) **/
	public abstract void setSpec();
	/*
	maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
	minTheta = -Math.PI;				// ���[�^�[�̍ŏ���]�p�x. rad
	maxTheta = Math.PI;					// ���[�^�[�̍ő��]�p�x. rad
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
	public LinearLayout getOperationLayout(MainActivity parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
		
		label = new TextView(parent);
		label.setText(name+"("+radToDeg(minTheta)+" ~ "+radToDeg(maxTheta)+")"+": "+radToDeg(ratioToTheta(state)));
		
        /* �V�[�N�o�[���쐬���ēo�^�@*/
		seekBar = new SeekBar(parent);
		
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onStartTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				state = (float) ratioToDuty((double)progress / seekBar.getMax());
				try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {
				}
				/* �V�[�N�o�[���ύX���ꂽ�Ƃ�pwm�l��ς��� */
				changeDuty();
				label.setText(name+"("+radToDeg(minTheta)+" ~ "+radToDeg(maxTheta)+")"+": "+radToDeg(ratioToTheta(state)));
			}
		});
		seekBar.setProgress((int)(getState() * seekBar.getMax()));
		seekBar.setEnabled(false);
		
		layout.addView(label);
	    layout.addView(seekBar);
        
		return layout;
	}
	
	public void activate() throws ConnectionLostException {
		isActive = true;
		changeDuty();
		seekBar.setEnabled(true);
	}
	public void disactivate() throws ConnectionLostException {
		//if(pin!=null)	pin.setDutyCycle((float) ratioToDuty(initState));
		if(pin!=null)	pin.setDutyCycle(0);
		isActive = false;
		seekBar.setEnabled(false);
	}
	public void disconnected() throws ConnectionLostException {
		pin = null;
		isActive = false;
		seekBar.setEnabled(false);
	}
	
	/** pwm�l��ς��� **/
	private void changeDuty(){
		if(isActive && pin!=null){
			try {
				pin.setDutyCycle(state);
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}
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
		return (theta-getMinTheta()) / (getMaxTheta()-getMinTheta());
	}
	
	/** �䗦(0~1)���p�x(rad)�ɕϊ� **/
	private double ratioToTheta(double ratio){
		return getMinTheta() + ratio * (getMaxTheta()-getMinTheta());
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
		return minTheta;
	}
	public double getMaxTheta() {
		return maxTheta;
	}
	public double getState() {
		return state;
	}
	public int getPinNum() {
		return pinNum;
	}

	/** Setter **/
}
