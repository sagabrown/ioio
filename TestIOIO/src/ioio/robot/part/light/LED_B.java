package ioio.robot.part.light;

import ioio.robot.util.Util;

public class LED_B extends LED {

	public LED_B(Util util, String name, float initState) {
		super(util, name, initState);
	}

	@Override
	public void setSpec() {
		minPulseRanging = 0;				// 可動な領域で最小のパルス幅. μsec
		maxPulseRanging = 1000;				// 可動な領域で最大のパルス幅. μsec
		freq = 1000;	// pwmピンの適切な周波数. minDuty~maxDutyが大体0~1になるよう定めておく. Hz
		minDuty = freq*minPulseRanging * 0.000001f;	// 最小デューティー比. 0以上
		maxDuty = freq*maxPulseRanging * 0.000001f;	// 最大デューティー比. 1以下
	}

}
