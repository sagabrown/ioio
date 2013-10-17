package ioio.robot.sensor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.util.Util;

public class SpeedMater {
	private Util util;
	private boolean isActive;
	private long startTime;
	private DigitalInput cycleIn;
    private int cycleCount, tempCount;
    private int slitNum = 2;	// �X���b�g�̐�
	private int distPerCycle = 48;	// ���[�^�[1��]�Ői�ދ���[mm]
    private float speed;  // ��/�b
    private LinearLayout layout;
    private TextView countView, speedView, timeView;
    private ScheduledExecutorService[] ses;

    
    public SpeedMater(Util util, int distPerCycle) {
		this.util = util;
		this.distPerCycle = distPerCycle;
	}


    public int openPins(IOIO ioio, int pinNum) throws ConnectionLostException{
		// ��]���v���̂��߂�digital input
		cycleIn = ioio.openDigitalInput(pinNum, DigitalInput.Spec.Mode.PULL_DOWN);
    	return 1;
    }

	/** �\���p�l���𐶐����ĕԂ� **/
    public LinearLayout getLayout(MainActivity parent){
        // �e�̃A�N�e�B�r�e�B�ɓ��I���C�A�E�g���쐬����
        layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setWeightSum(3);

		LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.weight = 1;
    	countView = new TextView(parent);
    	util.setText(countView, "count: ");
    	layout.addView(countView, lp);
    	speedView = new TextView(parent);
    	util.setText(speedView, "speed: ");
    	layout.addView(speedView,lp);
    	timeView = new TextView(parent);
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
	
	/** ��]���J�E���g�̃^�X�N **/
    private final Runnable countCycleTask = new Runnable(){
        @Override
        public void run() {
        	while(true){
        		if(cycleIn == null)	break;
        		try {
					cycleIn.waitForValue(false);
					cycleIn.waitForValue(true);
					cycleCount++;
					tempCount++;
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				} catch (ConnectionLostException e) {
					e.printStackTrace();
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
        }
    };
    
	/** ��]���\���̃^�X�N **/
    private final Runnable setViewTask = new Runnable(){
    	private long time;
        @Override
        public void run() {
        	time = System.currentTimeMillis();
			
        	util.setText(countView, "count: "+cycleCount);
        	util.setText(speedView, "speed: "+speed+" times/s = "+speed*distPerCycle+" mm/s");
        	util.setText(timeView, "time: "+(time-startTime)*0.001+" s");
        }
    };
}
