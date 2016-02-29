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
	private int shoulder, back, leg;	// ��\�_�̃C���f�b�N�X
	private String state;
	
	public PointOutMode() {
		button = null;
		isAuto = false;
		ses = new ScheduledExecutorService[2];
		state = "posture: OK"+"\n"+"habits: OK";
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
        button.setTextOn("point-out");
        button.setTextOff("ignore");
        button.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// ��������I�[�g���[�h���I�t��
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
        // �^�C�}�[���쐬����
        ses[0] = Executors.newSingleThreadScheduledExecutor();
        // 200ms���Ƃ�task�����s����
    	Log.i(TAG, "PointOutStarted");
    	ses[0].scheduleAtFixedRate(task, 0L, 200L, TimeUnit.MILLISECONDS);

    	wheel.setIsAutoControlled(this);
		ears.setIsAutoControlled(this);
    	eyes.setIsAutoControlled(this);
	}

	@Override
	public void stop() {
		if(!isAuto)	return;
		isAuto = false;
		robot.stand();
		wheel.stop();
    	wheel.setIsAutoControlled(null);
		ears.setIsAutoControlled(null);
		eyes.setIsAutoControlled(null);

		// �^�C�}�[���~����
		if(ses[0] == null)	return;
		ses[0].shutdown();
    	Log.i(TAG, "PointOutStopped");
	}
	


	/** �v�����ʒ񎦂̃^�X�N **/
    private final Runnable task = new Runnable(){
    	private boolean slouching;
    	private boolean kneeShaking;
    	private boolean isPointingOutSlouching;
    	private boolean isPointingOutKneeShaking;
    	private int goal;
    	private boolean goalReached;
    	int step = 0;
    	
        @Override
        public void run() {
        	Log.d("PointOut", "running...");
        	if(!isAuto)	return;
        	
        	int tpType = robot.sensor.getNowTpType();
        	shoulder = sensor.getMaxTpIndex() - 3;
        	back = sensor.getMaxTpIndex() / 2;
        	leg = 2;
        	
        	slouching = isSlouching();
        	kneeShaking = isKneeShaking();

        	String posture = (slouching)? "�L�w":"OK";
        	String habits = (kneeShaking)? "�n�R�h����":"OK";
    		state = "posture: " + posture + "\n" +"habits: " + habits;

        	if(isPointingOutSlouching){			// �L�w�w�E���[�h
        		if(!slouching){	// �������ꂽ�Ƃ�
        			endPointingOut();
        		}else{
	        		if(goalReached){	// �ړI�n�_�ɂ���
	        			// �O��ړ�
    					switch(step){
    					case 0: goal = back - 3; break;
    					case 1: goal = back + 3; break;
    					}
    					goalReached = false;
    					step = 1-step;
    					// �ڂ̓_��
        				eyes.flick();
	        		}else{				// �ړI�n�_���B�O
	        			toGoal();
	        		}
        		}
        	}else if(isPointingOutKneeShaking){	// �n�R�h����w�E���[�h
        		if(!kneeShaking){	// �������ꂽ�Ƃ�
        			endPointingOut();
        		}else{
        			if(goalReached){	// �ړI�n�_�ɂ���
        				// �킿��킿��
        				ears.swing();
        				eyes.flick();
	        		}else{				// �ړI�n�_���B�O
	        			toGoal();
	        		}
        		}
        	}else{								// ���펞
	        	switch(tpType){
	        	case TrailPoint.SHOLDER:
	        	case TrailPoint.BACK:
	            	// �p���������H
	        		if(slouching){
	        			startPointOutSlouching();
	            		break;
	        		}
	        		// �����Ȃ���΁���
	        	default:
	        		// �n�R�h����H
	        		if(kneeShaking){
	        			startPointOutKneeShaking();
	        		}else{
	        			robot.stand();
	        			toGoal();
	        		}
	        	}
        	}
        }

		// �n�R�h���肵�Ă�H
		private boolean isKneeShaking() {
			return sensor.isKneeShaking();
		}

		// �L�w�H
		private boolean isSlouching() {
			return sensor.isSlouching();
		}
		
		
        // �ڕW�l�_��
		private void toGoal() {
			int nowTpIndex = sensor.getNowTpIndex();
			if(nowTpIndex == goal){
				wheel.stop();
				goalReached = true;
				return;
			}else if(nowTpIndex > goal){
				try {
					wheel.goBackward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else if(nowTpIndex < goal){
				try {
					wheel.goForward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		// �w�E�̏I��
		private void endPointingOut() {
			isPointingOutSlouching = false;
			isPointingOutKneeShaking = false;
			goal = shoulder;
			goalReached = false;
			robot.stand();
		}
		
		// �L�w�w�E�̊J�n
		private void startPointOutSlouching() {
			isPointingOutSlouching = true;
			goalReached = false;
			goal = back;
			robot.angry();
		}
		
		// �n�R�h����w�E�̊J�n
		private void startPointOutKneeShaking() {
			isPointingOutKneeShaking = true;
			goalReached = false;
			goal = leg;
			robot.angry();
		}
    };
    
}
