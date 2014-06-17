package ioio.robot.part.sensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.util.Log;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.part.PinOpenable;

public class ArduinoSensor {
	private Uart uart;
	private InputStream in;
	private OutputStream out;
	private InputStreamReader isr;
	private BufferedReader br;
	private boolean isActive = false;
	private final static String TAG = "ArduinoSensor";
	
	private boolean isReading;
	private static final boolean DEBUG = false;
	
    
	// 初期化(接続するたびに呼び出す)
	public int openPins(IOIO ioio, int rx, int tx) throws ConnectionLostException {
		// ピンを開く
		uart = ioio.openUart(rx, tx, 9600, Uart.Parity.NONE, Uart.StopBits.ONE);
		return 2;
	}
	
	public boolean getData(float[] attitude, int[] accel) throws IOException{
		if(isActive && !isReading){
			isReading = true;
			/*
			// データたまり過ぎのとき破棄して戻る
			if(in.available() > 500){
				isReading = false;
				while(in.available()>0)	in.read();
				return false;
			}*/
			// データ取得依頼
			out.write(1);
			if(DEBUG)	Log.i(TAG, "Give me the data!!");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// ゴミデータを捨てる
			String text;
			long ct;
			do{
				// あまりにも長い間データが得られなかったらあきらめる
				ct = System.currentTimeMillis();
				while(in.available() <= 0){
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(System.currentTimeMillis()-ct > 100){
						isReading = false;
						Log.i(TAG, "not available");
						return false;
					}
				}
				text = br.readLine();
				if(DEBUG)	Log.i(TAG, "trash: "+text);
			}while(!text.equals("0"));
			// データを読む
			// あまりにも長い間データが得られなかったらあきらめる
			ct = System.currentTimeMillis();
			while(in.available() <= 0){
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(System.currentTimeMillis()-ct > 100){
					isReading = false;
					Log.i(TAG, "not available");
					return false;
				}
			}
			String readTextData = br.readLine();
			if(DEBUG)	Log.i(TAG, "read data: "+readTextData);
			isReading = false;
			
			// 値を切り分け
			String[] dataTexts = readTextData.split(":");
			if(dataTexts.length != 2)	return false;
			
			// 値を取り出してattitudeに格納
			String[] attitudeText = dataTexts[0].split(",");
			if(attitudeText.length != attitude.length+1 || !attitudeText[0].equals("angles"))	return false;
			for(int i=0; i<attitude.length; i++)	attitude[i] = Float.valueOf(attitudeText[i+1]);
			//Log.i(TAG , "pitch: "+attitude[0]+", roll: "+attitude[1]+", azimuth: "+attitude[2]);
			
			// 値を取り出してaccelに格納
			String[] accelText = dataTexts[1].split(",");
			if(accelText.length != accel.length+1 || !accelText[0].equals(" accels"))	return false;
			for(int i=0; i<accel.length; i++){
				accel[i] = Integer.valueOf(accelText[i+1].trim());
			}
			//Log.i(TAG, "Load Success");
			return true;
		}else{
			Log.i(TAG, "skipped! already reading");
			return false;
		}
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
		isActive = false;
		//if(in!=null)	in.close();
		//if(out!=null)	out.close();
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
