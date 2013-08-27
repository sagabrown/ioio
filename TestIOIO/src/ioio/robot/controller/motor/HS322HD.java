package ioio.robot.controller.motor;

import ioio.robot.util.Util;

/** HS-322HD�̃X�y�b�N�E�ݒ��ێ�����N���X **/
public class HS322HD extends ServoMotor implements Motor {
	public HS322HD(Util util, double theta0, String name) {
		super(util, theta0, name);
	}
	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h) **/
	public void setSpec(){
		maxSpeed = 60.0/19.0 * Math.PI/180.0;	// dig/msec * rad/dig = rad/msec
		minTheta = -Math.PI*0.5;				// ���[�^�[�̍ŏ���]�p�x. rad
		maxTheta = Math.PI*0.5;					// ���[�^�[�̍ő��]�p�x. rad
		minPulseRanging = 600;					// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 2400;					// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 400;								// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	}
}
