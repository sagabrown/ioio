package ioio.robot.sensor;

public class PoseAnalizer {
	protected final static float DEG2RAD = (float)(Math.PI/180.0);
	
	public static final int NONE = 0;

	public static final int BACK_STAND = 100;
	public static final int BACK_FOR1 = 110;
	public static final int BACK_FOR2 = 111;
	public static final int BACK_BACK1 = 120;
	public static final int BACK_BACK2 = 121;
	
	public static final int ARM_STAND = 200;
	public static final int ARM_UP1 = 210;
	public static final int ARM_UP2 = 211;
	public static final int ARM_DOWN1 = 220;
	public static final int ARM_DOWN2 = 221;
	
	public static final float THRESHOLD_BACK1 = 15 * DEG2RAD;
	public static final float THRESHOLD_BACK2 = 70 * DEG2RAD;
	public static final float THRESHOLD_ARM1 = 30 * DEG2RAD;
	public static final float THRESHOLD_ARM2 = 60 * DEG2RAD;

	public int poseAnalize(int pointType, float dif){
    	switch(pointType){
    	case TrailPoint.BACK:
    	case TrailPoint.SHOLDER:
    		if(dif < -THRESHOLD_BACK2)			return BACK_FOR2;
    		else if(dif < -THRESHOLD_BACK1)		return BACK_FOR1;
    		else if(dif > THRESHOLD_BACK2)		return BACK_BACK2;
    		else if(dif > THRESHOLD_BACK1)		return BACK_BACK1;
    		else								return BACK_STAND;
    	case TrailPoint.ARM:
    		if(dif < -THRESHOLD_ARM2)			return ARM_DOWN2;
    		else if(dif < -THRESHOLD_ARM1)		return ARM_DOWN1;
    		else if(dif > THRESHOLD_ARM2)		return ARM_UP2;
    		else if(dif > THRESHOLD_ARM1)		return ARM_UP1;
    		else								return ARM_STAND;
    	default:					    		return NONE;
    	}
	}
	
	public String getPoseInfo(int pointType, float dif){
		int result = poseAnalize(pointType, dif);
		switch(result){
		case BACK_STAND:	return "直立";
		case BACK_FOR1:		return "前傾姿勢";
		case BACK_FOR2:		return "うつぶせ";
		case BACK_BACK1:	return "後傾姿勢";
		case BACK_BACK2:	return "仰向け";
		case ARM_STAND:		return "ふつう";
		case ARM_UP1:		return "腕を上げぎみ";
		case ARM_UP2:		return "腕を上げている";
		case ARM_DOWN1:		return "腕を下げぎみ";
		case ARM_DOWN2:		return "腕を下げている";
		default:			return "--";
		}
	}
}
