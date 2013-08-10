package ioio.examples.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

public class TrailView extends GLSurfaceView {
	private int halfW = 2, halfH = 1;
    private List<TrailPoint> tpList;
    private TrailPoint lastTp;
    private float oldX1, oldY1, oldZ1;
    private float lookX, lookY, lookZ;
    private float lookUD, lookLR;
    private int halfHeight = 200;
    private int scale = 1;
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
        lastTp = new TrailPoint(0,0,0,0,0,0);
        oldX1 = oldY1 = oldZ1 = 0;
        lookX = lookY = lookZ = 30f;
        
        setRenderer(new Renderer(){
			@Override
			public void onDrawFrame(GL10 gl) {
				//Log.d("xyz", lookX+","+lookY+","+lookZ);
				gl.glClearColor(0.5f, 0.5f, 0.5f, 1f);
				gl.glClear(GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_COLOR_BUFFER_BIT );
				gl.glMatrixMode(GL10.GL_MODELVIEW);
				gl.glLoadIdentity();
				GLU.gluLookAt(gl, lookX, lookY, lookZ, 0f, 0f, 0f, 0f, 0f, 1f);
				gl.glLineWidth(3);
				
				// 軸
				// ライティングoff
				gl.glDisable(GL10.GL_LIGHTING);
				float[] vertices1 = {-1000,0,0,1000,0,0};
				gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
				drawLine(gl, vertices1);
				float[] vertices2 = {0,-1000,0,0,1000,0};
				gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
				drawLine(gl, vertices2);
				float[] vertices3 = {0,0,-1000,0,0,1000};
				gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
				drawLine(gl, vertices3);
				
				// 軌跡
				// ライティングon
				gl.glEnable(GL10.GL_LIGHTING);
				gl.glEnable(GL10.GL_LIGHT0);
				// 色
				gl.glColor4f(1.0f, 1.0f, 0.0f, 0.8f);
				synchronized(tpList){
					int oldXr, oldYr, oldZr, oldXl, oldYl, oldZl;
					oldXr=oldYr=oldZr=oldXl=oldYl=oldZl= 0;
			        for(TrailPoint tp : tpList){
			        	int xr = (int) (scale * tp.xr);
			        	int yr = (int) (scale * tp.yr);
			        	int zr = (int) (scale * tp.zr);
			        	int xl = (int) (scale * tp.xl);
			        	int yl = (int) (scale * tp.yl);
			        	int zl = (int) (scale * tp.zl);
						float[] vertices = {
								oldXr, oldYr, oldZr,
								oldXl, oldYl, oldZl,
								xr, yr, zr,
								xl, yl, zl
						};
						drawRect(gl, vertices);
						oldXr = xr;
						oldYr = yr;
						oldZr = zr;
						oldXl = xl;
						oldYl = yl;
						oldZl = zl;
			        }
				}
			}
			
			void drawLine(GL10 gl, float[] vertices){
				ByteBuffer byteBuffer= ByteBuffer.allocateDirect(4 * 3 * 2);
				byteBuffer.order(ByteOrder.nativeOrder());
				FloatBuffer line = byteBuffer.asFloatBuffer();
				line.put(vertices);
				line.position(0);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, line);
				gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
				gl.glDrawArrays(GL10.GL_LINES, 0, 2 );
			}
			void drawRect(GL10 gl, float[] v){
				float[] vertices = {v[0], v[1], v[2],
									v[3], v[4], v[5],
									v[6], v[7], v[8],
									v[3], v[4], v[5],
									v[6], v[7], v[8],
									v[9], v[10], v[11]};
				ByteBuffer byteBuffer= ByteBuffer.allocateDirect(4 * 3 * 6);
				byteBuffer.order(ByteOrder.nativeOrder());
				FloatBuffer rect = byteBuffer.asFloatBuffer();
				rect.put(vertices);
				rect.position(0);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, rect);
				gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
				gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 6 );
			}
			
			@Override
			public void onSurfaceChanged(GL10 gl, int width, int height) {
				gl.glViewport(0, 0, width, height);
				// 視体積の設定
				float ratio = (float)width/height;
				gl.glMatrixMode(GL10.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glFrustumf(-ratio*20, ratio*20, -20, 20, 10, 200);
			}
			@Override
			public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
				
			}}
        );
        
    }
    
    // 視点の設定
    public void setLookX(float x){
		lookX = x;
    }
    public void setLookY(float y){
		lookY = y;
    }
    public void setLookZ(float z){
		lookZ = z;
    }
    public void setLookUD(float ud){
		lookUD = ud;
    }
    public void setLookLR(float lr){
		lookLR = lr;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Viewの描画サイズを指定する
        setMeasuredDimension(width,2*halfHeight);
    }
    
    
    public void addTp(float x1, float y1, float z1, float x2, float y2, float z2){
    	float x = lastTp.x + halfH * (float)0.5*(oldX1+x1);
    	float y = lastTp.y + halfH * (float)0.5*(oldY1+y1);
    	float z = lastTp.z + halfH * (float)0.5*(oldZ1+z1);
    	float xr = x + halfW * x2;
    	float yr = y + halfW * y2;
    	float zr = z + halfW * z2;
    	float xl = x - halfW * x2;
    	float yl = y - halfW * y2;
    	float zl = z - halfW * z2;
    	TrailPoint tp = new TrailPoint(xr,yr,zr,xl,yl,zl);
		synchronized(tpList){
			tpList.add(tp);
		}
		//Log.d("TP", xr+","+yr+","+zr+","+xl+","+yl+","+zl);
    	lastTp = tp;
    	oldX1 = x1;
    	oldY1 = y1;
    	oldZ1 = z1;
    }
 
    // 描画処理
    /*
    @Override
    protected void onDraw(Canvas canvas) {
        //final Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        synchronized(tpList){
	        for(TrailPoint tp : tpList){
	        	int xr = (int) (scale * tp.xr);
	        	int yr = (int) (scale * tp.yr);
	        	int zr = (int) (scale * tp.zr);
	        	int xl = (int) (scale * tp.xl);
	        	int yl = (int) (scale * tp.yl);
	        	int zl = (int) (scale * tp.zl);
	        	canvas.drawCircle(xr, halfHeight - zr, 2, paint);
	        	canvas.drawCircle(xl, halfHeight - zl, 2, paint);
	        }
        }
    }
    */
 
    public void onResume(){
        // タイマーを作成する
        ses = Executors.newSingleThreadScheduledExecutor();
 
        // 100msごとにRunnableの処理を実行する
        ses.scheduleAtFixedRate(task, 0L, 1000L, TimeUnit.MILLISECONDS);
    }
 
    public void onPause(){
        // タイマーを停止する
        ses.shutdown();
        ses = null;
    }
}
