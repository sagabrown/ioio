package ioio.examples.test;

public interface Motor {
	public void setSpec();
	public double getDuty(double theta);
	public double getDuty2(double ratio);
	public double getInitDuty();
	public int getFreq();
	public double getMinTheta();
	public double getMaxTheta();
	public void setTheta0(double theta0);
}
