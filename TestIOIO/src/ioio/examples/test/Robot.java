package ioio.examples.test;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import android.widget.LinearLayout;

public interface Robot {
	public LinearLayout getLayout(MainActivity parent);
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException;
	public void activate() throws ConnectionLostException;
	public void disactivate() throws ConnectionLostException;
	public void disconnected() throws ConnectionLostException;
}