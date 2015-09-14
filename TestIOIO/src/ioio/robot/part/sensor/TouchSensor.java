package ioio.robot.part.sensor;

import android.util.Log;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.part.PinOpenable;
import ioio.robot.util.Util;

public class TouchSensor implements PinOpenable {
	private PulseInput pin;
	protected static final int pinNum = 1;	// ïKóvÇ»ÉsÉìÇÃêî
	private boolean isActive;

	private static final String TAG = "TouchSensor";
	private Util util;
	private String name;
	private boolean isAutoControlled;
	private final static int initState = 0;
	private int state;
	

	public TouchSensor( Util util, String name) {
		this.util = util;
		this.name = name;
		init();
	}

	@Override
	public void init() {
		state = initState;
		isActive = false;
	}

	@Override
	public boolean openPins(IOIO ioio, int[] nums) throws ConnectionLostException{
		if(nums.length != pinNum){
			Log.e(TAG, "cannot open pin: Ellegal pinNum");
			return false;
		}
		pin = ioio.openPulseInput(nums[0], PulseMode.POSITIVE);
		return true;
	}

	@Override
	public void activate() throws ConnectionLostException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disactivate() throws ConnectionLostException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnected() throws ConnectionLostException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setIsAutoControlled(boolean tf) {
		
		
	}

}
