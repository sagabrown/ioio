package ioio.robot.sensor;

import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;

public class NineDigreesOfFreedom {
	private TwiMaster twi;
	
	// �e�Z���T�̃X���[�u�A�h���X
	//private byte MPU6050Address = (byte) 0b1101000;  // JRE1.7����Ȃ��Ǝg���Ȃ�
	private byte MPU6050Address = 0X68;
	private byte AK8975Address = 0X0C;
    private static final long REGISTER_WRITE_DELAY = 200L;

    // �����x�E�W���C���Z���T�̃��W�X�^�}�b�v
    private static final byte ACCEL_XOUT_H = 59;
    private static final byte ACCEL_XOUT_L = 60;
    private static final byte ACCEL_YOUT_H = 61;
    private static final byte ACCEL_YOUT_L = 62;
    private static final byte ACCEL_ZOUT_H = 63;
    private static final byte ACCEL_ZOUT_L = 64;
    private static final byte GYRO_XOUT_H = 67;
    private static final byte GYRO_XOUT_L = 68;
    private static final byte GYRO_YOUT_H = 69;
    private static final byte GYRO_YOUT_L = 70;
    private static final byte GYRO_ZOUT_H = 71;
    private static final byte GYRO_ZOUT_L = 72;
    
    // �n���C�Z���T�̃��W�X�^�}�b�v
    private static final byte HXL = 3;
    private static final byte HXH = 4;
    private static final byte HYL = 5;
    private static final byte HYH = 6;
    private static final byte HZL = 7;
    private static final byte HZH = 8;

    private static final int ACCEL_RANGE = 4096;
    private static final int GYRO_RANGE = 4096;
    private static final int GEO_RANGE = 4096;
	
    // ���[�h�o�b�t�@�ƃ��C�g�o�b�t�@��field�Ŏ����Ă���
    private static final int READ_BUFFER_SIZE = 10; // bytes
    private static final int WRITE_BUFFER_SIZE = 10; // bytes
    private byte[] readBuffer = new byte[READ_BUFFER_SIZE];
    private byte[] writeBuffer = new byte[WRITE_BUFFER_SIZE];
    
	// ������(�ڑ����邽�тɌĂяo��)
	private void init(IOIO ioio) throws ConnectionLostException, InterruptedException{
		twi = ioio.openTwiMaster(1, TwiMaster.Rate.RATE_400KHz, false);
		// MPU6050�̃X���[�v�r�b�g(107�ԃ��W�X�^��Bit6)��0�ɂ��Ċe�@�\��L����
		writeByte(MPU6050Address, (byte)107, (byte)0);
		// MPU6050��I2C _BYPASS _EN(55�ԃ��W�X�^��Bit1)��1�ɂ���auxiliary I2C��L���� 
		//writeByte(MPU6050Address, (byte)55, (byte)0000010);
		writeByte(MPU6050Address, (byte)55, (byte)0X02);
		// �W���C���A�����x�̑��背���W���}2000deg/s,�}4g�ɐݒ�
		//byte[] range = new byte[]{(byte) 00011000,(byte) 00001000};
		byte[] range = new byte[]{(byte) 0X18,(byte) 0X08};
		writeBytes(MPU6050Address, (byte)27, range);
	}
	
	// �����x�̒l���擾����accel�Ɋi�[
	public void getAccel(int[] accel) throws ConnectionLostException, InterruptedException{
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[6];
		read(MPU6050Address, ACCEL_XOUT_H, data.length, data);

		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌���
		for(int i=0; i<accel.length; i++)
			accel[i] = (data[i*2]<<8) | data[i*2+1];
	}
	
	// �W���C���̒l���擾����gyro�Ɋi�[
	public void getGyro(int[] gyro) throws ConnectionLostException, InterruptedException{
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[6];
		read(MPU6050Address, GYRO_XOUT_H, data.length, data);

		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌���
		for(int i=0; i<gyro.length; i++)
			gyro[i] = (data[i*2]<<8) | data[i*2+1];
	}
	
	// �����x�ƃW���C���̒l���擾����accel, gyro�Ɋi�[
	public void getAccelAndGyro(int[] accel, int[] gyro) throws ConnectionLostException, InterruptedException{
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[14];
		read(MPU6050Address, ACCEL_XOUT_H, data.length, data);

		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌���
		for(int i=0; i<3; i++){
			accel[i] = (data[i*2]<<8) | data[i*2+1];
			gyro[i] = (data[8+i*2]<<8) | data[8+i*2+1];
		}
	}
	
	// �n���C�̒l���擾����geometric�Ɋi�[
	public void getGeometric(int[] geometric) throws ConnectionLostException, InterruptedException{
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[6];
		read(AK8975Address, HXL, data.length, data);

		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌���
		for(int i=0; i<geometric.length; i++)
			geometric[i] = (data[i*2]<<8) | data[i*2+1];
	}
	
	// �����x�A�W���C���A�n���C�̒l���擾����accel, gyro, geometric�Ɋi�[
	public void getMotion9(int[] accel, int[] gyro, int[] geometric) throws ConnectionLostException, InterruptedException{
		getAccelAndGyro(accel, gyro);
		getGeometric(geometric);
	}
	
	
	/* 
	 * �������݂Ɋւ���֐�
	 */
	
	private void writeByte(int address, byte registerAddress, byte data) throws ConnectionLostException, InterruptedException{
		writeBuffer[0] = registerAddress;
        writeBuffer[1] = data;
        flush(address, 2);
	}
	private void writeBytes(int address, byte registerAddress, byte[] data) throws ConnectionLostException, InterruptedException{
        writeBuffer[0] = registerAddress;
        System.arraycopy(data, 0, writeBuffer, 1, data.length);
        flush(address, 1 + data.length);
	}
    protected void flush(int address, int length) throws ConnectionLostException, InterruptedException {
        int writeSize = length;
        int readSize = 0;
		twi.writeRead(address, false, writeBuffer, writeSize, readBuffer, readSize);
        if (REGISTER_WRITE_DELAY > 0)
                Thread.sleep(REGISTER_WRITE_DELAY);
    }
	
    /* 
     * �ǂݏo���Ɋւ���֐�
     */
    protected void read(int address, byte registerAddress, int length, byte[] data) throws ConnectionLostException, InterruptedException {
	    writeBuffer[0] = registerAddress;
	    int writeSize = 1;
	    int readSize = length;
	    twi.writeRead(address, false, writeBuffer, writeSize, data, readSize);
    }
    
}
