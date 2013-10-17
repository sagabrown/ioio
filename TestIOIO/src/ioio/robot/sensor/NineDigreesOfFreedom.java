package ioio.robot.sensor;

import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;

public class NineDigreesOfFreedom {
	private TwiMaster twi;
	private int MPU6050Address = 1101000;
	private int AK8975Address = 0X0C;
	
	// 初期化
	private void init(IOIO ioio) throws ConnectionLostException, InterruptedException{
		twi = ioio.openTwiMaster(1, TwiMaster.Rate.RATE_400KHz, false);
		// MPU6050のスリープビット(107番レジスタのBit6)を0にして各機能を有効化
		writeBit(MPU6050Address, (byte)107, (byte)0);
		// MPU6050のI2C _BYPASS _EN(55番レジスタのBit1)を1にしてauxiliary I2Cを有効化 
		writeBit(MPU6050Address, (byte)55, (byte)0000010);
	}
	
	private void writeBit(int address, byte registerAddress, byte data) throws ConnectionLostException, InterruptedException{
		byte[] request = new byte[] { registerAddress, data };
		byte[] response = new byte[4];
		twi.writeRead(address, false, request, request.length, response, response.length);
	}
	
	private float read(){
		return 0f;
	}
}
