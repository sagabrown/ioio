package ioio.robot.region.crawl;

import ioio.lib.api.IOIO;
import ioio.robot.region.Region;
import ioio.robot.part.motor.DCMotor;

public class Wheel extends Region {

	private final static float GO_FORWARD = 1.0f;
	private final static float GO_INIT = 0f;
	private final static float GO_BACKWARD = -1.0f;
	private final static float GO_DD = 0.1f;
	private final static int GO_SLEEP = 10;
	
	private DCMotor[] motor;
	
	
	
	/** êiÇﬁ 
	 * @throws InterruptedException **/
	public void goForward() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s<GO_FORWARD; s+=GO_DD){
			motor[0].changeState(s);
			Thread.sleep(GO_SLEEP);
		}
		motor[0].changeState(GO_FORWARD);
	}
	/** ñﬂÇÈ 
	 * @throws InterruptedException **/
	public void goBackward() throws InterruptedException{
		float state = (float)motor[0].getState();
		for(float s=state; s>GO_BACKWARD; s-=GO_DD){
			motor[0].changeState(s);
			Thread.sleep(GO_SLEEP);
		}
		motor[0].changeState(GO_BACKWARD);
	}
	/** í‚é~ **/
	public void stop(){
		motor[0].changeState(GO_INIT);
	}
	
	
	@Override
	public boolean openPins(IOIO ioio, int[] pinNums) {
		// TODO Auto-generated method stub
		return false;
	}
}
