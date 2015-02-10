package ioio.robot.region.crawl.sensor;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;

public class AccelView extends View {
    private ScheduledExecutorService[] ses = null;
    private static final int halfHeight = 75;
    
    private ArrayList<Float> accelNorm;
    private static final int ACCEL_NORM_SIZE = 70;
    private static final int HEIGHT_RATE = 180;
    private static final int WIDTH_RATE = 3;
    private static final int FFT_SIZE = 64;
    private static final int FFT_POINT = (int)Math.sqrt(FFT_SIZE);
    private static final int HEIGHT_RATE_FFT = 4 * halfHeight;
    private static final int WIDTH_RATE_FFT = 6;
    
    DoubleFFT_1D fft;
    double[] fftData;
    float[] spect;
    int maxPoint;
    float peak, peakVal, range;
    
    int samplingFreq;
    float df;

	private Paint paint;
	
    private final Runnable task = new Runnable(){
        @Override
        public void run() {
        	//Log.d("Graph", "running...");
            // 画面を更新する
            postInvalidate();
        }
    };
    
    private final Runnable FFTTask = new Runnable(){
    	public void run(){
    		if(accelNorm.size() > FFT_SIZE){
    			float maxAccel=0, minAccel=Float.MAX_VALUE;
    	    	for(int i=0; i<FFT_SIZE; i++){
    	    		fftData[i] = accelNorm.get(i);
    	    		if(maxAccel < fftData[i])	maxAccel = (float) fftData[i];
    	    		if(minAccel > fftData[i])	minAccel = (float) fftData[i];
    	    	}
    			range = maxAccel - minAccel;
    	    	fft.realForward(fftData);
    	    	
    			float max = 0;
    			float sum = 0;
    			maxPoint = 0;
    	    	for(int i=1; i<fftData.length/2; i++){
    	    		spect[i] = (float) Math.sqrt( fftData[2*i]*fftData[2*i] + fftData[2*i+1]*fftData[2*i+1] ) / FFT_POINT;
    	    		sum += spect[i];
    				if(max < spect[i]){
    					max = spect[i];
    					maxPoint = i;
    				}
    	    	}
    	    	
    	    	// 振幅を占める割合(0~1)に変換
    	    	for(int i=0; i<spect.length; i++){
    	    		spect[i] /= sum;
    	    	}
    	    	
    	    	peak = maxPoint*df;
    	    	peakVal = spect[maxPoint];
    		}
    	}
    };

	public AccelView(Context context, long samplingFreq) {
		super(context);
		this.samplingFreq = (int)samplingFreq;
		df = (float)samplingFreq / FFT_SIZE;
		accelNorm = new ArrayList<Float>();
		fft = new DoubleFFT_1D(FFT_SIZE);
    	fftData = new double[FFT_SIZE];
    	spect = new float[FFT_SIZE/2];
	}
	
	protected void onDraw(Canvas canvas){
		int w = getWidth();
		int h = getHeight();
		
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		canvas.drawRect(0, 0, w, h, paint);
		
		paint.setColor(Color.RED);
		float prevNorm = 0f;
		if(accelNorm.size() > 2)	prevNorm = accelNorm.get(accelNorm.size()-1);
		for(int i=accelNorm.size()-2, cnt=0; i>=0; i--, cnt++){
			float a1 = prevNorm;
			float a2 = accelNorm.get(i);
			canvas.drawLine(w-cnt*WIDTH_RATE, h-a1/HEIGHT_RATE, w-(cnt+1)*WIDTH_RATE, h-a2/HEIGHT_RATE, paint);
			prevNorm = a2;
		}

    	// フーリエ変換の結果表示
		if(accelNorm.size() > FFT_SIZE){
			paint.setColor(Color.BLUE);
			for(int i=0; i<spect.length-1; i++){
				float f1 = spect[i];
				float f2 = spect[i+1];
				canvas.drawLine(i*WIDTH_RATE_FFT, h-f1*HEIGHT_RATE_FFT, (i+1)*WIDTH_RATE_FFT, h-f2*HEIGHT_RATE_FFT, paint);
				//Log.i("FFT", ""+fftData[i]);
			}

			paint.setColor(Color.BLACK);
			canvas.drawText("PEAK: " + peak + "Hz", 10, 10, paint);
			
			// 判定
		}
		//Log.i("debug", "onDraw End");
	}

	@Override
    protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Viewの描画サイズを指定する
        setMeasuredDimension(width,2*halfHeight);
    }
    
    public void addAccelNorm(float accel){
    	//Log.i("TestDataAdd", "add "+accel);
    	accelNorm.add(accel);
    	if(accelNorm.size() == ACCEL_NORM_SIZE){
    		accelNorm.remove(0);
    	}
    }
    

    
    public void onResume(){
        // タイマーを作成する
		ses = new ScheduledExecutorService[3];
		for(int i=0; i<ses.length; i++){
			ses[i] = Executors.newSingleThreadScheduledExecutor();
		}
        // 100msごとに描画taskを実行する
        ses[0].scheduleAtFixedRate(task, 0L, 100L, TimeUnit.MILLISECONDS);
        // 200msごとにFFTtaskを実行する
        ses[1].scheduleAtFixedRate(FFTTask, 0L, 200L, TimeUnit.MILLISECONDS);
        // 5msごとにtestAddTaskを実行する
        //ses[2].scheduleAtFixedRate(testAddTask, 0L, 1000/samplingFreq, TimeUnit.MILLISECONDS);
    }
 
    public void onPause(){
		// タイマーを停止する
		if(ses == null)	return;
		for(ScheduledExecutorService s : ses){
			if(s != null){
				s.shutdown();
				s = null;
			}
		}
    }
    
    
	
	public float getPeak() {
		return peak;
	}

	public float getPeakVal() {
		return peakVal;
	}

	public float getRange() {
		return range;
	}



	private final Runnable testAddTask = new Runnable(){
		float[] test = {10000, 15000, 16000, 17000, 18000, 20000, 20000, 18000, 16000, 15000, 10000};
		float[] test2 = {10000,10000, 15000, 16000, 16000, 17000, 18000, 20000, 20000, 20000, 20000, 18000, 16000, 16000, 15000, 10000,10000,10000};
		float[] test3 = {0, 100};
		float[] test4 = {10000, 16000, 18000, 20000, 18000, 15000, 10000};
		final static int CASE_NUM = 4;
		
		Random rand = new Random();

		final static int COUNT_MAX = 300;
		final static int RATE = 1;
		
		int count = 0;
		int flg = 0;
		public void run(){
        	//Log.d("TestDataAdd", "running..."+accelNorm.size());
			if(flg==0){
				addAccelNorm(test2[(count/RATE)%test2.length] + rand.nextInt(5000));
				if(count==COUNT_MAX){
					count = 0;
					flg = rand.nextInt(CASE_NUM);
				}
				else	count++;
			}else if(flg==1){
				addAccelNorm(test[(count/RATE)%test.length] + rand.nextInt(5000));
				if(count==COUNT_MAX){
					count = 0;
					flg = rand.nextInt(CASE_NUM);
				}
				else	count++;
			}else if(flg==2){
				addAccelNorm(test3[(count/RATE)%test3.length] + rand.nextInt(5000));
				if(count==COUNT_MAX){
					count = 0;
					flg = rand.nextInt(CASE_NUM);
				}
				else	count++;
			}else if(flg==3){
				addAccelNorm(test4[(count/RATE)%test4.length] + rand.nextInt(5000));
				if(count==COUNT_MAX){
					count = 0;
					flg = rand.nextInt(CASE_NUM);
				}
				else	count++;
			}else{
				Log.i("AccelView", "!?!?"+flg);
			}
		}
	};

}
