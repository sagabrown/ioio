package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
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
	
	/* コンストラクタ */
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

	/* 初期角度を設定する */
	private void init(){
		motor = new Motor[motorNum];
		motor[0] = new HS322HD(util, motorInitState[0], "首（ふる）");  // 首（振る）
		motor[1] = new HS322HD(util, motorInitState[1], "目");  // 目
		motor[2] = new BlueArrowBA_TS(util, motorInitState[2], "まぶた");  // まぶた
		motor[3] = new HS322HD(util, motorInitState[3], "首（傾げる）");  // 首（傾げる）
		motor[4] = new HS322HD(util, motorInitState[4], "頭");  // あたま
		motor[5] = new HS322HD(util, motorInitState[5], "首（頷く）");  // 首（頷く）
		motor[6] = new HS322HD(util, motorInitState[6], "<未使用>");  // <未使用>	
		for( Motor m : motor ){
			m.init();
		}
	}
	
	/* 操作パネルを生成して返す */
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
