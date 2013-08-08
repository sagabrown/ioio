package ioio.examples.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class TrailView extends View {
    private List<TrailPoint> tpList;
    private TrailPoint lastTp;
    private int halfHeight = 100;
    private int scale = 3;
    private ScheduledExecutorService ses = null;

    Paint paint;
 
    private final Runnable task = new Runnable(){
        @Override
        public void run() {
        	//Log.d("Trail", "running...");
 
            // 画面を更新する
            postInvalidate();
        }
    };
 
    /**
     * コンストラクタ
     * @param context
     */
    public TrailView(Context context) {
        super(context);
        paint = new Paint();
        tpList = new ArrayList<TrailPoint>();
        lastTp = new TrailPoint(0,0,0);
    }
    
    public void addTp(float x, float y, float z){
    	TrailPoint tp = new TrailPoint(lastTp.x + x, lastTp.y + y, lastTp.z + z);
		synchronized(tpList){
			tpList.add(tp);
		}
    	lastTp = tp;
    }
 
    // 描画処理
    @Override
    protected void onDraw(Canvas canvas) {
        //final Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        synchronized(tpList){
	        for(TrailPoint tp : tpList){
	        	int x = (int) (scale * tp.x);
	        	int y = (int) (scale * tp.y);
	        	int z = (int) (scale * tp.z);

	            //paint.setARGB(125+y,255,255,255);
	        	int round = 5 + y/10;
	        	if(round<1){
	        		round = 1;
	        	}else if(9<round){
	        		round = 9;
	        	}
	        	canvas.drawCircle(x, halfHeight - z, round, paint);
	        }
        }
    }
 
    public void onResume(){
        // タイマーを作成する
        ses = Executors.newSingleThreadScheduledExecutor();
 
        // 100msごとにRunnableの処理を実行する
        ses.scheduleAtFixedRate(task, 0L, 100L, TimeUnit.MILLISECONDS);
    }
 
    public void onPause(){
        // タイマーを停止する
        ses.shutdown();
        ses = null;
    }
}
