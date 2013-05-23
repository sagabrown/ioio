package ioio.examples.test;

/** ���[�^�[�̃X�y�b�N�E�ݒ肩��f���[�e�B�[����v�Z����N���X **/
public class ServoMotor implements Motor {
	protected double maxSpeed;				// dig/msec * rad/dig = rad/msec
	protected double minTheta;				// ���[�^�[�̍ŏ���]�p�x. rad
	protected double maxTheta;				// ���[�^�[�̍ő��]�p�x. rad
	protected int minPulseRanging;			// ���ȗ̈�ōŏ��̃p���X��. ��sec
	protected int maxPulseRanging;			// ���ȗ̈�ōő�̃p���X��. ��sec
	protected int freq;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
	protected double minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
	protected double maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	
	private double theta0;  // �����p�x. rad
	
	public ServoMotor(double theta0) {
		this.theta0 = theta0;
		setSpec();
	}
	
	/** �X�y�b�N��ݒ�(�I�[�o�[���C�h����) **/
	public void setSpec(){
		maxSpeed = Math.PI;					// dig/msec * rad/dig = rad/msec
		minTheta = -Math.PI;				// ���[�^�[�̍ŏ���]�p�x. rad
		maxTheta = Math.PI;					// ���[�^�[�̍ő��]�p�x. rad
		minPulseRanging = 1;				// ���ȗ̈�ōŏ��̃p���X��. ��sec
		maxPulseRanging = 1000;				// ���ȗ̈�ōő�̃p���X��. ��sec
		freq = 1000;	// pwm�s���̓K�؂Ȏ��g��. minDuty~maxDuty�����0~1�ɂȂ�悤��߂Ă���. Hz
		minDuty = freq*minPulseRanging * 0.000001;	// �ŏ��f���[�e�B�[��. 0�ȏ�
		maxDuty = freq*maxPulseRanging * 0.000001;	// �ő�f���[�e�B�[��. 1�ȉ�
	}
	
	/** �����������p�x(rad)�ɑ΂���, �f���[�e�B�[���Ԃ� **/
	public double getDuty(double theta){
		if( theta < minTheta){
			return minDuty;
		}else if( maxTheta < theta ){
			return maxDuty;
		}else{
			return minDuty + (maxDuty-minDuty) * (theta-minTheta)/(maxTheta-minTheta);
		}
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
