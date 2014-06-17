package ioio.robot.mode.crawl;

import java.util.Random;
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
import ioio.robot.robot.CrawlRobot;
import ioio.robot.util.Util;

public class AutoEmoMode extends AutoMode {
	private final static String TAG = "autoEmoMode";
	private Ears ears;
	private Eyes eyes;
	
	public AutoEmoMode() {
		button = null;
		isAuto = false;
		ses = new ScheduledExecutorService[1];
	}
	
	public void setParams(Util util, CrawlRobot robot){
		this.util = util;
		this.robot = robot;
		this.ears = robot.ears;
		this.eyes = robot.eyes;
	}
	
	protected void generateButton(Context context){
        // オート切り替えのボタン
        button = new ToggleButton(context);
        button.setTextOn("rand-emo");
        button.setTextOff("manu-emo");
        button.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// 競合するオートモードをオフに
					if(ears.getOwner() != null && ears.getOwner() != AutoEmoMode.this){
						ears.getOwner().buttonOff();
					}
					if(eyes.getOwner() != null && eyes.getOwner() != AutoEmoMode.this){
						eyes.getOwner().buttonOff();
					}
					start();
				}else{
					stop();
				}
			}
        });
        button.setText("manu-emo");
	}

	@Override
	public void start() {
		if(isAuto)	return;
		isAuto = true;
        // タイマーを作成する
        ses[0] = Executors.newSingleThreadScheduledExecutor();
        // 2000msごとにtaskを実行する
    	Log.i(TAG, "randomEmotionStarted");
    	ses[0].scheduleAtFixedRate(task, 0L, 2000L, TimeUnit.MILLISECONDS);
		
		ears.setIsAutoControlled(this);
    	eyes.setIsAutoControlled(this);
	}

	@Override
	public void stop() {
		if(!isAuto)	return;
		isAuto = false;
		robot.stand();
		ears.setIsAutoControlled(null);
		eyes.setIsAutoControlled(null);

		// タイマーを停止する
		if(ses[0] == null)	return;
		ses[0].shutdown();
    	Log.i(TAG, "randomEmotionStopped");
	}
	


	/** ランダム感情表現のタスク **/
    private final Runnable task = new Runnable(){
    	Random rand;
    	boolean inited = false;
        @Override
        public void run() {
        	if(!inited){
            	Log.d("randomEmotion", "init");
            	rand = new Random();
            	rand.setSeed(System.currentTimeMillis());
        		inited = true;
        	}
        	Log.d("randomEmotion", "running...");
        	if(!isAuto)	return;
        	try{
	        	switch(rand.nextInt(4)){
	        	case 0:	robot.stand();	break;
	        	case 1:	robot.happy();	break;
	        	case 2:	robot.angry();	break;
	        	case 3:	robot.sad();	break;
	        	}
        	}catch(InterruptedException e){
        		e.printStackTrace();
        	}
        }
    };
}
