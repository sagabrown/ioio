package ioio.robot.part.motor;

import ioio.robot.part.PinOpenable;
import android.content.Context;
import android.widget.LinearLayout;

public interface Motor extends PinOpenable {
	public void setSpec();
	public void init();
	public double thetaToDuty(double theta);
	public double ratioToDuty(double ratio);
	public LinearLayout getOperationLayout(Context parent);
	public int getFreq();
	public double getMinTheta();
	public double getMaxTheta();
	public double getState();
	public int getPinNum();
	public void setIsAutoControlled(boolean tf);
	public void changeState(float state);
	public void changeStateByRad(float rad);
}
