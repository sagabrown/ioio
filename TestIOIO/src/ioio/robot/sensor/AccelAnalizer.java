package ioio.robot.sensor;

public class AccelAnalizer {
	public static final float HZ_TO_KMperH = 3600f * 0.001f;	// 1•à1m‚Æ‚µ‚½‚Æ‚«‚Ì•ÏŠ·Ž®
	
	public static final int NONE = 0;
	
	public static final int STANDING = 101;
	public static final int WALKING = 102;
	public static final int RUNNING = 103;

	public static final int ARM_STOPPING = 200;
	public static final int ARM_SHAKING = 201;
	public static final int ARM_SHAKING_FAST = 202;

	public static final float RANGE_TH_LOW = 5000f;
	public static final float VAL_TH_LOW = 0.1f;
	
	public static final float WALKING_TH_LOW = 0.5f;
	public static final float WALKING_TH_HIGH = 1.2f;
	public static final float RUNNING_TH_LOW = 1.2f;
	public static final float RUNNING_TH_HIGH = 5.0f;
	
	public static final float ARM_SHAKING_TH_LOW = 0.5f;
	public static final float ARM_SHAKING_FAST_TH_LOW = 1.0f;
	
	
	public int walkingAnalize(float peak, float peakVal, float range){
		if(range < RANGE_TH_LOW){
			return STANDING;
		}else if(VAL_TH_LOW < peakVal){
			if( WALKING_TH_LOW < peak && peak < WALKING_TH_HIGH ){
				return WALKING;
			}else if( RUNNING_TH_LOW < peak && peak < RUNNING_TH_HIGH ){
				return RUNNING;
			}else{
				return NONE;
			}
		}else{
			return NONE;
		}
	}
	
	public int armShakingAnalize(float peak, float peakVal, float range){
		if(range < RANGE_TH_LOW){
			return ARM_STOPPING;
		}else if(VAL_TH_LOW < peakVal){
			if( peak < ARM_SHAKING_TH_LOW ){
				return ARM_STOPPING;
			}else if( ARM_SHAKING_TH_LOW < peak && peak < ARM_SHAKING_FAST_TH_LOW ){
				return ARM_SHAKING;
			}else{
				return ARM_SHAKING_FAST;
			}
		}else{
			return NONE;
		}
	}
	
	public String getAccelInfo(int pointType, float peak, float peakVal, float range){
		int result;
		
		switch(pointType){
		case TrailPoint.BACK:
		case TrailPoint.SHOLDER:
			result = walkingAnalize(peak, peakVal, range);
			break;
		case TrailPoint.ARM:
			result = armShakingAnalize(peak, peakVal, range);
			break;
		default:
			result = NONE;
		}
		
		switch(result){
		case STANDING:		return "’¼—§";
		case WALKING:		return "•à‚¢‚Ä‚¢‚é... "+peak*HZ_TO_KMperH+" km/h";
		case RUNNING:		return "‘–‚Á‚Ä‚¢‚é... "+peak*HZ_TO_KMperH+" km/h";
		case ARM_STOPPING:	return "ÃŽ~";
		case ARM_SHAKING:	return "˜r‚ðU‚Á‚Ä‚¢‚é... "+peak+" ‰ñ/s";
		case ARM_SHAKING_FAST:		return "˜r‚ð‘f‘‚­U‚Á‚Ä‚¢‚é... "+peak+" ‰ñ/s";
		default:			return "--";
		}
	}

}
