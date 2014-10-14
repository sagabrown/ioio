package ioio.robot.region.crawl;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ioio.robot.part.motor.SG90;
import ioio.robot.region.Region;
import ioio.robot.util.Util;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.LinearLayout;

public class Ears extends Region {
	private final static String TAG = "Ears";

	private final static float EAR_FORWARD = 0.05f;
	private final static float EAR_INIT = 0.4f;
	private final static float EAR_BACKWARD = 0.8f;
	private final static float EAR_DD = 0.02f;
	private final static int EAR_SLEEP = 20;
	
	private final static double[] defaultMotorInitState = {0.0};
	private final static int motorNum = defaultMotorInitState.length;
	
	private SG90[] motor;
	private LinearLayout layout;
	
	boolean alreadySwinging = false;

	/** コンストラクタ **/
	public Ears(Util util) {
		this(util, defaultMotorInitState);
	}

	/** 初期化つきコンストラクタ **/
	public Ears(Util util, double[] motorInitState) {
		this.util = util;
		motor = new SG90[motorNum];
		motor[0] = new SG90(util, "Ears", motorInitState[0]);
		part = motor;
	}
	
	/** レイアウトパネルを返す **/
	@Override
	public LinearLayout getLayout(Context context) {
		layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 1, 5, 1);
        // - モーターごとの操作パネルを登録
		for(int i=0; i<motor.length; i++)	layout.addView(motor[i].getOperationLayout(context));
		return layout;
	}
	
	
	/** 耳を任意の角度に **/
	public void changeStateByRad(float rad) {
		motor[0].changeStateByRad(rad);
	}
	/** 耳を立てる **/
	public void forward() {
		motor[0].changeState(EAR_FORWARD);
	}
	/** 耳を伏せる **/
	public void backward() {
		motor[0].changeState(EAR_BACKWARD);
	}
	/** 耳を初期位置に **/
	public void reset() {
		motor[0].changeState(EAR_INIT);
	}
	/** 1度だけふるわせる **/
	public void swing() {
		if( intrruptTask() ){
			ses = Executors.newSingleThreadScheduledExecutor();
			ses.schedule(swingEarsTask, 0, TimeUnit.MILLISECONDS);
		}
	}
	/** ゆっくり耳を立てる **/
	public void forwardSlowly(){
		if( intrruptTask() ){
			ses = Executors.newSingleThreadScheduledExecutor();
			ses.schedule(earsForwardSlowlyTask, 0, TimeUnit.MILLISECONDS);
		}
	}
	/** ゆっくり耳を伏せる **/
	public void backwardSlowly(){
		if( intrruptTask() ){
			ses = Executors.newSingleThreadScheduledExecutor();
			ses.schedule(earsBackwardSlowlyTask, 0, TimeUnit.MILLISECONDS);
		}
	}
	
	
	/** ふるわせのマネジメント **/
	public void manageSwing(boolean isSwing){
		if(ses != null){
        	if(isSwing){
        		if(!alreadySwinging){
					if( intrruptTask() ){
						ses = Executors.newSingleThreadScheduledExecutor();
		    	        Log.i(TAG, "earsSwingStarted");
		    	        ses.scheduleAtFixedRate(swingEarsTask2, 0L, 200L, TimeUnit.MILLISECONDS);
					}
					alreadySwinging = true;
        	}
        	}else{
        		if(alreadySwinging){
					if( intrruptTask() ){
		    	        Log.i(TAG, "earsSwingEnd");
        			}
					alreadySwinging = false;
	        		reset();
        		}
        	}
		}
	}
	
	
    /** ゆっくり耳を立てるtask **/
    private final Runnable earsForwardSlowlyTask = new Runnable(){
        @Override
        public void run() {
    		try {
    			float state = (float)motor[0].getState();
    			for(float s=state; s>EAR_FORWARD; s-=EAR_DD){
    				motor[0].changeState(s);
    				Thread.sleep(EAR_SLEEP);
    			}
			} catch (InterruptedException e) {e.printStackTrace();}
        }
    };
    /** ゆっくり耳を伏せるtask **/
    private final Runnable earsBackwardSlowlyTask = new Runnable(){
        @Override
        public void run() {
    		try {
    			float state = (float)motor[0].getState();
    			for(float s=state; s<EAR_BACKWARD; s+=EAR_DD){
    				motor[0].changeState(s);
    				Thread.sleep(EAR_SLEEP);
    			}
			} catch (InterruptedException e) {e.printStackTrace();}
        }
    };
    /** 耳をふるわせるtask **/
    private final Runnable swingEarsTask = new Runnable(){
        @Override
        public void run() {
    		try {
	    		forward();
				Thread.sleep(100);
	    		backward();
	    		Thread.sleep(100);
	    		forward();
	    		Thread.sleep(100);
	    		backward();
	    		Thread.sleep(100);
	    		reset();
			} catch (InterruptedException e) {e.printStackTrace();}
        }
    };
    /** 耳を繰り返しふるわせるtask **/
    private final Runnable swingEarsTask2 = new Runnable(){
    	private int[] taskLoopSwingFast = {1,0,1,0,1,0,1,0,1,0,1,0};
    	private int[] taskLoopSwingSlow = {1,1,0,0,1,1,0,0,1,1,0,0};
    	private int taskCnt = 0;
        @Override
        public void run() {
        	if(!isAuto)	return;
        	Log.d("SwingEars", "running...");
			switch(taskLoopSwingFast[taskCnt]){
			case 0:		forward();		break;
			case 1:		backward();		break;
			}
        	if(taskCnt==taskLoopSwingFast.length-1)	taskCnt = 0;
        	else									taskCnt++;
        }
    };


}
