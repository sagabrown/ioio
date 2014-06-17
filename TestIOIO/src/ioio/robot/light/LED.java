package ioio.robot.light;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.util.Util;

public abstract class LED {
	private PwmOutput pin;
	protected int minPulseRanging;			// 可動な領域で最小のパルス幅. μsec
	protected int maxPulseRanging;			// 可動な領域で最大のパルス幅. μsec
	protected int freq;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
	protected float minDuty;	// 最小デューティー比. 0以上
	protected float maxDuty;	// 最大デューティー比. 1以下
	private float initState;
	private float state;
	private boolean isActive;

	private LinearLayout layout;
	private SeekBar seekBar;
	private TextView label;
	
	private Util util;
	private String name;
	
	public LED(Util util, String name) {
		this.util = util;
		this.name = name;
		this.initState = 0f;
		setSpec();
		init();
	}

	
	/** スペックを設定(オーバーライドする) **/
	abstract public void setSpec();
		/*
		minPulseRanging = 0;				// 可動な領域で最小のパルス幅. μsec
		maxPulseRanging = 1000;				// 可動な領域で最大のパルス幅. μsec
		freq = 1000;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
		minDuty = freq*minPulseRanging * 0.000001f;	// 最小デューティー比. 0以上
		maxDuty = freq*maxPulseRanging * 0.000001f;	// 最大デューティー比. 1以下
		*/
	
	/** 初期化 **/
	public void init(){
		state = initState;
		isActive = false;
	}

	/** 受け取った番号のピンを開いて対応づける(開いたピンの数1を返す) **/
	public int openPin(IOIO ioio, int num) throws ConnectionLostException{
		pin = ioio.openPwmOutput(num, freq);
		changeDuty();
		return 1;
	}

	/** 操作パネルを生成して返す **/
	public LinearLayout getOperationLayout(Context parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 1, 5, 1);
        /* シークバーを作成して登録　*/
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
	
	/** pwm値を変える **/
	private void changeDuty(){
		if(isActive && pin!=null){
			try {
				pin.setDutyCycle( minDuty + (maxDuty-minDuty)*state );
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** stateを変更する **/
	public void changeState(float state, boolean fromUser){
		this.state = state;
		changeDuty();	// pwm値の変更
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

}
