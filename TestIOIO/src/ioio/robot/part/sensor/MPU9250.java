package ioio.robot.part.sensor;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.util.Log;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.part.PinOpenable;

public class MPU9250 implements PinOpenable {
	private static final boolean DEBUG_REGISTER = false;
	private static final boolean DEBUG_CALC_ATTITUDE = false;

    private static final long REGISTER_WRITE_DELAY = 10L;
	private static final long SAMPRING_RATE = 200L;
	
	// �e�Z���T�̃X���[�u�A�h���X
	//private byte MPU6050Address = (byte) 0b1101000;  // JRE1.7����Ȃ��Ǝg���Ȃ�
	private byte MPU6050Address = 0X68;
	private byte AK8975Address = 0X0C;
    // �����x�E�W���C���Z���T�̃��W�X�^�}�b�v
    private static final byte WHO_AM_I = 0X75;
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
    private static final byte ST1 = 2;
    private static final byte HXL = 3;
    private static final byte HXH = 4;
    private static final byte HYL = 5;
    private static final byte HYH = 6;
    private static final byte HZL = 7;
    private static final byte HZH = 8;
    private static final byte ST2 = 9;
    
    
	private String sensorInfo;
	private String log;
	
	private TwiMaster twi;
    private Handler handler = new android.os.Handler();
    private Timer timer;
    
	private short[] accel = new short[3];
	private short[] gyro = new short[3];
	private short[] geomag = new short[3];
	private float[] attitude = new float[3];
	
	private boolean reading = false;
	private int count;
	
	GetAccelAndGyroTask task1;
	GetGeomagTask task2;

	
    private class GetAccelAndGyroTask implements Runnable{
    	short[] accel, gyro;
    	public void setAccel(short[] accel){
    		this.accel = accel;
    	}
    	public void setGyro(short[] gyro){
    		this.gyro = gyro;
    	}
		@Override
		public void run() {
            try {
				getAccelAndGyroFromSensor(accel, gyro);
			} catch (ConnectionLostException e) {e.printStackTrace();
			} catch (InterruptedException e) {e.printStackTrace();
			}
		}
    };
    private class GetGeomagTask implements Runnable{
    	short[] geomag;
    	public void setGeomag(short[] geomag){
    		this.geomag = geomag;
    	}
		@Override
		public void run() {
            try {
				getGeomagneticFromSensor(geomag);
			} catch (ConnectionLostException e) {e.printStackTrace();
			} catch (InterruptedException e) {e.printStackTrace();
			}
		}
    };
    
	private class GetSensorDataTask extends TimerTask {
        public void run() {
            handler.post(new Runnable() {
                public void run() {
        			try {
        		    	synchronized(accel){synchronized(gyro){synchronized(geomag){synchronized(attitude){
        		    		count++;
        		    		Log.e("sensor", "start: "+count);
        		    		if(reading){
        		    			Log.e("sensor", "skip");
        		    			return;
        		    		}
        		    		reading = true;
        		    		long start = System.currentTimeMillis();
	        				//getAccelAndGyroFromSensor(accel, gyro);
	        				getMotion9FromSensor(accel, gyro, geomag);
	        				float[] newAttitude = new float[3];
	        				newAttitude = calcAttitude(attitude, SAMPRING_RATE, accel, gyro, geomag);
	        				attitude = newAttitude;
	        				sensorInfo = "<ACCEL> x: "+accel[0]+", y: "+accel[1]+", z: "+accel[2]+"\n"
        							+"<GYRO> x: "+gyro[0]+", y: "+gyro[1]+", z: "+gyro[2]+"\n"
        							+"<MAG> x: "+geomag[0]+", y: "+geomag[1]+", z: "+geomag[2]+"\n"
        							+"pitch: "+String.format("%.3f", attitude[0])
        							+", roll: "+String.format("%.3f", attitude[1])
        							+", azimuth: "+String.format("%.3f", attitude[2])+"\n";
	        				reading = false;
        		    	}}}}
        			} catch (Exception e) {e.printStackTrace();}
        			
                }
            });
        }
	};
    
    public void startCommunication(){
        // I2C�ʐM�̃^�C�}�[
        timer = new Timer(false);
        timer.schedule(new GetSensorDataTask(), 10, SAMPRING_RATE);
    }
    
    public void stopCommunication(){
    	timer.cancel();
    }
    
    public boolean getData(float[] attitude, int[] accel){
    	getAttitude(attitude);
    	getAccel(accel);
    	return true;
    }
    
    /**
     * ���f�[�^����p���p���Z�o
     */
    private static final float GYRO_FULL_RANGE = (float) ( 250 * Math.PI/180 * 0.001 ); // rad/ms
    private static final float RAWGYRO_TO_RADPERSEC = GYRO_FULL_RANGE  / 65535; // rad/ms/bit
    private static final float ACCEL_FULL_RANGE = 4f; // g
    private static final float G = (float) ( 65535 / ACCEL_FULL_RANGE ); // bit/g
    
    private float[] calcAttitude(float[] oldAttitude, long dt, short[] accel, short[] gyro){
    	short[] geomag = new short[1];
    	return calcAttitude(oldAttitude, dt, accel, gyro, geomag);
    }
    private float[] calcAttitude(float[] oldAttitude, long dt, short[] accel, short[] gyro, short[] geomag){
    	// attitude�ɂ͑O��̌����f�[�^�������Ă���Ƃ���
    	float[] attitude = new float[3];
    	
    	// pitch, roll, azimuth�̏�
    	float[] attitudeFromAccel = new float[2];  // ���R����azimuth�͏o�Ȃ�
    	float[] attitudeFromGyro = new float[3];
    	float gx, gy; // �e���܂��̏d�͉����x
    	// �d�͉����x
    	gx = (float) Math.sqrt( accel[1]*accel[1] + accel[2]*accel[2] );
    	gy = (float) Math.sqrt( accel[2]*accel[2] + accel[0]*accel[0] );
    	// �����x����pitch���Z�o
    	attitudeFromAccel[0] = (float) Math.asin(accel[1] / gx);
    	// �����x����roll���Z�o
    	attitudeFromAccel[1] = (float) Math.asin(accel[0] / gy);
    	// ���Ԃ��p�^�[���̕␳
    	if(accel[2] < 0){
    		if(Math.abs(attitudeFromAccel[0]) > Math.abs(attitudeFromAccel[1])){
	    		if(attitudeFromAccel[0] > 0){
	    			attitudeFromAccel[0] = (float) (Math.PI - attitudeFromAccel[0]);
	    		}else{
	    			attitudeFromAccel[0] = (float) (-Math.PI - attitudeFromAccel[0]);
	    		}
    		}else{
	    		if(attitudeFromAccel[1] > 0){
	    			attitudeFromAccel[1] = (float) (Math.PI - attitudeFromAccel[1]);
	    		}else{
	    			attitudeFromAccel[1] = (float) (-Math.PI - attitudeFromAccel[1]);
	    		}
    		}
    	}
    	// �W���C������pitch, roll, azimuth���Z�o
    	attitudeFromGyro[0] = (float) attitude[0] + gyro[0] * RAWGYRO_TO_RADPERSEC * dt;
    	attitudeFromGyro[1] = (float) attitude[1] + gyro[1] * RAWGYRO_TO_RADPERSEC * dt;
    	attitudeFromGyro[2] = (float) attitude[2] + gyro[2] * RAWGYRO_TO_RADPERSEC * dt;
    	
    	// pitch, roll�̃t���[�W����
    	float gBar = (float) Math.sqrt( accel[0]*accel[0] + accel[1]*accel[1] + accel[2]*accel[2]);
    	if( gBar < Math.sqrt(2)*G ){ 
    		// �قڐÎ~���Ă�ƌ���
    		attitude[0] = attitudeFromAccel[0];
    		attitude[1] = attitudeFromAccel[1];
    		if(DEBUG_CALC_ATTITUDE)	addLog(String.format("gBar: %4f < %4f ... stop", gBar, Math.sqrt(2)*G)
    				+String.format(" p: %4f , r: %4f", attitude[0], attitude[1]));
    	}else if( accel[0]<2*G && accel[1]<2*G && accel[2]<2*G ){ 
    		// �����ɓ����Ă�
    		attitude[0] = 0.1f*attitudeFromAccel[0] + 0.9f*attitudeFromGyro[0];
    		attitude[1] = 0.1f*attitudeFromAccel[1] + 0.9f*attitudeFromGyro[1];
    		if(DEBUG_CALC_ATTITUDE)	addLog("gBar: "+String.format("%4f", gBar)+" > "+String.format("%4f", Math.sqrt(2)*G)+", all < "+String.format("%4f", 2*G)+" ... move little");
    	}else{ 
    		// �����܂���
    		attitude[0] = attitudeFromGyro[0];
    		attitude[1] = attitudeFromGyro[1];
    		if(DEBUG_CALC_ATTITUDE)	addLog("move! move!");
    	}
    	
    	// azimuth�̃t���[�W����
    	if(geomag.length == 3){ // �n���C�g���H
    		// �n���C����azimuth���Z�o
    		attitude[2] = - (float) Math.atan2(geomag[0], geomag[1]);
    	}else{
    		attitude[2] = attitudeFromGyro[2];
    	}
    	
    	return attitude;
    }


	// �����x�̒l���擾����accel�Ɋi�[
	public void getAccelFromSensor(short[] accel) throws ConnectionLostException, InterruptedException{
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[6];
		readBytes(MPU6050Address, ACCEL_XOUT_H, data);

		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌���
		for(int i=0; i<accel.length; i++){
			accel[i] = (short) ( data[i*2]<<8 | data[i*2+1] & 0xFF ); // 0xFF�Ń}�X�N���Ȃ��ƕ����Ƃ��Ċg������Ă��܂�
		}
	}
	
	// �W���C���̒l���擾����gyro�Ɋi�[
	public void getGyroFromSensor(short[] gyro) throws ConnectionLostException, InterruptedException{
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[6];
		readBytes(MPU6050Address, GYRO_XOUT_H, data);

		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌���
		for(int i=0; i<gyro.length; i++){
			gyro[i] = (short)( (data[i*2]<<8) | data[i*2+1] & 0xFF );
		}
	}
	
	// �����x�ƃW���C���̒l���擾����accel, gyro�Ɋi�[
	public void getAccelAndGyroFromSensor(short[] accel, short[] gyro) throws ConnectionLostException, InterruptedException{
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[14];
		readBytes(MPU6050Address, ACCEL_XOUT_H, data);

		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌���
		for(int i=0; i<3; i++){
			accel[i] = (short) ( data[i*2]<<8 | data[i*2+1] & 0xFF ); // 0xFF�Ń}�X�N���Ȃ��ƕ����Ƃ��Ċg������Ă��܂�
			gyro[i] = (short)( data[8+i*2]<<8 | data[8+i*2+1] & 0xFF );
		}
	}
	
	// �n���C�̒l���擾����geomagnetic�Ɋi�[
	public void getGeomagneticFromSensor(short[] geomagnetic) throws ConnectionLostException, InterruptedException{
		
		// byte�f�[�^�̓ǂݍ���
		byte[] data = new byte[8];
		readBytes(AK8975Address, ST1, data);

		// data ready?
		while((data[0]&0x01) != 1){
			Thread.sleep(5);
			readBytes(AK8975Address, ST1, data);
		}
		
		// ��ʃr�b�g�Ɖ��ʃr�b�g�̌����i�����ʂ̕�����ɕ���ł���̂Œ��Ӂj
		for(int i=0; i<geomagnetic.length; i++)
			geomagnetic[i] = (short)( data[i*2+2]<<8 | data[i*2+1] & 0xFF );
		
	}
	
	// �����x�A�W���C���A�n���C�̒l���擾����accel, gyro, geomagnetic�Ɋi�[
	public void getMotion9FromSensor(short[] accel, short[] gyro, short[] geomagnetic) throws ConnectionLostException, InterruptedException{
		if(task1 != null){
			task1.setAccel(accel);
			task1.setGyro(gyro);
			new Thread(task1).start();
		}
		if(task2 != null){
			task2.setGeomag(geomag);
			new Thread(task2).start();
		}
		
		/* ���̕��@����Bluetooth�̒x�����v���I�ȃ{�g���l�b�N�ɂȂ��Ă��܂�...
		getAccelAndGyroFromSensor(accel, gyro);
		getGeomagneticFromSensor(geomagnetic);
		*/
	}
	
	
	/* 
	 * �������݂Ɋւ���֐�
	 */
	private void writeByte(int address, byte registerAddress, byte data) throws ConnectionLostException, InterruptedException{
		byte request[] = {registerAddress, data};
		byte result[] = new byte[1];
        flush(address, request, result);
	}
	private void writeBytes(int address, byte registerAddress, byte[] data) throws ConnectionLostException, InterruptedException{
		byte request[] = new byte[1+data.length];
		request[0] = registerAddress;
        System.arraycopy(data, 0, request, 1, data.length);
		byte result[] = new byte[1];
        flush(address, request, result);

    	if(DEBUG_REGISTER){
			String text = "["+String.format("%02x", request[0])+"] << ";
			for(int i=1; i<request.length; i++){
				text += String.format("%02x", request[i]);
				text += ", ";
			}
			addLog(text);
    	}
	}
	
    /* 
     * �ǂݏo���Ɋւ���֐�
     */
    protected byte readByte(int address, byte registerAddress, byte result) throws ConnectionLostException, InterruptedException {
    	byte results[] = {result};
    	readBytes(address, registerAddress, results);
    	return results[0];
    }
    protected void readBytes(int address, byte registerAddress, byte[] result) throws ConnectionLostException, InterruptedException {
    	byte request[] = {registerAddress};
    	flush(address, request, result);

    	if(DEBUG_REGISTER){
			String text = "["+String.format("%02x", registerAddress)+"] >> ";
			for(int i=0; i<result.length; i++){
				text += String.format("%02x", result[i]);
				text += ", ";
			}
			addLog(text);
    	}
    }
    
    protected void flush(int address, byte[] request, byte[] result) throws ConnectionLostException, InterruptedException {

    	long start = System.currentTimeMillis();
		twi.writeRead(address, false, request, request.length, result, result.length);
		Log.w("sensor", "flush_time: "+(System.currentTimeMillis()-start));
		
        if (request.length > 1 && REGISTER_WRITE_DELAY > 0)
                Thread.sleep(REGISTER_WRITE_DELAY);
    }

	public void close() {
		if(twi!=null){
			twi.close();
		}
		twi = null;
	}
	
	private void addLog(String text){
		if(log.length() > 600){
			log = text + "\n" + log.substring(0, 500);
		}else{
			log = text + "\n" + log;
		}
	}
	
    
	@Override
	public void init() {
		sensorInfo = "<ACCEL> x: -, y: -, z: -\n"
				+"<GYRO> x: -, y: -, z: -\n"
				+"<MAG> x: -, y: -, z: -\n";
		log = "";
		task1 = new GetAccelAndGyroTask();
		task2 = new GetGeomagTask();
	}

	@Override
	public boolean openPins(IOIO ioio, int[] nums)
			throws ConnectionLostException {
		twi = ioio.openTwiMaster(nums[0], TwiMaster.Rate.RATE_400KHz, false);
		return true;
	}

	@Override
	public void activate() throws ConnectionLostException {
		try {
			// MPU6050�̃X���[�v�r�b�g(107�ԃ��W�X�^��Bit6)��0�ɂ��Ċe�@�\��L����
			writeByte(MPU6050Address, (byte)0X6B, (byte)0X00);
			// MPU6050��I2C _BYPASS _EN(55�ԃ��W�X�^��Bit1)��1�ɂ���auxiliary I2C��L���� 
			writeByte(MPU6050Address, (byte)0X37, (byte)0X02);
			// AK8975��10�ԃ��W�X�^��0x12����������AD�ϊ��J�n
			writeByte(AK8975Address, (byte)10, (byte)0X12);
		} catch (InterruptedException e) {e.printStackTrace();
		} catch (ConnectionLostException e) {e.printStackTrace();
		}
		
		startCommunication();
	}

	@Override
	public void disactivate() throws ConnectionLostException {
		stopCommunication();
	}

	@Override
	public void disconnected() throws ConnectionLostException {
		disactivate();
		close();
	}

	@Override
	public void setIsAutoControlled(boolean tf) {
	}
	
	
    /** getter **/
    public String getSensorInfo(){
    	return sensorInfo;
    }
    public String getLog(){
    	return log;
    }

    public boolean getAccel(int[] accel){
    	if(accel.length != this.accel.length)	return false;
    	synchronized(this.accel){
	    	for(int i=0; i<accel.length; i++){
	    		accel[i] = this.accel[i];
	    	}
    	}
    	return true;
    }
    public boolean getGyro(int[] gyro){
    	if(gyro.length != this.gyro.length)	return false;
    	synchronized(this.gyro){
	    	for(int i=0; i<gyro.length; i++){
	    		gyro[i] = this.gyro[i];
	    	}
    	}
    	return true;
    }
    public boolean getGeomagnetic(int[] geomag){
    	if(geomag.length != this.geomag.length)	return false;
    	synchronized(this.geomag){
	    	for(int i=0; i<geomag.length; i++){
	    		geomag[i] = this.geomag[i];
	    	}
    	}
    	return true;
    }
    public boolean getAttitude(float[] attitude){
    	if(attitude.length != this.attitude .length)	return false;
    	synchronized(this.attitude){
	    	for(int i=0; i<attitude.length; i++){
	    		attitude[i] = this.attitude[i];
	    	}
    	}
    	return true;
    }

}
