package ioio.robot.robot;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.MainActivity;
import android.content.Context;
import android.widget.LinearLayout;

public interface Robot {
	public LinearLayout getLayout(Context parent);
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException, InterruptedException;
	public void activate() throws ConnectionLostException;
	public void disactivate() throws ConnectionLostException;
	public void disconnected() throws ConnectionLostException;
	public void setSpeed(float speed);
	public void incCount();
	public void decCount();
	public void onResume();
	public void onPause();
}
