package ioio.examples.test;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.view.Window;

public class MainActivityGroup extends ActivityGroup {
	private Util util;
	// アクティビティを管理するクラス
    private LocalActivityManager lam;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		util = new Util(new Handler());
        setContentView(R.layout.main);
	    // インスタンスの取得
	    lam = getLocalActivityManager();

	    setView(SensorTest.class, "SensorTest");
	    setView(MainActivity.class, "MainActivity");
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
    }
}
