package ioio.robot.part.light;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.MainActivity;
import ioio.robot.part.PinOpenable;
import ioio.robot.util.Util;

public abstract class LED implements PinOpenable {
	private PwmOutput pin;
	protected static final int pinNum = 1;	// �K�v�ȃs���̐�
	protected int minPulseRanging;			// ���ȗ̈�ōŏ��̃p���X��. ��sec
	protected int maxPulseRanging;			// ���ȗ̈�ōő�̃p���X��. ��sec
	protected int freq;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
	protected float minDuty;	// �ŏ��f���[�e�B�[��. 0�ȏ�
	protected float maxDuty;	// �ő�f���[�e�B�[��. 1�ȉ�
	private float initState;
	private float state;
	private boolean isActive;

	private LinearLayout layout;
	private SeekBar seekBar;
	private TextView label;
	
	private Util util;
	private final static String TAG = "LED";
	private String name;
	private boolean isAutoControlled;
	
	public LED(Util util, String name, float initState) {
		this.util = util;
		this.name = name;
		this.initState = initState;
		setSpec();
		init();
	}

	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h����) **/
	abstract public void setSpec();
		/*
		minPulseRanging = 0;				// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 1000;				// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 1000;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001f;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001f;	// �ő�f���[�e�B�[��. 1�ȉ�
		*/
	
	/** ������ **/
	public void init(){
		state = initState;
		isActive = false;
	}

	/** �󂯎�����ԍ��̃s�����J���đΉ��Â���(�J�����s���̐�1��Ԃ�) **/
	public boolean openPins(IOIO ioio, int[] nums) throws ConnectionLostException{
		if(nums.length != pinNum){
			Log.e(TAG, "cannot open pin: Ellegal pinNum");
			return false;
		}
		pin = ioio.openPwmOutput(nums[0], freq);
		changeDuty();
		return true;
	}

	/** ����p�l���𐶐����ĕԂ� **/
	public LinearLayout getOperationLayout(Context parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 1, 5, 1);
        /* �V�[�N�o�[���쐬���ēo�^�@*/
		seekBar = new SeekBar(parent);
		
		label = new TextView(parent);
		util.setText(label, name+": "+state);
		
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onStartTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				changeState( (float)progress / seekBar.getMax() , true);
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
				pin.setDutyCycle( minDuty + (maxDuty-minDuty)*state );
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}
	}

	/** state��ύX���� **/
	public void changeState(float state, boolean fromUser){
		this.state = state;
		changeDuty();	// pwm�l�̕ύX
		if(!fromUser)	seekBar.setProgress((int)(state*seekBar.getMax()));
	}
	
	public void activate() throws ConnectionLostException {
		isActive = true;
		changeDuty();
		util.setEnabled(seekBar, true);
	}
	public void disactivate() throws ConnectionLostException {
		if(pin!=null)	pin.setDutyCycle(0);
		isActive = false;
		util.setEnabled(seekBar, false);
	}
	
	public void disconnected() throws ConnectionLostException {
		pin = null;
		isActive = false;
		util.setEnabled(seekBar, false);
	}
	public double getState() {
		return state;
	}

	public void setIsAutoControlled(boolean isAutoControlled){
		this.isAutoControlled = isAutoControlled;
		if(isActive)	util.setEnabled(seekBar, !isAutoControlled);
	}

}
