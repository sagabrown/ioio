package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import android.widget.LinearLayout;

public interface Motor {
	public void setSpec();
	public void init();
	public double thetaToDuty(double theta);
	public double ratioToDuty(double ratio);
	public LinearLayout getOperationLayout(MainActivity parent);
	public int openPin(IOIO ioio, int num) throws ConnectionLostException;
	public void activate() throws ConnectionLostException;
	public void disactivate() throws ConnectionLostException;
	public void disconnected() throws ConnectionLostException;
	public int getFreq();
	public double getMinTheta();
	public double getMaxTheta();
	public double getState();
	public int getPinNum();
}
