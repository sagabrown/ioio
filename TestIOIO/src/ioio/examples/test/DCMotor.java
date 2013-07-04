package ioio.examples.test;

/** ���[�^�[�̃X�y�b�N�E�ݒ肩��f���[�e�B�[����v�Z����N���X **/
public class DCMotor implements Motor {
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected int minPulseRanging;			// ���ȗ̈�ōŏ��̃p���X��. ��sec
	protected int maxPulseRanging;			// ���ȗ̈�ōő�̃p���X��. ��sec
	protected int freq;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
	protected double minDuty;	// �ŏ��f���[�e�B�[��. 0�ȏ�
	protected double maxDuty;	// �ő�f���[�e�B�[��. 1�ȉ�
	
	public DCMotor() {
		setSpec();
	}
	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h����) **/
	public void setSpec(){
		maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
		minPulseRanging = 1;				// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 1000;				// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 1000;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	}
	
	/** �����������p�x(rad)�ɑ΂���, �f���[�e�B�[���Ԃ� **/
	public double getDuty(double theta){
		return 0.0;
	}

	/** �V�[�N�o�[�̔䗦(0~1)�ɑ΂���, �f���[�e�B�[���Ԃ� **/
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
