package ioio.robot.util;

import android.app.LocalActivityManager;
import android.content.Intent;

/**  Handler‚É“n‚·‚½‚ß‚ÌƒNƒ‰ƒX  **/
public class ManageActivityRunnable implements Runnable {
	private int mode;
	private LocalActivityManager lam;
	private String name;
	private Intent intent;
	
	/* startActivity */
	public ManageActivityRunnable( int mode, LocalActivityManager lam,
			String name, Intent intent) {
		this.mode = mode;
		this.lam = lam;
		this.name = name;
		this.intent = intent;
	}

	@Override
	public void run() {
		switch(mode){
		case 0:
			lam.startActivity(name, intent);
			break;
		}
		
	}

}
