package ioio.robot.region;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.mode.AutoMode;
import ioio.robot.part.PinOpenable;
import ioio.robot.util.Util;

public abstract class Region {
	protected String TAG = "Region";
	protected Util util;
	protected ScheduledExecutorService ses;
	protected boolean isAuto;
	protected PinOpenable[] part;
	protected AutoMode owner;
	
	public void init(){
		for(PinOpenable p : part)	p.init();
		owner = null;
		ses = Executors.newSingleThreadScheduledExecutor();
	}
	
	public abstract LinearLayout getLayout(Context context);

	public boolean openPins(IOIO ioio, int[][] pinNums) {
		if(part.length != pinNums.length){
			Log.e(TAG, "cannot open pin: Ellegal pinNum");
			return false;
		}
		for(int i=0; i<part.length; i++){
			try {
				part[i].openPins(ioio, pinNums[i]);
			} catch (ConnectionLostException e) {
				Log.e(TAG, "cannot open pin: ConnectionLostException");
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				Log.e(TAG, "cannot open pin: Exception");
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	public void activate() throws ConnectionLostException {
		for(PinOpenable p : part)	p.activate();
	}
	public void disactivate() throws ConnectionLostException {
		for(PinOpenable p : part)	p.disactivate();
	}
	public void disconnected() throws ConnectionLostException {
		for(PinOpenable p : part)	p.disconnected();
	}
	
	/** ƒI[ƒg§Œä‚«‚è‚©‚¦ **/
	public void setIsAutoControlled(AutoMode mode){
		if(mode == null){
			isAuto = false;
		}else{
			isAuto = true;
			setOwner(mode);
		}
		for(PinOpenable p : part)	p.setIsAutoControlled(isAuto);
	}

	public AutoMode getOwner() {
		return owner;
	}

	public void setOwner(AutoMode owner) {
		this.owner = owner;
	}
}
