package ioio.robot.part.light;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.robot.part.PinOpenable;
import ioio.robot.util.Util;

public class FullColorLED implements PinOpenable {
	private LED[] led;
	private boolean isActive;

	private LinearLayout layout, line1, line2;
	private Button[] colorButton;
	private float[] color;
	private float luminous;
	private TextView label;
	
	private Util util;
	private String name;
	private boolean isAutoControlled;
	
	public FullColorLED(Util util, String name, float[] initState) {
		this.util = util;
		this.name = name;
		color = new float[3];
		led = new LED[3];
		if(initState.length < 3)	initState = new float[3];
		led[0] = new LED_R(util, "R", initState[0]);	// R
		led[1] = new LED_G(util, "G", initState[1]);	// G
		led[2] = new LED_B(util, "B", initState[2]);	// B
		init();
	}
	
	/** ������ **/
	public void init(){
		for(LED l : led)	l.init();
		isActive = false;
	}

	/** �󂯎�����ԍ��̃s�����J���đΉ��Â��� **/
	public boolean openPins(IOIO ioio, int[] nums) throws ConnectionLostException{
		for(int i=0; i<led.length; i++){
			int[] num = new int[1];
			num[0] = nums[i];
			if( !led[i].openPins(ioio, num) )	return false;
		}
		return true;
	}

	/** ����p�l���𐶐����ĕԂ� **/
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
		
        /* �q�̃V�[�N�o�[���쐬���ēo�^�@*/
		line2 = new LinearLayout(parent);
        line2.setOrientation(LinearLayout.HORIZONTAL);
        line2.setWeightSum(3);
		for(LED l : led)	line2.addView(l.getOperationLayout(parent), lp);
		
		layout.addView(line2);
		layout.addView(line1);
        
		return layout;
	}
	
	/** �����^�b�`�E�� **/
	public void red(){
		float[] clr = {1f, 0f, 0f};
		setColor(clr);
		setLuminous(1f);
	}
	/** �����^�b�`�E�� **/
	public void green(){
		float[] clr = {0f, 1f, 0f};
		setColor(clr);
		setLuminous(1f);
	}
	/** �����^�b�`�E�� **/
	public void blue(){
		float[] clr = {0f, 0f, 1f};
		setColor(clr);
		setLuminous(1f);
	}
	/** �����^�b�`�E�I�t **/
	public void off(){
		float[] clr = {0f, 0f, 0f};
		setColor(clr);
		setLuminous(0f);
	}
	/** �C�ӂ̐F�ɐݒ� **/
	public void setColor(float[] clr){
		if(clr.length < 3)	return;
		for(int i=0; i<3; i++)	color[i] = clr[i];
		for(int i=0; i<3; i++)	led[i].changeState(color[i]*luminous, false);
	}
	/** ���̋�����ݒ� **/
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

	public void setIsAutoControlled(boolean isAutoControlled){
		this.isAutoControlled = isAutoControlled;
		for(LED l : led)	l.setIsAutoControlled(isAutoControlled);
		if(isActive)	for(Button b : colorButton)	util.setEnabled(b, !isAutoControlled);
	}

}
