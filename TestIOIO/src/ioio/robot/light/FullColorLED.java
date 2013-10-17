package ioio.robot.light;

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
	
	/** ������ **/
	public void init(){
		for(LED l : led)	l.init();
		isActive = false;
	}

	/** �󂯎�����ԍ��̃s�����J���đΉ��Â���(�J�����s���̐�1��Ԃ�) **/
	public int openPin(IOIO ioio, int num) throws ConnectionLostException{
		for(LED l : led)	num += l.openPin(ioio, num);
		return 3;
	}

	/** ����p�l���𐶐����ĕԂ� **/
	public LinearLayout getOperationLayout(MainActivity parent){
		layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        
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
		}
		colorButton[0].setText("��");
        colorButton[0].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				red();
			}
        });
		colorButton[1].setText("��");
        colorButton[1].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				green();
			}
        });
		colorButton[2].setText("��");
        colorButton[2].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				blue();
			}
        });
		colorButton[3].setText("��");
        colorButton[3].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				off();
			}
        });
		
        /* �q�̃V�[�N�o�[���쐬���ēo�^�@*/
		line2 = new LinearLayout(parent);
        line2.setOrientation(LinearLayout.HORIZONTAL);
        line1.setWeightSum(3);
		for(LED l : led)	line2.addView(l.getOperationLayout(parent), lp);
		
		layout.addView(line1);
		layout.addView(line2);
        
		return layout;
	}
	
	/** �����^�b�`�E�� **/
	private void red(){
		led[0].changeState(1f, false);
		led[1].changeState(0f, false);
		led[2].changeState(0f, false);
	}
	/** �����^�b�`�E�� **/
	private void green(){
		led[0].changeState(0f, false);
		led[1].changeState(1f, false);
		led[2].changeState(0f, false);
	}
	/** �����^�b�`�E�� **/
	private void blue(){
		led[0].changeState(0f, false);
		led[1].changeState(0f, false);
		led[2].changeState(1f, false);
	}
	/** �����^�b�`�E�I�t **/
	private void off(){
		led[0].changeState(0f, false);
		led[1].changeState(0f, false);
		led[2].changeState(0f, false);
	}
	
	public void activate() throws ConnectionLostException {
		isActive = true;
		for(LED l : led)	l.activate();
	}
	public void disactivate() throws ConnectionLostException {
		isActive = false;
		for(LED l : led)	l.disactivate();
	}
	
	public void disconnected() throws ConnectionLostException {
		isActive = false;
		for(LED l : led)	l.disconnected();
	}

}
