package ioio.robot.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
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
    
    private String fullDirName;
    private boolean dirMade;
    public void saveText(Context context, String fname, String text){
    	// サブディレクトリの作成
    	if(!dirMade){
    		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd_HHmmss");
    		Date now = new Date();
			String subDirName = "data_"+f.format(now);
			fullDirName = Environment.getExternalStorageDirectory() + "/ioio.robot/" + subDirName;
			File dir = new File(fullDirName);
			if (!dir.exists()) {
			    dir.mkdirs();
			}
			dirMade = true;
    	}
    	
        String filePath = fullDirName + "/" + fname + ".dat";
        String inputText = text;
        File file = new File(filePath);
        file.getParentFile().mkdir();
                
        try {
          FileOutputStream outStream = new FileOutputStream(file, true);
          OutputStreamWriter writer = new OutputStreamWriter(outStream);
          writer.write(inputText);
          writer.flush();
          writer.close();
          Log.i("saveText", "write accels to a file!!! : "+filePath);
          FileInputStream inStream = new FileInputStream(file);
          BufferedReader reader= new BufferedReader(new InputStreamReader(inStream,"UTF-8"));
          String lineBuffer;
          while( (lineBuffer = reader.readLine()) != null ){
              Log.d("FileAccess",lineBuffer);
          }
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
}
