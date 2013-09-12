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
    private int slitNum = 2;	// スリットの数
	private int distPerCycle = 48;	// モーター1回転で進む距離[mm]
    private float speed;  // 回/秒
    private LinearLayout layout;
    private TextView countView, speedView, timeView;
    private ScheduledExecutorService[] ses;

    
    public SpeedMater(Util util, int distPerCycle) {
		this.util = util;
		this.distPerCycle = distPerCycle;
	}


    public int openPins(IOIO ioio, int pinNum) throws ConnectionLostException{
		// 回転数計測のためのdigital input
		cycleIn = ioio.openDigitalInput(pinNum, DigitalInput.Spec.Mode.PULL_DOWN);
    	return 1;
    }

	/** 表示パネルを生成して返す **/
    public LinearLayout getLayout(MainActivity parent){
        // 親のアクティビティに動的レイアウトを作成する
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
        // タイマーを作成する
		ses = new ScheduledExecutorService[3];
		for(int i=0; i<3; i++){
			ses[i] = Executors.newSingleThreadScheduledExecutor();
		}
        // 回転数カウントを開始
    	ses[0].schedule(countCycleTask, 0L, TimeUnit.MILLISECONDS);
        // 100msごとに画面更新taskを実行する
        ses[1].scheduleAtFixedRate(setViewTask, 0L, 100L, TimeUnit.MILLISECONDS);
        // 1000msごとに速度計算taskを実行する
        ses[2].scheduleAtFixedRate(calcSpeedTask, 0L, 1000L, TimeUnit.MILLISECONDS);
    }

    public void disactivate(){
		isActive = false;
		// タイマーを停止する
		for(ScheduledExecutorService s : ses){
			if(s != null){
				s.shutdown();
				s = null;
			}
		}
    }
    
    public void disconnected(){
		disactivate();
		cycleIn.close();
		cycleIn = null;
    }
	
	/** 回転数カウントのタスク **/
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
    
	/** スピード計算のタスク **/
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
    
	/** 回転数表示のタスク **/
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
