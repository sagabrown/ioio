package ioio.robot.controller.motor;

import ioio.robot.util.Util;

/** HS-322HDのスペック・設定を保持するクラス **/
public class HS322HD extends ServoMotor implements Motor {
	public HS322HD(Util util, double theta0, String name) {
		super(util, theta0, name);
	}
	
	/** スペックを設定(オーバーライド) **/
	public void setSpec(){
		maxSpeed = 60.0/19.0 * Math.PI/180.0;	// dig/msec * rad/dig = rad/msec
		minTheta = -Math.PI*0.5;				// モーターの最小回転角度. rad
		maxTheta = Math.PI*0.5;					// モーターの最大回転角度. rad
		minPulseRanging = 600;					// 可動な領域で最小のパルス幅. μsec
		maxPulseRanging = 2400;					// 可動な領域で最大のパルス幅. μsec
		freq = 400;								// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// 最小デューティー比. 0以上
		maxDuty = freq*maxPulseRanging * 0.000001;	// 最大デューティー比. 1以下
	}
}
