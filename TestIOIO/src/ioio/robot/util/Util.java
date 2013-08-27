package ioio.robot.util;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class Util {
	private Handler handler;
    public Util(Handler handler) {
		this.handler = handler;
	}
	public void setText(TextView textView, String string){
		handler.post( new SetViewRunnable(textView, string) );
	}
    public void setProgress(SeekBar seekBar, int progress){
		handler.post( new SetViewRunnable(seekBar, progress) );
	}
    public void setEnabled(View view, boolean enabled){
		handler.post( new SetViewRunnable(view, enabled) );
	}
    public void startActivity(LocalActivityManager lam, String name, Intent intent){
		handler.post( new ManageActivityRunnable(0, lam, name, intent) );
	}
}
