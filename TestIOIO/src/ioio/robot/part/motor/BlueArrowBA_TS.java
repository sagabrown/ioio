package ioio.robot.part.motor;

import ioio.robot.util.Util;

/** Blue Arrow BA-TS-3.6 のスペック・設定を保持するクラス **/
public class BlueArrowBA_TS extends ServoMotor implements Motor {
	public BlueArrowBA_TS(Util util, String name, double theta0) {
		super(util, name, theta0);
		setSpec();
	}
	
	/** スペックを設定(オーバーライド) **/
	public void setSpec(){
		maxSpeed = 60.0/19.0 * Math.PI/180.0;	// dig/msec * rad/dig = rad/msec
		minTheta = -Math.PI*0.5;				// モーターの最小回転角度. rad
		maxTheta = Math.PI*0.5;					// モーターの最大回転角度. rad
		minThetaLimit = -Math.PI*0.25;				// モーターの最小制限回転角度. rad
		maxThetaLimit = Math.PI*0.25;				// モーターの最大制限回転角度. rad
		minPulseRanging = 500;					// 可動な領域で最小のパルス幅. μsec
		maxPulseRanging = 2500;					// 可動な領域で最大のパルス幅. μsec
		freq = 50;								// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// 最小デューティー比. 0以上
		maxDuty = freq*maxPulseRanging * 0.000001;	// 最大デューティー比. 1以下
	}
}
