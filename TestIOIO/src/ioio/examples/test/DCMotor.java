package ioio.examples.test;

/** モーターのスペック・設定からデューティー比を計算するクラス **/
public class DCMotor implements Motor {
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected int minPulseRanging;			// 可動な領域で最小のパルス幅. μsec
	protected int maxPulseRanging;			// 可動な領域で最大のパルス幅. μsec
	protected int freq;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
	protected double minDuty;	// 最小デューティー比. 0以上
	protected double maxDuty;	// 最大デューティー比. 1以下
	
	public DCMotor() {
		setSpec();
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
	
	/** 動かしたい角度(rad)に対して, デューティー比を返す **/
	public double getDuty(double theta){
		return 0.0;
	}

	/** シークバーの比率(0~1)に対して, デューティー比を返す **/
	public double getDuty2(double ratio){
		if( ratio < 0){
			return minDuty;
		}else if( 1 < ratio ){
			return maxDuty;
		}else{
			return minDuty + (maxDuty-minDuty) * ratio;
		}
	}
	
	public double getInitDuty(){
		return 0.0;
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
	
	/** Setter **/
	public void setTheta0(double theta0){
	}
}
