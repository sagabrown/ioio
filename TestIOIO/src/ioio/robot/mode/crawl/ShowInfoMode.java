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
import ioio.robot.region.crawl.sensor.PoseAnalizer;
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

		// �^�C�}�[���~����
		if(ses[0] == null)	return;
		ses[0].shutdown();
    	Log.i(TAG, "showInfoStopped");
	}
	


	/** �v�����ʒ񎦂̃^�X�N **/
    private final Runnable task = new Runnable(){
        @Override
        public void run() {
        	//Log.d("showInfo", "running...");
        	if(!isAuto)	return;
        	
        	float dif = sensor.getPitchDifference();
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
        	// ���茋�ʂ�ڂ̓_�łƎ��Ŏ���
        	if(Math.abs(dif) < PoseAnalizer.THRESHOLD_BACK1){
            	eyes.manageFlick(false);
        		ears.manageSwing(false);
        		ears.changeStateByRad(-dif);
        	}else{
            	eyes.manageFlick(true);
        		ears.manageSwing(true);
        	}
        }
    };

}
