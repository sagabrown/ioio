package ioio.robot.part.motor;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.MainActivity;
import ioio.robot.region.crawl.sensor.SpeedMater;
import ioio.robot.util.Util;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** モーターのスペック・設定からデューティー比を計算するクラス **/
public class DCMotor implements Motor {
	private Util util;
	private final static String TAG = "DCMotor";
	
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
	private boolean isAutoControlled;

	private String name;
	private LinearLayout layout;
	private SeekBar seekBar;
	private TextView label;
	
	private SpeedMater speedMater;
	
	
	public DCMotor(Util util, String name) {
		this.util = util;
		setSpec();
		this.initState = (float) 0.5;
		this.name = name;
		this.isAutoControlled = false;
	}
	public DCMotor(Util util, String name, double initState) {
		this(util, name);
		this.initState = (float)initState;
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
	public LinearLayout getOperationLayout(Context parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 1, 10, 1);
        /* シークバーを作成して登録　*/
		seekBar = new SeekBar(parent);
		
		label = new TextView(parent);
		util.setText(label, name+": "+state);
		
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onStartTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				changeState( (float)progress / seekBar.getMax() * 2.0f - 1.0f );
			}
		});
		util.setProgress(seekBar, (int)(getState() * seekBar.getMax()));
		util.setEnabled(seekBar, false);
		
		layout.addView(label);
	    layout.addView(seekBar);
        
		return layout;
	}
	
	/** 受け取った番号のピンを開いて対応づける **/
	public boolean openPins(IOIO ioio, int[] nums) throws ConnectionLostException{
		if(nums.length != pinNum){
			Log.e(TAG, "cannot open pin: Ellegal pinNum");
			return false;
		}
		pin1 = ioio.openPwmOutput(nums[0], getFreq());
		pin2 = ioio.openPwmOutput(nums[1], getFreq());
		changeDuty();
		util.setText(label, name+": "+state);
		return true;
	}
	
	/** pwm値を変える **/
	private void changeDuty(){
		if(isActive && pin1!=null && pin2!=null	){
			try {
				if(state > 0){	// 前進
					if(speedMater!=null)	speedMater.setFoward(true);
					pin1.setDutyCycle(state);
					pin2.setDutyCycle(0);
				}else{			// 後退
					if(speedMater!=null)	speedMater.setFoward(false);
					pin1.setDutyCycle(0);
					pin2.setDutyCycle(-state);
				}
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** stateを変更する **/
	public void changeState(float state){
		this.state = state;
		changeDuty();	// pwm値の変更
		util.setText(label, name+": "+state);
		if(isAutoControlled)	seekBar.setProgress((int)((state+1.0)*seekBar.getMax()*0.5));
	}
	
	public void activate() throws ConnectionLostException {
		isActive = true;
		changeDuty();
		if(!isAutoControlled)	util.setEnabled(seekBar, true);
	}
	public void disactivate() throws ConnectionLostException {
		if(pin1!=null)	pin1.setDutyCycle(0);
		if(pin2!=null)	pin2.setDutyCycle(0);
		isActive = false;
		util.setEnabled(seekBar, false);
	}
	
	public void disconnected() throws ConnectionLostException {
		pin1 = null;
		pin2 = null;
		isActive = false;
		util.setEnabled(seekBar, false);
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
	public void setIsAutoControlled(boolean isAutoControlled){
		this.isAutoControlled = isAutoControlled;
		if(isActive)	util.setEnabled(seekBar, !isAutoControlled);
	}
	public void setSpeedMater(SpeedMater speedMater) {
		this.speedMater = speedMater;
	}
	@Override
	public void changeStateByRad(float rad) {
		// do nothing
	}
	
}
