package ioio.robot.sensor;

public class TrailPoint {
	public float x, y, z;
	public float xr, yr, zr;
	public float xl, yl, zl;
	public float azimuth, pitch, roll;
	public int type;

	public static final int NO_TYPE = 0;
	public static final int SHOLDER = 1;
	public static final int BACK = 2;
	public static final int ARM = 3;
	
	public TrailPoint(float xr, float yr, float zr, float xl, float yl, float zl, float azimuth, float pitch, float roll) {
		this.xr = xr;
		this.yr = yr;
		this.zr = zr;
		this.xl = xl;
		this.yl = yl;
		this.zl = zl;
		x = (float)0.5 * (xr+xl);
		y = (float)0.5 * (yr+yl);
		z = (float)0.5 * (zr+zl);
		this.azimuth = azimuth;
		this.pitch = pitch;
		this.roll = roll;
		type = NO_TYPE;
	}

}
