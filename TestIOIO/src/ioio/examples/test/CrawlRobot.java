package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import android.os.Handler;
import android.widget.LinearLayout;

public class CrawlRobot implements Robot {
	private Util util;
	private Motor[] motor;
	private double[] motorInitState = {0.5};  // 初期値
	private int motorNum = motorInitState.length;
	private LinearLayout layout;
	
	/* コンストラクタ */
	public CrawlRobot(Util util) {
		super();
		this.util = util;
		init();
	}
	public CrawlRobot(Util util, double[] motorInitState) {
		super();
		this.util = util;
		int len = motorNum;
		if(motorInitState.length < motorNum)	len = motorInitState.length;
		for(int i=0; i<len; i++){
			this.motorInitState[i] = motorInitState[i];
		}
		init();
	}

	/* 初期速度を設定する */
	private void init(){
		motor = new Motor[motorNum];
		motor[0] = new DCMotor(util, motorInitState[0], "くるま");  // くるま
		for( Motor m : motor ){
			m.init();
		}
		
	}
	
	public LinearLayout getLayout(MainActivity parent){
        /* 親のアクティビティに動的レイアウトを作成する　*/
        layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        /* モーターごとの操作パネルを登録　*/
		for(int i=0; i<motorNum; i++){
	        layout.addView(motor[i].getOperationLayout(parent));
		}
		return layout;
	}

	/* ピンを開いて各モーターに対応させる */
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException{
		int cnt = startPin;
        // ピンにモーターを対応させる
		for(int i=0; i<motorNum; i++){
			cnt += motor[i].openPin(ioio, cnt);
		}
		return cnt;
	}

	/* onにする */
	public void activate() throws ConnectionLostException {
		for(Motor m : motor){
			m.activate();
		}
	}
	/* offにする */
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
