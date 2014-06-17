package ioio.robot.region.crawl.sensor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.MainActivity;
import ioio.robot.robot.Robot;
import ioio.robot.util.Util;

public class SpeedMater {
	private Util util;
	private boolean isActive;
	private long startTime;
	private DigitalInput cycleIn;
    private int cycleCount, tempCount;
    private int slitNum = 2;	// �X���b�g�̐�
	private int distPerCycle = 48;	// ���[�^�[1��]�Ői�ދ���[mm]
	private boolean foward = true;
	private float speed;  // ��/�b
    private LinearLayout layout;
    private TextView countView, speedView, timeView;
    private ScheduledExecutorService[] ses;
    private Robot parent;

    
    public SpeedMater(Util util, int distPerCycle, Robot parent) {
		this.util = util;
		this.distPerCycle = distPerCycle;
		this.parent = parent;
	}


    public int openPins(IOIO ioio, int pinNum) throws ConnectionLostException{
		// ��]���v���̂��߂�digital input
		cycleIn = ioio.openDigitalInput(pinNum, DigitalInput.Spec.Mode.PULL_DOWN);
    	return 1;
    }

	/** �\���p�l���𐶐����ĕԂ� **/
    public LinearLayout getLayout(Context parent2){
        // �e�̃A�N�e�B�r�e�B�ɓ��I���C�A�E�g���쐬����
        layout = new LinearLayout(parent2);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setWeightSum(3);

		LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.weight = 1;
    	countView = new TextView(parent2);
    	util.setText(countView, "count: ");
    	layout.addView(countView, lp);
    	speedView = new TextView(parent2);
    	util.setText(speedView, "speed: ");
    	layout.addView(speedView,lp);
    	timeView = new TextView(parent2);
    	util.setText(timeView, "time: ");
    	layout.addView(timeView,lp);
    	
    	return layout;
    }
    
    public void activate(){
		isActive = true;
		startTime = System.currentTimeMillis();
        // �^�C�}�[���쐬����
		ses = new ScheduledExecutorService[3];
		for(int i=0; i<3; i++){
			ses[i] = Executors.newSingleThreadScheduledExecutor();
		}
        // ��]���J�E���g���J�n
    	ses[0].schedule(countCycleTask, 0L, TimeUnit.MILLISECONDS);
        // 100ms���Ƃɉ�ʍX�Vtask�����s����
        ses[1].scheduleAtFixedRate(setViewTask, 0L, 100L, TimeUnit.MILLISECONDS);
        // 1000ms���Ƃɑ��x�v�Ztask�����s����
        ses[2].scheduleAtFixedRate(calcSpeedTask, 0L, 1000L, TimeUnit.MILLISECONDS);
    }

    public void disactivate(){
		isActive = false;
		// �^�C�}�[���~����
		if(ses == null)	return;
		for(ScheduledExecutorService s : ses){
			if(s != null){
				s.shutdown();
				s = null;
			}
		}
    }
    
    public void disconnected(){
		disactivate();
		if(cycleIn != null)	cycleIn.close();
		cycleIn = null;
    }
    
    public void setFoward(boolean foward) {
		this.foward = foward;
	}

	
	/** ��]���J�E���g�̃^�X�N **/
    private final Runnable countCycleTask = new Runnable(){
        @Override
        public void run() {
        	while(true){
        		if(cycleIn == null)	break;
        		try {
					cycleIn.waitForValue(false);
					cycleIn.waitForValue(true);
					if(foward){
						cycleCount++;
						tempCount++;
						parent.incCount();
					}else{
						cycleCount--;
						tempCount--;
						parent.decCount();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				} catch (ConnectionLostException e) {
					if(isActive)	e.printStackTrace();
					break;
				}
        	}
        }
    };
    
	/** �X�s�[�h�v�Z�̃^�X�N **/
    private final Runnable calcSpeedTask = new Runnable(){
    	private long time, lastTime;
        @Override
        public void run() {
        	time = System.currentTimeMillis();
			speed = 1000f * (float)tempCount / (float)slitNum / (float)(time-lastTime);
			tempCount = 0;
        	lastTime = time;
        	parent.setSpeed(speed);
        }
    };
    
	/** ��]���\���̃^�X�N **/
    private final Runnable setViewTask = new Runnable(){
    	private long time;
        @Override
        public void run() {
        	time = System.currentTimeMillis();
			
        	util.setText(countView, "count: "+(float)cycleCount);
        	util.setText(speedView, "speed: "+String.format("%.2f", speed)+" times/s = "+String.format("%.2f", (speed*distPerCycle))+" mm/s");
        	util.setText(timeView, "time: "+String.format("%.2f", ((time-startTime)*0.001))+" s");
        }
    };
}
