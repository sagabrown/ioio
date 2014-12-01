package ioio.robot.mode.crawl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import ioio.robot.mode.AutoMode;
import ioio.robot.region.crawl.Ears;
import ioio.robot.region.crawl.Eyes;
import ioio.robot.region.crawl.Wheel;
import ioio.robot.region.crawl.sensor.SensorTester;
import ioio.robot.region.crawl.sensor.TrailPoint;
import ioio.robot.region.crawl.sensor.TrailView;
import ioio.robot.robot.CrawlRobot;
import ioio.robot.util.Util;

public class PointOutMode extends AutoMode {
	private final static String TAG = "PointOutMode";
	private Wheel wheel;
	private Ears ears;
	private Eyes eyes;
	private SensorTester sensor;
	private int shoulder, back, leg;	// 代表点のインデックス
	
	public PointOutMode() {
		button = null;
		isAuto = false;
		ses = new ScheduledExecutorService[2];
	}
	
	public void setParams(Util util, CrawlRobot robot){
		this.util = util;
		this.robot = robot;
		this.wheel = robot.wheel;
		this.ears = robot.ears;
		this.eyes = robot.eyes;
		this.sensor = robot.sensor;
	}
	
	protected void generateButton(Context context){
        // オート切り替えのボタン
        button = new ToggleButton(context);
        button.setTextOn("point-out");
        button.setTextOff("ignore");
        button.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// 競合するオートモードをオフに
					if(wheel.getOwner() != null && wheel.getOwner() != PointOutMode.this){
						wheel.getOwner().buttonOff();
					}
					if(ears.getOwner() != null && ears.getOwner() != PointOutMode.this){
						ears.getOwner().buttonOff();
					}
					if(eyes.getOwner() != null && eyes.getOwner() != PointOutMode.this){
						eyes.getOwner().buttonOff();
					}
					start();
				}else{
					stop();
				}
			}
        });
        button.setText("ignore");
	}

	@Override
	public void start() {
		if(isAuto)	return;
		isAuto = true;
        // タイマーを作成する
        ses[0] = Executors.newSingleThreadScheduledExecutor();
        // 100msごとにtaskを実行する
    	Log.i(TAG, "PointOutStarted");
    	ses[0].scheduleAtFixedRate(task, 0L, 100L, TimeUnit.MILLISECONDS);

    	wheel.setIsAutoControlled(this);
		ears.setIsAutoControlled(this);
    	eyes.setIsAutoControlled(this);
	}

	@Override
	public void stop() {
		if(!isAuto)	return;
		isAuto = false;
		robot.stand();
    	wheel.setIsAutoControlled(null);
		ears.setIsAutoControlled(null);
		eyes.setIsAutoControlled(null);

		// タイマーを停止する
		if(ses[0] == null)	return;
		ses[0].shutdown();
    	Log.i(TAG, "PointOutStopped");
	}
	


	/** 計測結果提示のタスク **/
    private final Runnable task = new Runnable(){
    	private boolean isPointingOutSlouching;
    	private boolean isPointingOutKneeShaking;
    	private int goal;
    	private boolean goalReached;
    	
        @Override
        public void run() {
        	Log.d("PointOut", "running...");
        	if(!isAuto)	return;
        	
        	float dif = sensor.getPitchDifference();
        	int tpType = sensor.getNowTpType();
        	
        	if(isPointingOutSlouching){			// 猫背指摘モード
        		if(!isSlouching()){	// 解消されたとき
        			endPointingOut();
        		}else{
	        		if(goalReached){	// 目的地点についた
	        			// 前後移動
	        		}else{				// 目的地点到達前
	        			toGoal();
	        		}
        		}
        	}else if(isPointingOutKneeShaking){	// 貧乏揺すり指摘モード
        		if(!isKneeShaking()){	// 解消されたとき
        			endPointingOut();
        		}else{
        			if(goalReached){	// 目的地点についた
        				// わちゃわちゃ
	        		}else{				// 目的地点到達前
	        			toGoal();
	        		}
        		}
        	}else{								// 平常時
	        	switch(tpType){
	        	case TrailPoint.SHOLDER:
	        	case TrailPoint.BACK:
	            	// 姿勢が悪い？
	        		if(isSlouching()){
	        			startPointOutSlouching();
	            		break;
	        		}
	        		// 悪くなければ↓へ
	        	default:
	        		// 貧乏揺すり？
	        		if(isKneeShaking()){
	        			startPointOutKneeShaking();
	        		}else{
	        			robot.stand();
	        		}
	        	}
        	}
        }

		// 貧乏揺すりしてる？
		private boolean isKneeShaking() {
			// TODO Auto-generated method stub
			return false;
		}

		// 猫背？
		private boolean isSlouching() {
			// TODO Auto-generated method stub
			return false;
		}
		
		
        // 目標値点へ
		private void toGoal() {
			// TODO Auto-generated method stub
			
		}
		
		// 指摘の終了
		private void endPointingOut() {
			isPointingOutSlouching = false;
			isPointingOutKneeShaking = false;
			goalReached = false;
			goal = shoulder;
			robot.stand();
		}
		
		// 猫背指摘の開始
		private void startPointOutSlouching() {
			isPointingOutSlouching = true;
			goalReached = false;
			goal = back;
			robot.angry();
		}
		
		// 貧乏揺すり指摘の開始
		private void startPointOutKneeShaking() {
			isPointingOutKneeShaking = true;
			goalReached = false;
			goal = leg;
			robot.angry();
		}
    };
    
}
