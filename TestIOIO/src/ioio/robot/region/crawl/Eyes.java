package ioio.robot.region.crawl;

import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import ioio.robot.mode.AutoMode;
import ioio.robot.part.light.FullColorLED;
import ioio.robot.region.Region;
import ioio.robot.util.Util;

public class Eyes extends Region {
	private final static String TAG = "Eyes";
	private final static double[] defaultLedInitState = {0f};

	private FullColorLED[] led;
	private int ledNum = defaultLedInitState.length;
	private LinearLayout layout;

	/** コンストラクタ **/
	public Eyes(Util util) {
		this(util, defaultLedInitState);
	}
	
	/** 初期化つきコンストラクタ **/
	public Eyes(Util util, double[] ledInitState) {
		this.util = util;
		led = new FullColorLED[ledNum];
		led[0] = new FullColorLED(util, "Eyes");
		for( FullColorLED l : led )	l.init();
		part = led;
	}

	@Override
	public LinearLayout getLayout(Context context) {
		layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 1, 5, 1);
        // - 操作パネルを登録
		for(int i=0; i<ledNum; i++)	layout.addView(led[i].getOperationLayout(context));
		return layout;
	}
	

	public void red(){
		led[0].red();
	}
	public void green(){
		led[0].green();
	}
	public void blue(){
		led[0].blue();
	}
	public void setColor(float[] color){
		led[0].setColor(color);
	}
	public void setLuminous(float lum){
		led[0].setLuminous(lum);
	}

}
