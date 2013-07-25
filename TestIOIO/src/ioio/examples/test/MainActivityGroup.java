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
	// �A�N�e�B�r�e�B���Ǘ�����N���X
    private LocalActivityManager lam;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		util = new Util(new Handler());
        setContentView(R.layout.main);
	    // �C���X�^���X�̎擾
	    lam = getLocalActivityManager();

	    setView(SensorTest.class, "SensorTest");
	    setView(MainActivity.class, "MainActivity");
    }
    
    private void setView(Class<?> cls, String name){
	    // ���p������Activity�̃C���e���g�𐶐�
	    Intent intent = new Intent(getApplicationContext(),cls);
	    // lam���g���C���e���g����Window�𐶐�
	    Window window = lam.startActivity(name, intent);
	    //util.startActivity(lam, name, intent);
	    ViewGroup group = (ViewGroup)findViewById(R.id.layout);
	    // Window�̃I�u�W�F�N�g����View�����擾�����C�A�E�g�ɃZ�b�g����
	    group.addView(window.getDecorView());
    }
}
