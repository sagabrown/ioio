package ioio.robot;

import ioio.robot.R;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;

public class MainActivityGroup extends ActivityGroup {
	private ArrayList<Activity> activities;
	// アクティビティを管理するクラス
    private LocalActivityManager lam;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	    // インスタンスの取得
	    lam = getLocalActivityManager();
	    
	    activities = new ArrayList<Activity>();

	    setView(MainActivity.class, "MainActivity");
	    //Log.d("ActivityGroup", getCurrentActivity().getLocalClassName());
	    //setView(SensorTest.class, "SensorTest");
	    //Log.d("ActivityGroup", getCurrentActivity().getLocalClassName());
    }
    
    private void setView(Class<?> cls, String name){
	    // 利用したいActivityのインテントを生成
	    Intent intent = new Intent(getApplicationContext(),cls);
	    // lamを使いインテントからWindowを生成
	    Window window = lam.startActivity(name, intent);
	    //util.startActivity(lam, name, intent);
	    ViewGroup group = (ViewGroup)findViewById(R.id.layout);
	    // WindowのオブジェクトからView情報を取得しレイアウトにセットする
	    group.addView(window.getDecorView());
	    // フィールドに保持
	    activities.add(lam.getActivity(name));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	return getLocalActivityManager().getCurrentActivity().onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	return getLocalActivityManager().getCurrentActivity().onMenuItemSelected(featureId, item);
    }
    
}
