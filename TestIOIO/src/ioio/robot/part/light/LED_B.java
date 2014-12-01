package ioio.robot.part.light;

import ioio.robot.util.Util;

public class LED_B extends LED {

	public LED_B(Util util, String name, float initState) {
		super(util, name, initState);
	}

	@Override
	public void setSpec() {
		minPulseRanging = 0;				// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 1000;				// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 1000;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001f;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001f;	// �ő�f���[�e�B�[��. 1�ȉ�
	}

}
