package ioio.robot.util;

import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**  Handlerに渡すためのクラス  **/
public class SetViewRunnable implements Runnable {
	private int mode;
	private TextView textView;
	private String str;
	private SeekBar seekBar;
	private int progress;
	private View view;
	private boolean enabled;
	private int visibility;
	
	/* textView */
	SetViewRunnable( TextView view, String string ){
		mode = 0;
		this.textView = view;
		this.str = string;
	}
	/* seekBar */
	SetViewRunnable( SeekBar seekBar, int progress ){
		mode = 1;
		this.seekBar = seekBar;
		this.progress = progress;
	}
	/* enabled */
	SetViewRunnable( View view, boolean enabled ){
		mode = 2;
		this.view = view;
		this.enabled = enabled;
	}
	/* visibility */
	SetViewRunnable( View view, int visibility ){
		mode = 3;
		this.view = view;
		this.visibility = visibility;
	}
	public void run(){
		switch(mode){
		case 0:
			textView.setText(str);
			break;
		case 1:
			seekBar.setProgress(progress);
			break;
		case 2:
			view.setEnabled(enabled);
			break;
		case 3:
			view.setVisibility(visibility);
			break;
		}
	}
}
