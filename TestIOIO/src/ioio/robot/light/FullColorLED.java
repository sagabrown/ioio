package ioio.robot.light;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.controller.MainActivity;
import ioio.robot.util.Util;

public class FullColorLED {
	private LED[] led;
	private boolean isActive;

	private LinearLayout layout, line1, line2;
	private Button[] colorButton;
	private float[] color;
	private float luminous;
	private TextView label;
	
	private Util util;
	private String name;
	
	public FullColorLED(Util util, String name) {
		this.util = util;
		this.name = name;
		led = new LED[3];
		led[0] = new LED_R(util, "R");	// R
		led[1] = new LED_G(util, "G");	// G
		led[2] = new LED_B(util, "B");	// B
		init();
	}
	
	/** 初期化 **/
	public void init(){
		for(LED l : led)	l.init();
		isActive = false;
	}

	/** 受け取った番号のピンを開いて対応づける(開いたピンの数1を返す) **/
	public int openPin(IOIO ioio, int num) throws ConnectionLostException{
		for(LED l : led)	num += l.openPin(ioio, num);
		return 3;
	}

	/** 操作パネルを生成して返す **/
	public LinearLayout getOperationLayout(Context parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 1, 10, 1);
        
		label = new TextView(parent);
		util.setText(label, name);
		layout.addView(label);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
		
		line1 = new LinearLayout(parent);
        line1.setOrientation(LinearLayout.HORIZONTAL);
        line1.setWeightSum(4);
		colorButton = new Button[4];
		for(int i=0; i<colorButton.length; i++){
			colorButton[i] = new Button(parent);
			line1.addView(colorButton[i], lp);
			util.setEnabled(colorButton[i], false);
		}
		colorButton[0].setText("red");
        colorButton[0].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				red();
			}
        });
		colorButton[1].setText("green");
        colorButton[1].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				green();
			}
        });
		colorButton[2].setText("blue");
        colorButton[2].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				blue();
			}
        });
		colorButton[3].setText("off");
        colorButton[3].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				off();
			}
        });
		
        /* 子のシークバーを作成して登録　*/
		line2 = new LinearLayout(parent);
        line2.setOrientation(LinearLayout.HORIZONTAL);
        line2.setWeightSum(3);
		for(LED l : led)	line2.addView(l.getOperationLayout(parent), lp);
		
		layout.addView(line2);
		layout.addView(line1);
        
		return layout;
	}
	
	/** ワンタッチ・赤 **/
	public void red(){
		float[] clr = {1f, 0f, 0f};
		setColor(clr);
		setLuminous(1f);
	}
	/** ワンタッチ・緑 **/
	public void green(){
		float[] clr = {0f, 1f, 0f};
		setColor(clr);
		setLuminous(1f);
	}
	/** ワンタッチ・青 **/
	public void blue(){
		float[] clr = {0f, 0f, 1f};
		setColor(clr);
		setLuminous(1f);
	}
	/** ワンタッチ・オフ **/
	public void off(){
		float[] clr = {0f, 0f, 0f};
		setColor(clr);
		setLuminous(0f);
	}
	/** 任意の色に設定 **/
	public void setColor(float[] clr){
		if(clr.length < 3)	return;
		for(int i=0; i<3; i++)	color[i] = clr[i];
		for(int i=0; i<3; i++)	led[i].changeState(color[i]*luminous, false);
	}
	/** 光の強さを設定 **/
	public void setLuminous(float lum){
		luminous = lum;
		for(int i=0; i<3; i++)	led[i].changeState(color[i]*luminous, false);
	}
	
	public void activate() throws ConnectionLostException {
		isActive = true;
		for(Button b : colorButton)	util.setEnabled(b, true);
		for(LED l : led)	l.activate();
	}
	public void disactivate() throws ConnectionLostException {
		isActive = false;
		for(Button b : colorButton)	util.setEnabled(b, false);
		for(LED l : led)	l.disactivate();
	}
	public void disconnected() throws ConnectionLostException {
		isActive = false;
		for(Button b : colorButton)	util.setEnabled(b, false);
		for(LED l : led)	l.disconnected();
	}

}
