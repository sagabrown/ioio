package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** モーターのスペック・設定からデューティー比を計算するクラス **/
public class DCMotor implements Motor {
	protected static final int pinNum = 2;	// 必要なピンの数
	protected PwmOutput pin1, pin2;	// 対応しているピン
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected int minPulseRanging;			// 可動な領域で最小のパルス幅. μsec
	protected int maxPulseRanging;			// 可動な領域で最大のパルス幅. μsec
	protected int freq;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
	protected double minDuty;	// 最小デューティー比. 0以上
	protected double maxDuty;	// 最大デューティー比. 1以下
	
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
	
	/** スペックを設定(オーバーライドする) **/
	public void setSpec(){
		maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
		minPulseRanging = 1;				// 可動な領域で最小のパルス幅. μsec
		maxPulseRanging = 1000;				// 可動な領域で最大のパルス幅. μsec
		freq = 1000;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// 最小デューティー比. 0以上
		maxDuty = freq*maxPulseRanging * 0.000001;	// 最大デューティー比. 1以下
	}
	
	/** 初期化 **/
	public void init(){
		state = initState;
		isActive = false;
	}

	/** 操作パネルを生成して返す **/
	public LinearLayout getOperationLayout(MainActivity parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        /* シークバーを作成して登録　*/
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
	
	/** 受け取った番号からpinNumぶんのピンを開いて対応づける(開いたピンの数を返す) **/
	public int openPin(IOIO ioio, int num) throws ConnectionLostException{
		pin1 = ioio.openPwmOutput(num, getFreq());
		pin2 = ioio.openPwmOutput(num+1, getFreq());
		changeDuty();
		label.setText(name+": "+state);
		return pinNum;
	}
	
	/** pwm値を変える **/
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
	
	/** 動かしたい角度(rad)に対して, デューティー比を返す **/
	/** DCモーターにこの概念はない…… **/
	public double thetaToDuty(double theta){
		return 0.0;
	}

	/** シークバーの比率(0~1)に対して, デューティー比を返す **/
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
