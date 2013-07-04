package ioio.examples.test;

import ioio.examples.test.R;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.widget.*;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity {
	private ToggleButton button_;
	private SeekBar seekBarPwm_[], seekBarDCMotor_;
	private static int seekBarPwmId[] = {R.id.seekBarPwm1, R.id.seekBarPwm2, R.id.seekBarPwm3,
			R.id.seekBarPwm4, R.id.seekBarPwm5, R.id.seekBarPwm6, R.id.seekBarPwm7};
	private final static int motorNum = seekBarPwmId.length;  // 操作するモーターの数
	private Motor motor_[];
	private DCMotor dcMotor_;

	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		button_ = (ToggleButton) findViewById(R.id.button);
		
		/* 初期角度を設定する */
		double motorInitState[] = {Math.PI*0.5, -Math.PI*0.5, Math.PI*0.25, -Math.PI*0.25, 0.0, 0.0, 0.0};
		motor_ = new Motor[motorNum];
		motor_[0] = new HS322HD(motorInitState[0]);  // 頭
		motor_[1] = new HS322HD(motorInitState[1]);  // まぶた
		motor_[2] = new HS322HD(motorInitState[2]);  // 目
		motor_[3] = new HS322HD(motorInitState[3]);  // 首（傾げる）
		motor_[4] = new HS322HD(motorInitState[4]);  // 首（頷く）
		motor_[5] = new HS322HD(motorInitState[5]);  // 首（振る）
		motor_[6] = new ServoMotor(motorInitState[6]);  // <未使用>
		
		seekBarPwm_ = new SeekBar[motorNum];
		for(int i=0; i<motorNum; i++){
			seekBarPwm_[i] = (SeekBar) findViewById(seekBarPwmId[i]);
			seekBarPwm_[i].setProgress(thetaToProgress(motorInitState[i], motor_[i], seekBarPwm_[i].getMax()));
		}
		
		
		/* DCモータの設定 */
		dcMotor_ = new DCMotor();
		seekBarDCMotor_ =(SeekBar) findViewById(R.id.seekBarDCMotor);
		seekBarDCMotor_.setProgress(seekBarDCMotor_.getMax()/2);
	}
	
	private int thetaToProgress(double theta, Motor motor, int maxProgress){
		return (int)( (theta-motor.getMinTheta())
				/ (motor.getMaxTheta()-motor.getMinTheta()) 
				* maxProgress );
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
		private PwmOutput pwm_[];
		private PwmOutput dcpwm1_, dcpwm2_;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(0, true);
			pwm_ = new PwmOutput[motorNum];  // ピン1～ピンmotorNumに対応. 更にmotor[0]~motor[motorNum-1]に対応
			for(int i=0; i<motorNum; i++){
				pwm_[i] = ioio_.openPwmOutput(i+1, motor_[i].getFreq());
			}
			
			// ピン10と11はペア. DCモーターの制御
			dcpwm1_ = ioio_.openPwmOutput(10, dcMotor_.getFreq());
			dcpwm2_ = ioio_.openPwmOutput(11, dcMotor_.getFreq());
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
			if(button_.isChecked()){
				for(int i=0; i<motorNum; i++){
					pwm_[i].setDutyCycle((float) motor_[i].getDuty2((double)seekBarPwm_[i].getProgress() / seekBarPwm_[i].getMax()));
				}
				
				float dcProgress = (float)((double)seekBarDCMotor_.getProgress() / seekBarDCMotor_.getMax() * 2.0 - 1.0);
				if(dcProgress < 0){
					dcpwm1_.setDutyCycle(0);
					dcpwm2_.setDutyCycle(-dcProgress);
				}else{
					dcpwm1_.setDutyCycle(dcProgress);
					dcpwm2_.setDutyCycle(0);
				}
			}else{
				dcpwm1_.setDutyCycle(0);
				dcpwm2_.setDutyCycle(0);
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
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