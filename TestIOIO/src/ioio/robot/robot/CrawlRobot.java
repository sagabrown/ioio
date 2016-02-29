package ioio.robot.robot;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.momot.MainActivity;
import ioio.momot.R;
import ioio.robot.mode.crawl.AutoEmoMode;
import ioio.robot.mode.crawl.InteractionMode;
import ioio.robot.mode.crawl.PointOutMode;
import ioio.robot.mode.crawl.ShowInfoMode;
import ioio.robot.mode.crawl.TestMode;
import ioio.robot.part.light.FullColorLED;
import ioio.robot.part.light.LED;
import ioio.robot.part.motor.DCMotor;
import ioio.robot.part.motor.Motor;
import ioio.robot.part.motor.SG90;
import ioio.robot.part.motor.ServoMotor;
import ioio.robot.part.sensor.TouchSensor;
import ioio.robot.region.crawl.Ears;
import ioio.robot.region.crawl.Eyes;
import ioio.robot.region.crawl.Wheel;
import ioio.robot.region.crawl.sensor.SensorTester;
import ioio.robot.region.crawl.sensor.SpeedMater;
import ioio.robot.region.crawl.sensor.TrailPoint;
import ioio.robot.region.crawl.sensor.TrailView;
import ioio.robot.util.Util;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;


public class CrawlRobot implements Robot {
	private final static String TAG = "CrawlRobot";
	private Util util;
	private SharedPreferences sharedPreferences;
	
	// setting�œǂݍ��ނ���
	private boolean usingMode = true;
	private boolean gettingTrail = true;
	
	private final static String FOR_TEXT = "for";
	private final static String BACK_TEXT = "back";
	private final static String STOP_TEXT = "��";
	
	public Wheel wheel;
	public Ears ears;
	public Eyes eyes;
	public TouchSensor[] touchSensor;

	/**
	pin 1, 2	: i2c�Z���T�ʐM
	pin 4		: ��]������
	pin 5, 6	: DC���[�^
	pin 7		: �T�[�{���[�^
	pin 9		: �X�s�[�J�[?
	pin 12-14	: LED
	pin 10, 11	: ���~�b�g�X�C�b�`
	**/
	private final static int speedMaterPinNum = 4;
	private final static int sensorPinNum = 1;
	private final static int[][] wheelPinNums = {{5, 6}};
	private final static int[][] earsPinNums = {{7}};
	private final static int[][] eyesPinNums = {{12,13,14}};
	private final static int[][] touchSensorPinNum = {{10}, {11}};
	
	private SpeedMater speedMater;
	public SensorTester sensor;
    // ��{�I�ɂ͐ݒ肩��ǂݍ���
	private int distPerCycle = 48;	// ���[�^�[1��]�Ői�ދ���[mm]
	private int slitNum = 4;
	
	private TestMode testMode;
	private AutoEmoMode autoEmoMode;
	private ShowInfoMode showInfoMode;
	private PointOutMode pointOutMode;
	private InteractionMode interactionMode;
	
	private LinearLayout layout;
	private LinearLayout modeSelectLayout, manualContollerLayout, sensorTextLayout, trailControllerLayout, trailViewLayout;
	private FrameLayout sensorLayout;
	private ToggleButton autoButton, autoEmoButton, showInfoButton, pointOutButton, interactionButton;
	private Button backButton, forwardButton, stopButton;
	private Button[] emoButton;
    private boolean isActive, isAuto;
	
	/** �R���X�g���N�^ **/
	public CrawlRobot(Util util, SharedPreferences sharedPreferences) {
		super();
		this.util = util;
		this.sharedPreferences = sharedPreferences;
		
		wheel = new Wheel(util, this);
		ears = new Ears(util);
		eyes = new Eyes(util);
		speedMater = new SpeedMater(util, distPerCycle, slitNum, this);
		sensor = new SensorTester(util, this);
		touchSensor = new TouchSensor[2];
		touchSensor[0] = new TouchSensor(util, "head");
		touchSensor[1] = new TouchSensor(util, "back");

		testMode = new TestMode();
		autoEmoMode = new AutoEmoMode();
		showInfoMode = new ShowInfoMode();
		pointOutMode = new PointOutMode();
		interactionMode = new InteractionMode();
		init();
	}

	/** �����ݒ� **/
	private void init(){
		testMode.setParams(util, this);
		autoEmoMode.setParams(util, this);
		showInfoMode.setParams(util, this);
		pointOutMode.setParams(util, this);
		interactionMode.setParams(util, this);
		wheel.init();
		ears.init();
		eyes.init();
		// �X�s�[�h���[�^�̃Z�b�g
		wheel.setSpeedMater(speedMater);
		// �Z���T
		sensor.initAccels();
		sensor.incCount();
		for(TouchSensor t : touchSensor)	t.init();
	}

	@Override
	/** ���{�b�g�̑���p�l��������ĕԂ� **/
	public LinearLayout getLayout(Context parent){
        // �e�̃A�N�e�B�r�e�B�ɓ��I���C�A�E�g���쐬����
        layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        // �I�[�g����̃��C�A�E�g��o�^
        modeSelectLayout = getAutoLayout(parent);
        layout.addView(modeSelectLayout);
        
        // �}�j���A������p�l�����쐬
        manualContollerLayout = new LinearLayout(parent);
        manualContollerLayout.setOrientation(LinearLayout.VERTICAL);
        manualContollerLayout.setBackgroundColor(Color.DKGRAY);
		manualContollerLayout.setVisibility(View.GONE);
        // - ����܂̑���p�l����o�^
        manualContollerLayout.addView(wheel.getLayout(parent), LayoutParams.FILL_PARENT);
        // - ���̑���p�l����o�^
        manualContollerLayout.addView(ears.getLayout(parent));
        // - �ڂ̑���p�l����o�^
        manualContollerLayout.addView(eyes.getLayout(parent));

        // emotion�̑���p�l����o�^
        layout.addView(getEmoOperationLayout(parent));
        // �\�����肩��+�ȈՑ���̃��C�A�E�g��o�^
        layout.addView(getShowModeLayout(parent));
        // �}�j���A������p�l����o�^
        layout.addView(manualContollerLayout);

        // trail control�p�l���̓o�^
        trailControllerLayout = sensor.getTrailControllerLayout(parent);
        layout.addView(trailControllerLayout);
        
		// TrailView�ƃZ���T�\�����d�˂ĕ\��
		sensorLayout = new FrameLayout(parent);
        
			// TrailView�\���̃p�l����o�^
	        trailViewLayout = sensor.getTrailViewLayout(parent);
	        sensorLayout.addView(trailViewLayout);
	        
			// �Z���T�[�e�L�X�g�\���̃p�l����o�^
			sensorTextLayout = new LinearLayout(parent);
			sensorTextLayout.setOrientation(LinearLayout.VERTICAL);
			sensorTextLayout.setPadding(10, 5, 10, 0);
			sensorLayout.addView(sensorTextLayout);
			// - �X�s�[�h���[�^�̃p�l����o�^
			sensorTextLayout.addView(speedMater.getLayout(parent));
			// - 9���Z���T�[�̃p�l����o�^
			sensorTextLayout.addView(sensor.getTextLayout(parent));
		
		layout.addView(sensorLayout);
		
		return layout;
	}
	


	/** �\�����肩���{�ȈՑ���p�l���𐶐����ĕԂ� **/
	public LinearLayout getShowModeLayout(Context parent){
		LinearLayout layout = new LinearLayout(parent);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        // manual����p�l���\���؂�ւ�
        CheckBox manualShowCheck = new CheckBox(parent);
        manualShowCheck.setText("controller  ");
        manualShowCheck.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					manualContollerLayout.setVisibility(View.VISIBLE);
				}else{
					manualContollerLayout.setVisibility(View.GONE);
				}
			}
        });
        manualShowCheck.setBackgroundColor(Color.DKGRAY);
        layout.addView(manualShowCheck);
        manualShowCheck.setChecked(false);

		// back�{�^��
		backButton = new Button(parent);
		backButton.setText(BACK_TEXT);
		backButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					wheel.goBackward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		layout.addView(backButton);
		// stop�{�^��
		stopButton = new Button(parent);
		stopButton.setText(STOP_TEXT);
		stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				wheel.stop();
			}
		});
		layout.addView(stopButton);
		// forward�{�^��
		forwardButton = new Button(parent);
		forwardButton.setText(FOR_TEXT);
		forwardButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					wheel.goForward();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		layout.addView(forwardButton);
        
		return layout;
	}

	/** auto����p�l���𐶐����ĕԂ� **/
	public LinearLayout getAutoLayout(Context parent){
		LinearLayout autoLayout = new LinearLayout(parent);
        autoLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
		
        autoLayout.setWeightSum(3);

        // �I�[�g�؂�ւ��̃{�^��
        autoButton = testMode.getOnOffButton(parent);
        autoLayout.addView(autoButton,lp);
        // �I�[�gemotion�؂�ւ��̃{�^��
        autoEmoButton = autoEmoMode.getOnOffButton(parent);
        autoLayout.addView(autoEmoButton,lp);
        // ���茋�ʒ񎦐؂�ւ��̃{�^��
        showInfoButton = showInfoMode.getOnOffButton(parent);
        //autoLayout.addView(showInfoButton,lp);
        // �w�E���[�h�؂�ւ��̃{�^��
        pointOutButton = pointOutMode.getOnOffButton(parent);
        autoLayout.addView(pointOutButton,lp);
        // �C���^���N�V�������[�h�؂�ւ��̃{�^��
        interactionButton = interactionMode.getOnOffButton(parent);
        autoLayout.addView(interactionButton,lp);
        
		return autoLayout;
	}

	/** emotion����p�l���𐶐����ĕԂ� **/
	public LinearLayout getEmoOperationLayout(Context parent){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
		LinearLayout emoLayout = new LinearLayout(parent);
        emoLayout.setOrientation(LinearLayout.HORIZONTAL);
        emoLayout.setWeightSum(4);
        
		emoButton = new Button[4];
		for(int i=0; i<emoButton.length; i++){
			emoButton[i] = new Button(parent);
			emoLayout.addView(emoButton[i], lp);
		}
		emoButton[0].setText("nomal");
        emoButton[0].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				stand();
			}
        });
		emoButton[1].setText("happy");
		emoButton[1].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					happy();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
        });
		emoButton[2].setText("angry");
		emoButton[2].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				angry();
			}
        });
		emoButton[3].setText("sad");
		emoButton[3].setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					sad();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
        });
		for(Button b : emoButton)	util.setEnabled(b, false);
        
		return emoLayout;
	}
	

	@Override
	/** �s�����J���Ċe���[�^�[�ɑΉ������� **/
	public int openPins(IOIO ioio, int startPin) throws ConnectionLostException, InterruptedException{
		int cnt = startPin;
		// 9���Z���T�̓��̓s��(pin1,2)
		try {
			sensor.openPins(ioio, new int[]{sensorPinNum});
		} catch (IOException e) {
			e.printStackTrace();
		}
        // �s���Ƀ��[�^�[��Ή�������
		wheel.openPins(ioio, wheelPinNums);
		ears.openPins(ioio, earsPinNums);
		// �X�s�[�J�[
		//ioio.openPwmOutput(7, 500).setDutyCycle(0.5f);
		// �X�s�[�h���[�^�̓��̓s��
		speedMater.openPins(ioio, speedMaterPinNum);
		// ��
		eyes.openPins(ioio, eyesPinNums);
		// �^�b�`�Z���T
		touchSensor[0].openPins(ioio, touchSensorPinNum[0]);
		touchSensor[1].openPins(ioio, touchSensorPinNum[1]);
		
		return cnt;
	}

	@Override
	/** on�ɂ��� **/
	public void activate() throws ConnectionLostException {
		wheel.activate();
		ears.activate();
		eyes.activate();
		speedMater.activate();
		//sensor.activate();
		for(TouchSensor t : touchSensor)	t.activate();
		for(Button b : emoButton)	util.setEnabled(b, true);
		isActive = true;
	}
	@Override
	/** off�ɂ��� **/
	public void disactivate() throws ConnectionLostException {
		wheel.disactivate();
		ears.disactivate();
		eyes.disactivate();
		speedMater.disactivate();
		sensor.disactivate();
		for(TouchSensor t : touchSensor)	t.disactivate();
		for(Button b : emoButton)	util.setEnabled(b, false);
		isActive = false;
	}
	@Override
	/** �ڑ��������ꂽ�Ƃ��̏��� **/
	public void disconnected() throws ConnectionLostException {
		wheel.disconnected();
		ears.disconnected();
		eyes.disconnected();
		speedMater.disconnected();
		sensor.disconnected();
		for(TouchSensor t : touchSensor)	t.disconnected();
		for(Button b : emoButton)	util.setEnabled(b, false);
		isActive = false;
	}


	/** ���� **/
	public void stand(){
		float[] pink = new float[]{1.0f, 0.4f, 0.4f};
		
		eyes.setColor(pink);

		try {
			ears.backwardSlowly();
			Thread.sleep(1000);
			ears.swing();
			Thread.sleep(1000);
			ears.backwardSlowly();
			Thread.sleep(1000);
			ears.swing();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		eyes.green();
		ears.reset();
		*/
	}
	/** ��� 
	 * @throws InterruptedException **/
	public void happy() throws InterruptedException{
		eyes.green();
		ears.swing();
	}
	/** �{�� **/
	public void angry(){
		eyes.red();
		ears.forward();
	}
	/** �߂��� 
	 * @throws InterruptedException **/
	public void sad() throws InterruptedException{
		eyes.blue();
		ears.backwardSlowly();
	}
	

	/** ��]���X�V�̂Ƃ��̏��� **/
	public void incCount(){
		sensor.incCount();
	}
	public void decCount(){
		sensor.decCount();
	}
	/** �Z���T�[�e�X�^�ɃX�s�[�h���[�^�̒l��`���� **/
	@Override
	public void setSpeed(float speed) {
		sensor.setSpeed(speed);
	}
	
	
	/** �ݒ�̔��f **/
	private void applySettings(){
		// �������䃂�[�h���g����
		usingMode = sharedPreferences.getBoolean("modeSelect", true);
		if(usingMode)	modeSelectLayout.setVisibility(View.VISIBLE);
		else			modeSelectLayout.setVisibility(View.GONE);
		
		// �o�H�����g����
		gettingTrail = sharedPreferences.getBoolean("trailSelect", true);
        if(gettingTrail){
        	trailControllerLayout.setVisibility(View.VISIBLE);
        	sensorLayout.setVisibility(View.VISIBLE);
        }else{
        	trailControllerLayout.setVisibility(View.GONE);
        	sensorLayout.setVisibility(View.GONE);
        }
        
        // ���]�Ői�ދ����ƃX���b�g�̐�
        try{
        	speedMater.setDistPerCycle(sharedPreferences.getInt("distPerCycle", distPerCycle));
        	speedMater.setSlitNum(sharedPreferences.getInt("slitNum", slitNum));
        }catch(ClassCastException e){
        	e.printStackTrace();
        }
	}

	@Override
	public void onResume() {
		applySettings();
		sensor.onResume();
	}
	@Override
	public void onPause() {
		sensor.onPause();
		testMode.onPause();
		pointOutMode.onPause();
		showInfoMode.onPause();
		autoEmoMode.onPause();
		interactionMode.onPause();
	}
}
