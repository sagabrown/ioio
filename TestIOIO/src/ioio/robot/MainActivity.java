package ioio.robot;

import ioio.robot.R;
import ioio.robot.robot.CrawlRobot;
import ioio.robot.robot.Robot;
import ioio.robot.util.Util;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;


public class MainActivity extends IOIOActivity {
	private Util util;
	private ToggleButton button_;
	private Robot robot_;

	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		util = new Util(new Handler());
		
		//robot_ = new TEROOS(util);
		robot_ = new CrawlRobot(util);
		
        /* アクティビティビューにレイアウトをセットする　*/
        setContentView(R.layout.controller);
		LinearLayout additionalLayout = (LinearLayout) findViewById(R.id.additionalLayout); 
		additionalLayout.addView(robot_.getLayout(this));
		
        /* ボタンの処理設定 */
		button_ = (ToggleButton) findViewById(R.id.button);
		button_.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				try {
					if(button_.isChecked()){
						robot_.activate();
					}else{
						robot_.disactivate();
					}
				} catch (ConnectionLostException e) {
					e.printStackTrace();
				}
			}
		});
		util.setEnabled(button_, false);	// IOIOに接続していないうちは押せない
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {
		/** The on-board LED. */
		private DigitalOutput led_;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException, InterruptedException {
			led_ = ioio_.openDigitalOutput(0, true);	// ledライト
			Log.d("debug", "open pins!!!");
			robot_.openPins(ioio_, 1);					// ピンにモーターを対応させる
			util.setEnabled(button_, true);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException {
			led_.write(!button_.isChecked());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		public void disconnected(){
			try {
				robot_.disconnected();
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
			util.setEnabled(button_, false);
			Log.d("debug", "disconnected!!!");
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}