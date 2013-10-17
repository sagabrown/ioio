package ioio.robot.light;

import ioio.robot.util.Util;

public class LED_R extends LED {

	public LED_R(Util util, String name) {
		super(util, name);
	}

	@Override
	public void setSpec() {
		minPulseRanging = 0;				// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 600;				// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 1000;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001f;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001f;	// �ő�f���[�e�B�[��. 1�ȉ�
	}

}