package ioio.robot.region.crawl;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
	
	boolean alreadyFlicking = false;

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
	public void flick(){
		
	}

	/** 点滅のマネジメント **/
	public void manageFlick(boolean isFlick){
		if(ses != null){
        	if(isFlick){
        		if(!alreadyFlicking){
					if( intrruptTask() ){
						ses = Executors.newSingleThreadScheduledExecutor();
		    	        Log.i(TAG, "eyesFlickStarted");
		    	        ses.scheduleAtFixedRate(eyesFlickTask, 0L, 200L, TimeUnit.MILLISECONDS);
					}
	    	        alreadyFlicking = true;
        		}
        	}else{
        		if(alreadyFlicking){
					if( intrruptTask() ){
		    	        Log.i(TAG, "eyesFlickEnd");
        			}
	    	        alreadyFlicking = false;
	        		led[0].setLuminous(1f);
        		}
        	}
		}
	}

    /** 目の点滅タスク **/
    private final Runnable eyesFlickTask = new Runnable(){
    	private int[] taskLoopFlickFast = {1,0,1,0,1,0,1,0,1,0,1,0};
    	private int[] taskLoopFlickSlow = {1,1,0,0,1,1,0,0,1,1,0,0};
    	private int taskCnt = 0;
        @Override
        public void run() {
        	if(!isAuto)	return;
        	Log.d("eyeFlick", "running...");
			switch(taskLoopFlickFast[taskCnt]){
			case 0:		led[0].setLuminous(0f);		break;
			case 1:		led[0].setLuminous(1f);		break;
			}
        	if(taskCnt==taskLoopFlickFast.length-1)	taskCnt = 0;
        	else									taskCnt++;
        }
    };

}
