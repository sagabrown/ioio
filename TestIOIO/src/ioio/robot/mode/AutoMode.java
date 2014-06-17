package ioio.robot.mode;

import java.util.concurrent.ScheduledExecutorService;

import ioio.robot.robot.CrawlRobot;
import ioio.robot.util.Util;
import android.content.Context;
import android.text.Layout;
import android.widget.ToggleButton;

public abstract class AutoMode {
	protected Util util;
	protected CrawlRobot robot;
	protected ToggleButton button;
	protected boolean isAuto;
	protected ScheduledExecutorService[] ses;
	
	
	public abstract void start();
	public abstract void stop();
	
	public void buttonOn(){
		button.setChecked(true);
	}
	public void buttonOff(){
		button.setChecked(false);
	}
	
	protected abstract void generateButton(Context context);
	
	public ToggleButton getOnOffButton(Context context) {
		if(button==null)	generateButton(context);
		return button;
	}
	
	public boolean hasExtraLayout() {
		return false;
	}
	public Layout getExtraLayout() {
		return null;
	}
}
