package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import android.util.Log;
import android.widget.LinearLayout;

public class TEROOS implements Robot {
	private static final double pi = Math.PI;
	private Motor[] motor;
	private double[] motorInitState = {0, 0, -pi*0.25, 0, 0, 0, 0};
	private int motorNum = motorInitState.length;
	private LinearLayout layout;
	
	/* コンストラクタ */
	public TEROOS() {
		super();
		init();
	}
	public TEROOS(double[] motorInitState) {
		super();
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
		motor[0] = new HS322HD(motorInitState[0], "首（ふる）");  // 首（振る）
		motor[1] = new HS322HD(motorInitState[1], "目");  // 目
		motor[2] = new BlueArrowBA_TS(motorInitState[2], "まぶた");  // まぶた
		motor[3] = new HS322HD(motorInitState[3], "首（傾げる）");  // 首（傾げる）
		motor[4] = new HS322HD(motorInitState[4], "頭");  // あたま
		motor[5] = new HS322HD(motorInitState[5], "首（頷く）");  // 首（頷く）
		motor[6] = new HS322HD(motorInitState[6], "<未使用>");  // <未使用>	
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
			Log.d("debug", "cnt="+cnt);
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
