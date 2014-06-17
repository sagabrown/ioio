package ioio.robot.sensor;

import java.util.ArrayList;

public class SensorLogManager {
	private int logSize;
	private ArrayList<float[]> logs;
	
	
	public SensorLogManager(int logSize) {
		this.logSize = logSize;
		logs = new ArrayList<float[]>(logSize);
	}

	public void addSensorLog(float[] log){
		logs.remove(0);
		logs.add(log);
	}
	
	
	// ã}â¡ë¨ÇµÇΩÇ©ÅH
	float threshold = 10;
	public boolean isAccelarated(int length){
		ArrayList<float[]> subList = (ArrayList<float[]>) logs.subList(logSize-1-length, logSize-1);
		float[] average;
		average = average(subList);
		return false;
	}
	
	private float[] average(ArrayList<float[]> list){
		float sum[] = {0f, 0f, 0f};
		for(float[] log : list){
			for(int i=0; i<3; i++){
				sum[i] += log[i];
			}
		}
		for(int i=0; i<3; i++){
			sum[i] /= list.size();
		}
		return sum;
	}
}
