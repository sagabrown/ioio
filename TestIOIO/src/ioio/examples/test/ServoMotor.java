package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** モーターのスペック・設定からデューティー比を計算するクラス **/
public abstract class ServoMotor implements Motor {
	protected static final int pinNum = 1;	// 必要なピンの数
	protected PwmOutput pin;				// 対応しているピン
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected double minTheta;				// モーターの最小回転角度. rad
	protected double maxTheta;				// モーターの最大回転角度. rad
	protected int minPulseRanging;			// 可動な領域で最小のパルス幅. μsec
	protected int maxPulseRanging;			// 可動な領域で最大のパルス幅. μsec
	protected int freq;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
	protected double minDuty = freq*minPulseRanging * 0.000001;	// 最小デューティー比. 0以上
	protected double maxDuty = freq*maxPulseRanging * 0.000001;	// 最大デューティー比. 1以下
	
	private float initState;	// 初期状態. 0~1
	private float state;  		// 現在の状態. 0~1
	private boolean isActive;
	
	private String name;
	private LinearLayout layout;
	private SeekBar seekBar;
	private TextView label;

	/** コンストラクタ(オーバーライドする) **/
	public ServoMotor(double theta0, String name) {  // 初期角度を受け取る
		setSpec();
		this.initState = (float)thetaToRatio(theta0);
		this.name = name;
	}
	
	/** スペックを設定(オーバーライドする) **/
	public abstract void setSpec();
	/*
	maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
	minTheta = -Math.PI;				// モーターの最小回転角度. rad
	maxTheta = Math.PI;					// モーターの最大回転角度. rad
	minPulseRanging = 1;				// 可動な領域で最小のパルス幅. μsec
	maxPulseRanging = 1000;				// 可動な領域で最大のパルス幅. μsec
	freq = 1000;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
	minDuty = freq*minPulseRanging * 0.000001;	// 最小デューティー比. 0以上
	maxDuty = freq*maxPulseRanging * 0.000001;	// 最大デューティー比. 1以下
	*/
	
	/** 初期化 **/
	public void init(){
		state = initState;
		isActive = false;
	}
	
	/** 操作パネルを生成して返す **/
	public LinearLayout getOperationLayout(MainActivity parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
		
		label = new TextView(parent);
		label.setText(name+"("+radToDeg(minTheta)+" ~ "+radToDeg(maxTheta)+")"+": "+radToDeg(ratioToTheta(state)));
		
        /* シークバーを作成して登録　*/
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
				/* シークバーが変更されたときpwm値を変える */
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
	
	/** pwm値を変える **/
	private void changeDuty(){
		if(isActive && pin!=null){
			try {
				pin.setDutyCycle(state);
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** 受け取った番号からpinNumぶんのピンを開いて対応づける(開いたピンの数を返す) **/
	public int openPin(IOIO ioio, int num) throws ConnectionLostException{
		pin = ioio.openPwmOutput(num, getFreq());
		changeDuty();
		return pinNum;
	}
	
	/** 動かしたい角度(rad)に対して, デューティー比を返す **/
	public double thetaToDuty(double theta){
		return ratioToDuty(thetaToRatio(theta));
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
	
	/** 角度(rad)を比率(0~1)に変換 **/
	private double thetaToRatio(double theta){
		return (theta-getMinTheta()) / (getMaxTheta()-getMinTheta());
	}
	
	/** 比率(0~1)を角度(rad)に変換 **/
	private double ratioToTheta(double ratio){
		return getMinTheta() + ratio * (getMaxTheta()-getMinTheta());
	}
	
	/** radをdigに変換 **/
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
