package ioio.robot.light;

import ioio.robot.util.Util;

public class LED_R extends LED {

	public LED_R(Util util, String name) {
		super(util, name);
	}

	@Override
	public void setSpec() {
		minPulseRanging = 0;				// 可動な領域で最小のパルス幅. μsec
		maxPulseRanging = 600;				// 可動な領域で最大のパルス幅. μsec
		freq = 1000;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
		minDuty = freq*minPulseRanging * 0.000001f;	// 最小デューティー比. 0以上
		maxDuty = freq*maxPulseRanging * 0.000001f;	// 最大デューティー比. 1以下
	}

}
