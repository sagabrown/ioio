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
	
	private boolean isReading;
	
    
	// 初期化(接続するたびに呼び出す)
	public void init(IOIO ioio, int rx, int tx) throws ConnectionLostException, IOException{
		// ピンを開く
		uart = ioio.openUart(rx, tx, 9600, Uart.Parity.NONE, Uart.StopBits.ONE);
	}
	
	public boolean getData(float[] attitude) throws IOException{
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
			Log.i(TAG, "Give me the data!!");
			try {
				Thread.sleep(20);
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
					if(System.currentTimeMillis()-ct > 500){
						isReading = false;
						Log.i(TAG, "not avairable");
						return false;
					}
				}
				text = br.readLine();
				Log.i(TAG, "trash: "+text);
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
				if(System.currentTimeMillis()-ct > 500){
					isReading = false;
					Log.i(TAG, "not avairable");
					return false;
				}
			}
			String readTextData = br.readLine();
			Log.i(TAG, "read data: "+readTextData);
			isReading = false;
			// 値を取り出してattitudeに格納
			String[] attitudeText = readTextData.split(",");
			if(attitudeText.length != 3){
				isReading = false;
				return false;
			}
			for(int i=0; i<attitude.length; i++){
				attitude[i] = Float.valueOf(attitudeText[i]);
			}
			//Log.i(TAG , "x: "+attitude[0]+", y: "+attitude[1]+", z: "+attitude[2]);
			return true;
		}
		return false;
	}
	
	public void activate(){
		isActive = true;
		// ストリームを開く
		in = uart.getInputStream();
		out = uart.getOutputStream();
		isr = new InputStreamReader(in);
		br = new BufferedReader(isr);
	}
	
	public void disactivate() throws IOException{
		isActive = false;
		//if(in!=null)	in.close();
		//if(out!=null)	out.close();
		isr = null;
		br = null;
	}
	
	public void disconnected() throws IOException{
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
