package ioio.robot.part.motor;

import ioio.robot.util.Util;

/** HS-322HD�̃X�y�b�N�E�ݒ��ێ�����N���X **/
public class HS322HD extends ServoMotor implements Motor {
	public HS322HD(Util util, String name, double theta0) {
		super(util, name, theta0);
	}
	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h) **/
	public void setSpec(){
		maxSpeed = 60.0/19.0 * Math.PI/180.0;	// dig/msec * rad/dig = rad/msec
		minTheta = -Math.PI*0.5;				// ���[�^�[�̍ŏ���]�p�x. rad
		maxTheta = Math.PI*0.5;					// ���[�^�[�̍ő��]�p�x. rad
		minThetaLimit = -Math.PI*0.5;				// ���[�^�[�̍ŏ�������]�p�x. rad
		maxThetaLimit = Math.PI*0.5;				// ���[�^�[�̍ő吧����]�p�x. rad
		minPulseRanging = 600;					// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 2400;					// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 400;								// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	}
}
