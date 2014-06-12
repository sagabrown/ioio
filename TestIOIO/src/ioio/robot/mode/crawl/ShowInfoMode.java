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

public class ShowInfoMode extends AutoMode {
	private final static String TAG = "showInfoMode";
	private Wheel wheel;
	private Ears ears;
	private Eyes eyes;
	private SensorTester sensor;
	
	public ShowInfoMode() {
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
        // �I�[�g�؂�ւ��̃{�^��
        button = new ToggleButton(context);
        button.setTextOn("show-info");
        button.setTextOff("hide-info");
        button.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// ��������I�[�g���[�h���I�t��
					if(wheel.getOwner() != null && wheel.getOwner() != ShowInfoMode.this){
						wheel.getOwner().buttonOff();
					}
					if(ears.getOwner() != null && ears.getOwner() != ShowInfoMode.this){
						ears.getOwner().buttonOff();
					}
					if(eyes.getOwner() != null && eyes.getOwner() != ShowInfoMode.this){
						eyes.getOwner().buttonOff();
					}
					start();
				}else{
					stop();
				}
			}
        });
        button.setText("hide-info");
	}

	@Override
	public void start() {
		if(isAuto)	return;
		isAuto = true;
        // �^�C�}�[���쐬����
        ses[0] = Executors.newSingleThreadScheduledExecutor();
        // 100ms���Ƃ�task�����s����
    	Log.i(TAG, "showInfoStarted");
    	ses[0].scheduleAtFixedRate(task, 0L, 100L, TimeUnit.MILLISECONDS);
		
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

		// �^�C�}�[���~����
		if(ses[0] == null)	return;
		ses[0].shutdown();
    	Log.i(TAG, "showInfoStopped");
	}
	


	/** �v�����ʒ񎦂̃^�X�N **/
    private final Runnable task = new Runnable(){
        @Override
        public void run() {
        	Log.d("showInfo", "running...");
        	if(!isAuto)	return;
        	
        	float dif = sensor.getPitchDifference();
        	// �p�x�̕ω������Ŏ���
        	ears.changeStateByRad(dif);
        	// �ʒu�̈Ⴂ��ڂŎ���
        	switch(sensor.getNowTpType()){
        	case TrailPoint.NO_TYPE:
        		eyes.setColor(TrailView.NO_TYPE_COLOR);
        		break;
        	case TrailPoint.BACK:
        		eyes.setColor(TrailView.BACK_COLOR);
        		break;
        	case TrailPoint.SHOLDER:
        		eyes.setColor(TrailView.SHOLDER_COLOR);
        		break;
        	case TrailPoint.ARM:
        		eyes.setColor(TrailView.ARM_COLOR);
        		break;
        	}
        	// ���茋�ʂ�ڂ̓_�łŎ���
        	if(Math.abs(dif) > 45*Math.PI && ses != null){
        		if(ses[1] == null){
        	        // �^�C�}�[���쐬����
        	        ses[1] = Executors.newSingleThreadScheduledExecutor();
        		}
    	        // 500ms���Ƃ�task�����s����
    	        Log.i(TAG, "eyesFlickStarted");
	        ses[1].scheduleAtFixedRate(eyesTask, 0L, 500L, TimeUnit.MILLISECONDS);	
        	}else{
        		if(!ses[1].isShutdown())	ses[0].shutdown();
        	}
        }

        
        @Override
        protected void finalize() throws Throwable {
			if(ses != null){
				// �^�C�}�[���~����
				ses[1].shutdown();
			}
        	super.finalize();
        }
    };
    

    /** �ڂ̓_�Ń^�X�N **/
    private final Runnable eyesTask = new Runnable(){
    	private int[] taskLoopFlickFast = {1,0,1,0,1,0,1,0,1,0,1,0};
    	private int[] taskLoopFlickSlow = {1,1,0,0,1,1,0,0,1,1,0,0};
    	private int taskCnt = 0;
        @Override
        public void run() {
        	if(!isAuto)	return;
        	Log.d("eyeFlick", "running...");
			switch(taskLoopFlickFast[taskCnt]){
			case 0:		eyes.setLuminous(0f);		break;
			case 1:		eyes.setLuminous(1f);	break;
			}
        	if(taskCnt==taskLoopFlickFast.length-1)	taskCnt = 0;
        	else									taskCnt++;
        }
    };
}
