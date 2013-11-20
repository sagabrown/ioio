package ioio.robot.controller.robot;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import android.widget.LinearLayout;

public interface Robot {
	public LinearLayout getLayout(MainActivity parent);
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException, InterruptedException;
	public void activate() throws ConnectionLostException;
	public void disactivate() throws ConnectionLostException;
	public void disconnected() throws ConnectionLostException;
}
