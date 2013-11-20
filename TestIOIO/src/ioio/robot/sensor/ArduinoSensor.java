package ioio.robot.sensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.util.Log;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

public class ArduinoSensor {
	private Uart uart;
	private InputStream in;
	private OutputStream out;
	private InputStreamReader isr;
	private BufferedReader br;
	private boolean isActive = false;
	private final static String TAG = "ArduinoSensor";
	
    
	// 初期化(接続するたびに呼び出す)
	public void init(IOIO ioio, int rx, int tx) throws ConnectionLostException{
		// ピンを開く
		uart = ioio.openUart(rx, tx, 9600, Uart.Parity.NONE, Uart.StopBits.ONE);
	}
	
	public void getData(float[] attitude) throws IOException{
		if(isActive){
			out.write(1);
			String readTextData = br.readLine();
			String[] attitudeText = readTextData.split(",");
			if(attitudeText.length != 3)	return;
			for(int i=0; i<attitude.length; i++){
				attitude[i] = Float.valueOf(attitudeText[i]);
				Log.i(TAG , i+": "+attitude[i]);
			}
		}
		return;
	}
	
	public void activate(){
		isActive = true;
		// ストリームを開く
		in = uart.getInputStream();
		out = uart.getOutputStream();
		isr = new InputStreamReader(in);
		br = new BufferedReader(isr);
	}
	
	public void disactivate(){
		isActive = true;
		in = null;
		out = null;
		isr = null;
		br = null;
	}
	
	public void disconnected(){
		disactivate();
		close();
	}
	
	
	public void close() {
		if(uart!=null){
			uart.close();
		}
		uart = null;
	}
    
}
