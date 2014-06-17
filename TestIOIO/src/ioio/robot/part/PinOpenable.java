package ioio.robot.part;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public interface PinOpenable {
	public void init();
	public boolean openPins(IOIO ioio, int[] nums) throws ConnectionLostException;
	public void activate() throws ConnectionLostException;
	public void disactivate() throws ConnectionLostException;
	public void disconnected() throws ConnectionLostException;
	public void setIsAutoControlled(boolean tf);
}
