package ioio.examples.test;

/** モーターのスペック・設定からデューティー比を計算するクラス **/
public class ServoMotor implements Motor {
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected double minTheta;				// モーターの最小回転角度. rad
	protected double maxTheta;				// モーターの最大回転角度. rad
	protected int minPulseRanging;			// 可動な領域で最小のパルス幅. μsec
	protected int maxPulseRanging;			// 可動な領域で最大のパルス幅. μsec
	protected int freq;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
	protected double minDuty = freq*minPulseRanging * 0.000001;	// 最小デューティー比. 0以上
	protected double maxDuty = freq*maxPulseRanging * 0.000001;	// 最大デューティー比. 1以下
	
	private double theta0;  // 初期角度. rad
	
	public ServoMotor(double theta0) {
		this.theta0 = theta0;
		setSpec();
	}
	
	/** スペックを設定(オーバーライドする) **/
	public void setSpec(){
		maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
		minTheta = -Math.PI;				// モーターの最小回転角度. rad
		maxTheta = Math.PI;					// モーターの最大回転角度. rad
		minPulseRanging = 1;				// 可動な領域で最小のパルス幅. μsec
		maxPulseRanging = 1000;				// 可動な領域で最大のパルス幅. μsec
		freq = 1000;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// 最小デューティー比. 0以上
		maxDuty = freq*maxPulseRanging * 0.000001;	// 最大デューティー比. 1以下
	}
	
	/** 動かしたい角度(rad)に対して, デューティー比を返す **/
	public double getDuty(double theta){
		if( theta < minTheta){
			return minDuty;
		}else if( maxTheta < theta ){
			return maxDuty;
		}else{
			return minDuty + (maxDuty-minDuty) * (theta-minTheta)/(maxTheta-minTheta);
		}
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
		return getDuty(theta0);
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
	
	/** Setter **/
	public void setTheta0(double theta0){
		this.theta0 = theta0;
	}
}
