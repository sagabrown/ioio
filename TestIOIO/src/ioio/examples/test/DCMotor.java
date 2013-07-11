package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** ���[�^�[�̃X�y�b�N�E�ݒ肩��f���[�e�B�[����v�Z����N���X **/
public class DCMotor implements Motor {
	protected static final int pinNum = 2;	// �K�v�ȃs���̐�
	protected PwmOutput pin1, pin2;	// �Ή����Ă���s��
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected int minPulseRanging;			// ���ȗ̈�ōŏ��̃p���X��. ��sec
	protected int maxPulseRanging;			// ���ȗ̈�ōő�̃p���X��. ��sec
	protected int freq;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
	protected double minDuty;	// �ŏ��f���[�e�B�[��. 0�ȏ�
	protected double maxDuty;	// �ő�f���[�e�B�[��. 1�ȉ�
	
	private float initState;
	private float state;
	private boolean isActive;

	private String name;
	private LinearLayout layout;
	private SeekBar seekBar;
	private TextView label;
	
	
	public DCMotor(String name) {
		setSpec();
		this.initState = (float) 0.5;
		this.name = name;
	}
	public DCMotor(double initState, String name) {
		setSpec();
		this.initState = (float)initState;
		this.name = name;
	}
	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h����) **/
	public void setSpec(){
		maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
		minPulseRanging = 1;				// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 1000;				// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 1000;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	}
	
	/** ������ **/
	public void init(){
		state = initState;
		isActive = false;
	}

	/** ����p�l���𐶐����ĕԂ� **/
	public LinearLayout getOperationLayout(MainActivity parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        /* �V�[�N�o�[���쐬���ēo�^�@*/
		seekBar = new SeekBar(parent);
		
		label = new TextView(parent);
		label.setText(name+": "+state);
		
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onStartTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				state = (float)((double)progress / seekBar.getMax() * 2.0 - 1.0);
				changeDuty();
				label.setText(name+": "+state);
			}
		});
		seekBar.setProgress((int)(getState() * seekBar.getMax()));
		seekBar.setEnabled(false);
		
		layout.addView(label);
	    layout.addView(seekBar);
        
		return layout;
	}
	
	/** �󂯎�����ԍ�����pinNum�Ԃ�̃s�����J���đΉ��Â���(�J�����s���̐���Ԃ�) **/
	public int openPin(IOIO ioio, int num) throws ConnectionLostException{
		pin1 = ioio.openPwmOutput(num, getFreq());
		pin2 = ioio.openPwmOutput(num+1, getFreq());
		changeDuty();
		label.setText(name+": "+state);
		return pinNum;
	}
	
	/** pwm�l��ς��� **/
	private void changeDuty(){
		if(isActive && pin1!=null && pin2!=null	){
			try {
				if(state < 0){
					pin1.setDutyCycle(0);
					pin2.setDutyCycle(-state);
				}else{
					pin1.setDutyCycle(state);
					pin2.setDutyCycle(0);
				}
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void activate() throws ConnectionLostException {
		isActive = true;
		changeDuty();
		seekBar.setEnabled(true);
	}
	public void disactivate() throws ConnectionLostException {
		if(pin1!=null)	pin1.setDutyCycle(0);
		if(pin2!=null)	pin2.setDutyCycle(0);
		isActive = false;
		seekBar.setEnabled(false);
	}
	
	public void disconnected() throws ConnectionLostException {
		pin1 = null;
		pin2 = null;
		isActive = false;
		seekBar.setEnabled(false);
	}
	
	/** �����������p�x(rad)�ɑ΂���, �f���[�e�B�[���Ԃ� **/
	/** DC���[�^�[�ɂ��̊T�O�͂Ȃ��c�c **/
	public double thetaToDuty(double theta){
		return 0.0;
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
	
	/** Getter **/
	public int getFreq(){
		return freq;
	}
	public double getMinTheta() {
		return 0.0;
	}
	public double getMaxTheta() {
		return 2*Math.PI;
	}
	public double getState() {
		return state;
	}
	public int getPinNum() {
		return pinNum;
	}

	/** Setter **/
	
	
}
