package ioio.robot.sensor;

public class TrailPoint {
	public float x, y, z;
	public float xr, yr, zr;
	public float xl, yl, zl;
	
	public TrailPoint(float xr, float yr, float zr, float xl, float yl, float zl) {
		this.xr = xr;
		this.yr = yr;
		this.zr = zr;
		this.xl = xl;
		this.yl = yl;
		this.zl = zl;
		x = (float)0.5 * (xr+xl);
		y = (float)0.5 * (yr+yl);
		z = (float)0.5 * (zr+zl);
	}

}
