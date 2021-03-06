package ioio.robot.region.crawl.sensor;

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
import android.graphics.Paint;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class TrailView extends GLSurfaceView {
	private static final String TAG = "TrailView";
	
	SeekBar seekBarX, seekBarY, seekBarZ;
	SeekBar seekBarLaX, seekBarLaY, seekBarLaZ;

    private static final int halfHeight = 200;
	private static final int halfW = 2;
    private static final int scale = 1;
    
	private float speed = 2;
    private ArrayList<TrailPoint> tpList;
    private TrailPoint lastTp;
	private float azimuth, pitch, roll;
	private float xf, yf, zf, xr, yr, zr;
    private float oldX1, oldY1, oldZ1;
    private float oldX2, oldY2, oldZ2;
    private float lookX, lookY, lookZ;
    private float laX, laY, laZ;
    
    private boolean fix;
    private ScheduledExecutorService ses = null;

	final static float[] vertices1 = {
			-1000,-1,-1,
			-1000,1,-1,
			-1000,-1,1,
			-1000,1,1,
			1000,-1,-1,
			1000,1,-1,
			1000,-1,1,
			1000,1,1};
	final static float[] vertices2 = {
			-1,-1000,-1,
			-1,-1000,1,
			1,-1000,-1,
			1,-1000,1,
			-1,1000,-1,
			-1,1000,1,
			1,1000,-1,
			1,1000,1};
	final static float[] vertices3 = {
		-1,-1,-1000,
		1,-1,-1000,
		-1,1,-1000,
		1,1,-1000,
		-1,-1,1000,
		1,-1,1000,
		-1,1,1000,
		1,1,1000};
    
	final static float[] RED = {1.0f, 0.0f, 0.0f, 1.0f};
	final static float[] GREEN = {0.0f, 1.0f, 0.0f, 1.0f};
	final static float[] BLUE = {0.0f, 0.0f, 1.0f, 1.0f};
	public final static float[] NO_FIX_COLOR = {1.0f, 1.0f, 0.0f, 0.8f};
	public final static float[] NO_TYPE_COLOR = {0.3f, 0.3f, 0.3f, 0.8f};
	public final static float[] BACK_COLOR = {1.0f, 0.7f, 0.0f, 0.8f};
	public final static float[] SHOLDER_COLOR = {1.0f, 0.2f, 0.2f, 0.8f};
	public final static float[] ARM_COLOR = {0.5f, 1.0f, 0.0f, 0.8f};
	public final static float[] LEG_COLOR = {0.5f, 0.0f, 1.0f, 0.8f};

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
        initTrail();
        lookX = lookY = 0f;
        lookZ = 0.5f;
        
        setRenderer(new Renderer(){
			@Override
			public void onDrawFrame(GL10 gl) {
				//Log.d("xyz", lookX+","+lookY+","+lookZ);
				gl.glClearColor(0.9f, 0.9f, 0.9f, 1f);
				gl.glClear(GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_COLOR_BUFFER_BIT );
				gl.glMatrixMode(GL10.GL_MODELVIEW);
				gl.glLoadIdentity();
				GLU.gluLookAt(gl, lookX, lookY, lookZ, laX, laY, laZ, 0f, 0f, 1f);
				
				// 地平線
				gl.glDisable(GL10.GL_LIGHTING);
				drawGround(gl);
				gl.glEnable(GL10.GL_LIGHTING);
				
				// 軸
				setColor(gl, RED);
				drawCuboid(gl, vertices1);
				setColor(gl, GREEN);
				drawCuboid(gl, vertices2);
				setColor(gl, BLUE);
				drawCuboid(gl, vertices3);
				
				// 軌跡
				// 色
				if(!fix){
					setColor(gl, NO_FIX_COLOR);
				}
				// 軌跡を描く
				synchronized(tpList){
					int oldXr, oldYr, oldZr, oldXl, oldYl, oldZl;
					oldXr=oldYr=oldZr=oldXl=oldYl=oldZl= 0;
			        for(TrailPoint tp : tpList){
			        	// 部位判別しているとき色付け
			        	if(fix){
			        		if(tp.type == TrailPoint.BACK)			setColor(gl, BACK_COLOR);
			        		else if(tp.type == TrailPoint.SHOLDER)	setColor(gl, SHOLDER_COLOR);
			        		else if(tp.type == TrailPoint.ARM)		setColor(gl, ARM_COLOR);
			        		else if(tp.type == TrailPoint.LEG)		setColor(gl, LEG_COLOR);
			        		else									setColor(gl, NO_TYPE_COLOR);
			        	}
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
				
				// 現在位置
				// 色
				float[] color5 = {1.0f, 0.5f, 0.0f, 0.8f};
				setColor(gl, color5);
				// 形
				int w = 6;
				int h = 7;
				int d = 3;
				float[] self = {
						-h,-w,-d,
						-h,w,-d,
						-h,-w,d,
						-h,w,d,
						h,-w,-d,
						h,w,-d,
						h,-w,d,
						h,w,d};
				w = 5;
				h = 3;
				d = 2;
				float[] self2 = {
						-h,-w,-d,
						-h,w,-d,
						-h,-w,d,
						-h,w,d,
						h,-w,-d,
						h,w,-d,
						h,-w,d,
						h,w,d};
				gl.glPushMatrix();
				// 移動
				gl.glTranslatef(lastTp.x,lastTp.y,lastTp.z);
				// 回転
				gl.glRotatef(-azimuth, 0, 0, 1);
				gl.glRotatef(-pitch, 0, 1, 0);
				gl.glRotatef(-roll, 1, 0, 0);
				//Log.d("APR", azimuth+","+pitch+","+roll);
				drawCuboid(gl, self);
				gl.glTranslatef(10,0,-1);
				drawCuboid(gl, self2);
				gl.glPopMatrix();

				/*
				gl.glLineWidthx(2);
				float[] colorTemp = {1.0f, 0f, 0.5f, 0.8f};
				setColor(gl, colorTemp);
				float[] temp = {0,0,0, xf*20,yf*20,zf*20};
				drawLine(gl, temp);
				float[] temp2 = {0,0,0, xr*20,yr*20,zr*20};
				drawLine(gl, temp2);
				*/
				
			}
			
			@Override
			public void onSurfaceChanged(GL10 gl, int width, int height) {
				gl.glViewport(0, 0, width, height);
				// 視体積の設定
				float ratio = (float)width/height;
				gl.glMatrixMode(GL10.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glFrustumf(-ratio*10, ratio*10, -10, 10, 10, 200);
				//gl.glOrthof(-ratio*100, ratio*100, -100, 100, 1, 300);
				// 法線の有効化
				gl.glEnable(GL10.GL_NORMALIZE);
				// ライティングの有効化
				gl.glEnable(GL10.GL_LIGHTING);
				gl.glEnable(GL10.GL_LIGHT0);
				// シェーディング
				gl.glShadeModel(GL10.GL_SMOOTH);
		        //デプスバッファの指定
		        gl.glEnable(GL10.GL_DEPTH_TEST);
		        gl.glDepthFunc(GL10.GL_LEQUAL);
			}
			@Override
			public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
				
			}}
        );
        
    }
    
    private void initTrail(){
        tpList = new ArrayList<TrailPoint>();
        tpList.add(new TrailPoint(0, 0, 0, 0, 0, 0, 0, 0, 0));
        lastTp = new TrailPoint(0,0,0,0,0,0,0,0,0);
        oldX1 = oldY1 = oldZ1 = 0;
        oldX2 = oldY2 = oldZ2 = 0;
        fix = false;
    }
    
    FloatBuffer array2FloatBuffer(float[] array){
		ByteBuffer byteBuffer= ByteBuffer.allocateDirect(4 * array.length);
		byteBuffer.order(ByteOrder.nativeOrder());
		FloatBuffer fb = byteBuffer.asFloatBuffer();
		fb.put(array);
		fb.position(0);
    	return fb;
    }
    
    void drawGround(GL10 gl) {
        float ground_max_x = 300.0f;
        float ground_max_y = 300.0f;
        gl.glColor4f(0.8f, 0.8f, 0.8f, 1.0f);  // 大地の色
        for(float ly = -ground_max_y ;ly <= ground_max_y; ly+=10.0){
        	float[] v = {-ground_max_x, ly,0, ground_max_x, ly,0};
        	drawLine(gl, v);
        }
        for(float lx = -ground_max_x ;lx <= ground_max_x; lx+=10.0){
        	float[] v = {lx, ground_max_y,0, lx, -ground_max_y,0};
        	drawLine(gl, v);
        }
    }
	
	void drawLine(GL10 gl, float[] v){
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, array2FloatBuffer(v));
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDrawArrays(GL10.GL_LINES, 0, 2 );
	}
	void drawTri(GL10 gl, float[] v){
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, array2FloatBuffer(v));
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		// 法線ベクトルを求める
		float[] x = {v[0], v[3], v[6]};
		float[] y = {v[1], v[4], v[7]};
		float[] z = {v[2], v[5], v[8]};
		float N_x = (y[0]-y[1])*(z[2]-z[1])-(z[0]-z[1])*(y[2]-y[1]);
		float N_y = (z[0]-z[1])*(x[2]-x[1])-(x[0]-x[1])*(z[2]-z[1]);
		float N_z = (x[0]-x[1])*(y[2]-y[1])-(y[0]-y[1])*(x[2]-x[1]);
		float length = (float) Math.sqrt(N_x * N_x + N_y * N_y + N_z * N_z);
		N_x /= length;
		N_y /= length;
		N_z /= length;
		float[] normals = {
				N_x, N_y, N_z,
				N_x, N_y, N_z,
				N_x, N_y, N_z};
		gl.glNormalPointer(GL10.GL_FLOAT, 0, array2FloatBuffer(normals));
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		
		gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3 );
	}
	void drawRect(GL10 gl, float[] v){
		float[] vertices1 = {v[0], v[1], v[2],
							v[3], v[4], v[5],
							v[6], v[7], v[8]};
		float[] vertices2 = {v[6], v[7], v[8],
							v[3], v[4], v[5],
							v[9], v[10], v[11]};
		drawTri(gl, vertices1);
		drawTri(gl, vertices2);
	}
	void drawCuboid(GL10 gl, float[] v){
		float[] vertices1 = {
				v[0], v[1], v[2],
				v[3], v[4], v[5],
				v[6], v[7], v[8],
				v[9], v[10], v[11]};
		drawRect(gl, vertices1);
		float[] vertices2 = {
				v[12], v[13], v[14],
				v[15], v[16], v[17],
				v[18], v[19], v[20],
				v[21], v[22], v[23]};
		drawRect(gl, vertices2);
		float[] vertices3 = {
				v[0], v[1], v[2],
				v[3], v[4], v[5],
				v[12], v[13], v[14],
				v[15], v[16], v[17]};
		drawRect(gl, vertices3);
		float[] vertices4 = {
				v[6], v[7], v[8],
				v[9], v[10], v[11],
				v[18], v[19], v[20],
				v[21], v[22], v[23]};
		drawRect(gl, vertices4);
		float[] vertices5 = {
				v[0], v[1], v[2],
				v[6], v[7], v[8],
				v[12], v[13], v[14],
				v[18], v[19], v[20]};
		drawRect(gl, vertices5);
		float[] vertices6 = {
				v[3], v[4], v[5],
				v[9], v[10], v[11],
				v[15], v[16], v[17],
				v[21], v[22], v[23]};
		drawRect(gl, vertices6);
	}
	
	private void setColor(GL10 gl, float[] light_diffuse){
		float lmodel_ambient[] = { 0.7f, 0.7f, 0.7f, 1.0f }; /* 周囲光 */
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, array2FloatBuffer(light_diffuse));
		gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, array2FloatBuffer(lmodel_ambient));
	}
	

	public LinearLayout getLayout(Context context) {
		int LINE_NUM = 2;
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
        layout.setWeightSum(LINE_NUM);
        
		LinearLayout[] columns = new LinearLayout[LINE_NUM];
		TextView[] labels = new TextView[LINE_NUM];
		for(int i=0; i<LINE_NUM; i++){
			columns[i] = new LinearLayout(context);
			columns[i].setOrientation(LinearLayout.VERTICAL);
			columns[i].setPadding(5, 1, 5, 1);
			labels[i] = new TextView(context);
			columns[i].addView(labels[i]);
			layout.addView(columns[i], lp);
		}
		
		// TrailViewの操作パネル
		labels[0].setText("from:");
		makeLookSeekBar(context, columns[0], seekBarX, 200, 0, "X");
		makeLookSeekBar(context, columns[0], seekBarY, 200, 1, "Y");
		makeLookSeekBar(context, columns[0], seekBarZ, 200, 2, "Z");
		labels[1].setText("at:");
		makeLookSeekBar(context, columns[1], seekBarLaX, 200, 3, "X");
		makeLookSeekBar(context, columns[1], seekBarLaY, 200, 4, "Y");
		makeLookSeekBar(context, columns[1], seekBarLaZ, 200, 5, "Z");
		
		return layout;
	}
	
	public void makeLookSeekBar(Context context, LinearLayout layout, SeekBar sb, final int max, final int tag, String name){
		sb = new SeekBar(context);
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onStartTrackingTouch(SeekBar seekBar) {/* do nothing */}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				switch(tag){
				case 0: setLookX((float)(progress-max*0.5)); break;
				case 1: setLookY((float)(progress-max*0.5)); break;
				case 2:	setLookZ((float)(progress-max*0.5)); break;
				case 3: setLaX((float)(progress-max*0.5)); break;
				case 4: setLaY((float)(progress-max*0.5)); break;
				case 5: setLaZ((float)(progress-max*0.5)); break;
				}
			}
		});
		sb.setMax(max);
		if(tag==0)		sb.setProgress(max/4);
		else if(tag==1)	sb.setProgress(max/4);
		else if(tag==2)	sb.setProgress(max*3/4);
		else if(tag==5)	sb.setProgress(max*3/5);
		else			sb.setProgress(max/2);
		
		TextView label = new TextView(context);
		label.setText(name);
		LinearLayout ll = new LinearLayout(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setWeightSum(10);
		LayoutParams lp1 = new LinearLayout.LayoutParams(
	                    LinearLayout.LayoutParams.FILL_PARENT,
	                    LinearLayout.LayoutParams.WRAP_CONTENT);
		lp1.weight = 9;
		ll.addView(label, lp1);
		LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
		lp2.weight = 1;
		ll.addView(sb, lp2);
		layout.addView(ll);
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
    public void setLaX(float laX) {
		this.laX = laX;
	}
	public void setLaY(float laY) {
		this.laY = laY;
	}
	public void setLaZ(float laZ) {
		this.laZ = laZ;
	}
	
	//　現在位置の設定
	public void setNowData(float azimuth, float pitch, float roll, 
			float xf, float yf, float zf, float xr, float yr, float zr) {
		this.azimuth = azimuth;
		this.pitch = pitch;
		this.roll = roll;
		this.xf = xf;
		this.yf = yf;
		this.zf = zf;
		this.xr = xr;
		this.yr = yr;
		this.zr = zr;
	}

	@Override
    protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Viewの描画サイズを指定する
        setMeasuredDimension(width,2*halfHeight);
    }
    
	
    /** 
     * @param x1,y1,z1 前方の単位ベクトル
     * @param x2,y2,z2 右方の単位ベクトル
     */
    public void addTp(int count, boolean forward, float x1, float y1, float z1, float x2, float y2, float z2, float azimuth, float pitch, float roll ){
		float x,y,z;
    	if(forward){
    		x = lastTp.x + speed * (float)0.5*(oldX1+x1);
    		y = lastTp.y + speed * (float)0.5*(oldY1+y1);
    		z = lastTp.z + speed * (float)0.5*(oldZ1+z1);
    	}else{
    		x = lastTp.x + speed * (float)0.5*(-oldX1-x1);
    		y = lastTp.y + speed * (float)0.5*(-oldY1-y1);
    		z = lastTp.z + speed * (float)0.5*(-oldZ1-z1);
    	}
    	float xr = x + halfW * x2;
    	float yr = y + halfW * y2;
    	float zr = z + halfW * z2;
    	float xl = x - halfW * x2;
    	float yl = y - halfW * y2;
    	float zl = z - halfW * z2;
    	TrailPoint tp = new TrailPoint(xr,yr,zr,xl,yl,zl,azimuth,pitch,roll);
		synchronized(tpList){
			// 追加のとき
			if(count < 0){
				// 先頭に追加
				tpList.add(0, tp);
			}else if(count <= tpList.size()){
				// 末尾に追加
				tpList.add(tp);
			}
			// 書き換えのとき
			else{
				tpList.remove(count);
				tpList.add(count, tp);
			}
		}
		Log.i("ADD TP", tp.azimuth+","+tp.pitch+","+tp.roll);
    	lastTp = tp;
    	oldX1 = x1;
    	oldY1 = y1;
    	oldZ1 = z1;
    	oldX2 = x2;
    	oldY2 = y2;
    	oldZ2 = z2;
    }
    
    public void selectTp(int count){
    	TrailPoint temp;
		synchronized(tpList){
			temp = tpList.get(count);
		}
		if(temp != null)	lastTp = temp;
    }
    
    public TrailPoint getNowTp(){
    	return lastTp;
    }
    
    
    public void clearTrail(){
    	initTrail();
    }

    
    public void setTrail(){
    	fix = true;
    	PointTypeAnalyzer ptr = new PointTypeAnalyzer();
    	ptr.pointAnalyze(tpList);
    }
    
    public int getTpLength(){
    	return tpList.size();
    }
 
 
    public void onResume(){
        // タイマーを作成する
        ses = Executors.newSingleThreadScheduledExecutor();
        // 500msごとにRunnableの処理を実行する
        ses.scheduleAtFixedRate(task, 0L, 500L, TimeUnit.MILLISECONDS);
    }
 
    public void onPause(){
        // タイマーを停止する
        ses.shutdown();
        ses = null;
    }
}
