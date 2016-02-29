package ioio.robot.part.sensor;

import java.util.ArrayList;

import android.util.Log;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.part.PinOpenable;
import ioio.robot.util.Util;

public class TouchSensor implements PinOpenable {
	private final static boolean DEBUG = true;
	
	private DigitalInput pin;
	protected static final int pinNum = 1;	// ïKóvÇ»ÉsÉìÇÃêî
	private boolean isActive;

	public final static int NOT_TOUCH = 0;
	public final static int SHORT_TOUCH = 1;
	public final static int LONG_TOUCH = 2;
	public final static int ATTACK_TOUCH = 3;

	private static final String TAG = "TouchSensor";
	private Util util;
	private String name;
	private boolean isAutoControlled;
	private ArrayList<Boolean> state;

	private final static int LIST_LENGTH = 30;
	private final static int LONG_TOUCH_THRESHOLD = 20;
	private final static int SHORT_TOUCH_THRESHOLD = 4;
	private final static int ATTACK_TOUCH_THRESHOLD = 1;
	

	public TouchSensor( Util util, String name) {
		this.util = util;
		this.name = name;
		init();
	}

	@Override
	public void init() {
		state = new ArrayList<Boolean>(LIST_LENGTH);
		isActive = false;
	}
	
	public void addData() throws InterruptedException, ConnectionLostException{
		if(state != null){
			if(state.size() == LIST_LENGTH){
				state.remove(0);
			}
			state.add(pin.read());
		}
	}
	
	public int checkTouch(){
		int count = 0;
		boolean nowEnd = false;
		int trackStart = state.size()-1;

		if(state.size() < 5){	
			return NOT_TOUCH;
		}
		
		if(state.get(state.size()-1) == false && state.get(state.size()-2) == true){
			nowEnd = true;
			trackStart = state.size()-2;
		}
		
		for(int i = trackStart; i>=0; i--){
			if(state.get(i)){
				count++;
			}else{
				break;
			}
		}
		
		if(DEBUG){
			String debugMsg = "";
			for(int i=0; i<state.size(); i++){
				if(state.get(i)){
					debugMsg += "1,";
				}else{
					debugMsg += "0,";
				}
			}
			Log.i(TAG, debugMsg);
		}
		
		if(count >= LONG_TOUCH_THRESHOLD){
			return LONG_TOUCH;
		}else if(count >= SHORT_TOUCH_THRESHOLD && nowEnd){
			return SHORT_TOUCH;
		}else if(count >= ATTACK_TOUCH_THRESHOLD && nowEnd){
			return ATTACK_TOUCH;
		}else{
			return NOT_TOUCH;
		}
	}

	@Override
	public boolean openPins(IOIO ioio, int[] nums) throws ConnectionLostException{
		if(nums.length != pinNum){
			Log.e(TAG, "cannot open pin: Ellegal pinNum");
			return false;
		}
		pin = ioio.openDigitalInput(nums[0], DigitalInput.Spec.Mode.PULL_DOWN);
		return true;
	}

	@Override
	public void activate() throws ConnectionLostException {
		isActive = true;
		init();
	}

	@Override
	public void disactivate() throws ConnectionLostException {
		isActive = false;
	}

	@Override
	public void disconnected() throws ConnectionLostException {
		disactivate();
	}

	@Override
	public void setIsAutoControlled(boolean tf) {
		return;
	}

}
