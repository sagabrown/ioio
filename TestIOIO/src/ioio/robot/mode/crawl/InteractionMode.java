package ioio.robot.mode.crawl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.mode.AutoMode;
import ioio.robot.part.sensor.TouchSensor;
import ioio.robot.region.crawl.Ears;
import ioio.robot.region.crawl.Eyes;
import ioio.robot.region.crawl.Wheel;
import ioio.robot.robot.CrawlRobot;
import ioio.robot.util.Util;

public class InteractionMode extends AutoMode {
	private static final boolean DEBUG = false;
	private final static String TAG = "interactionMode";
	private Wheel wheel;
	private Ears ears;
	private Eyes eyes;
	private TouchSensor[] touchSensor;
	
	public InteractionMode() {
		button = null;
		isAuto = false;
		ses = new ScheduledExecutorService[1];
	}
	
	public void setParams(Util util, CrawlRobot robot){
		this.util = util;
		this.robot = robot;
		this.wheel = robot.wheel;
		this.ears = robot.ears;
		this.eyes = robot.eyes;
		this.touchSensor = robot.touchSensor;
	}
	
	protected void generateButton(Context context){
        // オート切り替えのボタン
        button = new ToggleButton(context);
        button.setTextOn("interact");
        button.setTextOff("indep");
        button.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// 競合するオートモードをオフに
					if(wheel.getOwner() != null && wheel.getOwner() != InteractionMode.this){
						wheel.getOwner().buttonOff();
					}
					if(ears.getOwner() != null && ears.getOwner() != InteractionMode.this){
						ears.getOwner().buttonOff();
					}
					if(eyes.getOwner() != null && eyes.getOwner() != InteractionMode.this){
						eyes.getOwner().buttonOff();
					}
					start();
				}else{
					stop();
				}
			}
        });
        button.setText("indep");
	}

	@Override
	public void start() {
		if(isAuto)	return;
		isAuto = true;
        // タイマーを作成する
        ses[0] = Executors.newSingleThreadScheduledExecutor();
        // 50msごとにtaskを実行する
    	Log.i(TAG, "InteractionStarted");
    	ses[0].scheduleAtFixedRate(task, 0L, 50L, TimeUnit.MILLISECONDS);
		
    	wheel.setIsAutoControlled(this);
		ears.setIsAutoControlled(this);
		eyes.setIsAutoControlled(this);
	}

	@Override
	public void stop() {
		if(!isAuto)	return;
		isAuto = false;
		wheel.stop();
		wheel.setIsAutoControlled(null);
		ears.setIsAutoControlled(null);
		eyes.setIsAutoControlled(null);

		// タイマーを停止する
		if(ses[0] == null)	return;
		ses[0].shutdown();
    	Log.i(TAG, "InteractionStopped");
	}
	


	/** 自動制御のタスク **/
    private final Runnable task = new Runnable(){
		public void run() {
			try {
				for(TouchSensor t: touchSensor)	t.addData();
			} catch (InterruptedException e) {e.printStackTrace();
			} catch (ConnectionLostException e) {e.printStackTrace();
			}
			int headTouchState = touchSensor[0].checkTouch();
			int backTouchState = touchSensor[1].checkTouch();

			try {
				if(backTouchState == TouchSensor.LONG_TOUCH){
					wheel.goForward();
					if(DEBUG)	Log.i(TAG, "go");
				}else if(headTouchState == TouchSensor.LONG_TOUCH){
					wheel.goBackward();
					if(DEBUG)	Log.i(TAG, "back");
				}else if(backTouchState == TouchSensor.SHORT_TOUCH){
					wheel.stop();
					robot.sad();
					if(DEBUG)	Log.i(TAG, "sad");
				}else if(headTouchState == TouchSensor.SHORT_TOUCH){
					wheel.stop();
					robot.happy();
					if(DEBUG)	Log.i(TAG, "happy");
				}else if(headTouchState == TouchSensor.ATTACK_TOUCH){
					wheel.stop();
					robot.angry();
					if(DEBUG)	Log.i(TAG, "angry");
				}else if(backTouchState == TouchSensor.ATTACK_TOUCH){
						wheel.stop();
					if(DEBUG)	Log.i(TAG, "stop only");
				}else{
					if(DEBUG)	Log.i(TAG, "none");
				};
			} catch (InterruptedException e) {e.printStackTrace();}
			}
    };
}
