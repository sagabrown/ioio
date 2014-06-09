package ioio.robot.region;

import ioio.lib.api.IOIO;

public abstract class Region {
	public abstract boolean openPins(IOIO ioio, int[] pinNums);
	public void activate() {
	}
	public void disactivate() {
	}
	public void disconnected() {
	}
}
