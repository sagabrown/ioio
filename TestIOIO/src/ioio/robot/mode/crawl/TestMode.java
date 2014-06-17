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
import ioio.robot.region.crawl.Wheel;
import ioio.robot.robot.CrawlRobot;
import ioio.robot.util.Util;

public class TestMode extends AutoMode {
	private final static String TAG = "testMode";
	private Wheel wheel;
	private Ears ears;
	
	public TestMode() {
		button = null;
		isAuto = false;
		ses = new ScheduledExecutorService[1];
	}
	
	public void setParams(Util util, CrawlRobot robot){
		this.util = util;
		this.robot = robot;
		this.wheel = robot.wheel;
		this.ears = robot.ears;
	}
	
	protected void generateButton(Context context){
        // オート切り替えのボタン
        button = new ToggleButton(context);
        button.setTextOn("test");
        button.setTextOff("manual");
        button.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// 競合するオートモードをオフに
					if(wheel.getOwner() != null && wheel.getOwner() != TestMode.this){
						wheel.getOwner().buttonOff();
					}
					if(ears.getOwner() != null && ears.getOwner() != TestMode.this){
						ears.getOwner().buttonOff();
					}
					start();
				}else{
					stop();
				}
			}
        });
        button.setText("manual");
	}

	@Override
	public void start() {
		if(isAuto)	return;
		isAuto = true;
        // タイマーを作成する
        ses[0] = Executors.newSingleThreadScheduledExecutor();
        // 500msごとにtaskを実行する
    	Log.i(TAG, "testControllStarted");
    	ses[0].scheduleAtFixedRate(task, 0L, 500L, TimeUnit.MILLISECONDS);
		
    	wheel.setIsAutoControlled(this);
		ears.setIsAutoControlled(this);
	}

	@Override
	public void stop() {
		if(!isAuto)	return;
		isAuto = false;
		wheel.stop();
		wheel.setIsAutoControlled(null);
		ears.setIsAutoControlled(null);

		// タイマーを停止する
		if(ses[0] == null)	return;
		ses[0].shutdown();
    	Log.i(TAG, "testControllStopped");
	}
	


	/** 自動制御のタスク **/
    private final Runnable task = new Runnable(){
    	private int[] taskLoopDrive = {1,1,1,1-1,-1,-1,-1,0,0,0,0};
    	private int[] taskLoopEars = {0,0,0,0,0,0,1,-1,1,-1,0,0};
    	private int taskCnt = 0;
        @Override
        public void run() {
        	if(!isAuto)	return;
        	Log.d("autoControll", "running...");
			try {
				switch(taskLoopEars[taskCnt]){
				case 1:		ears.forwardSlowly();		break;
				case -1:	ears.forwardSlowly();	break;
				default:	ears.reset();		break;
				}
				switch(taskLoopDrive[taskCnt]){
				case 1:		wheel.goForward();		break;
				case -1:	wheel.goBackward();	break;
				default:	wheel.stop();				break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	
        	if(taskCnt==taskLoopDrive.length-1)	taskCnt = 0;
        	else								taskCnt++;
        }
    };
}
