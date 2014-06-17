package ioio.robot.region.crawl.sensor;

import java.util.ArrayList;
import java.util.List;

public class PointTypeAnalyzer {
	public static final int HALF_SHOLDER_WIDTH = 2;
	public static final int SMOOTH_WIDTH = 2;

	public void pointAnalyze(ArrayList<TrailPoint> tpList){
		smoothing(tpList);

    	int len = tpList.size();
    	float max = 0;
    	int maxPointIndex = 0;
		synchronized(tpList){
			for(TrailPoint tp : tpList){
				if(tp.z > max){
					max = tp.z;
					maxPointIndex = tpList.indexOf(tp);
				}
			}
			int i = 0;
			for(; i<5 && i<len; i++)	tpList.get(i).type = TrailPoint.LEG;
			for(; i<maxPointIndex-HALF_SHOLDER_WIDTH && i<len; i++)	tpList.get(i).type = TrailPoint.BACK;
			for(; i<maxPointIndex+HALF_SHOLDER_WIDTH && i<len; i++)	tpList.get(i).type = TrailPoint.SHOLDER;
			for(; i<len; i++)	tpList.get(i).type = TrailPoint.ARM;
		}
	}
	
	public void smoothing(List<TrailPoint> tpList){
		int len = tpList.size();
		ArrayList<TrailPoint> newList = new ArrayList<TrailPoint>();
		for(int i=0; i<len; i++){
			float[] sum = new float[9];
			int cnt = 0;
			for(int j=-SMOOTH_WIDTH; j<=SMOOTH_WIDTH; j++){
				if(i+j < 0 || i+j >= len)	continue;
				sum[0] += tpList.get(i+j).xr;
				sum[1] += tpList.get(i+j).yr;
				sum[2] += tpList.get(i+j).zr;
				sum[3] += tpList.get(i+j).xl;
				sum[4] += tpList.get(i+j).yl;
				sum[5] += tpList.get(i+j).zl;
				sum[6] += tpList.get(i+j).azimuth;
				sum[7] += tpList.get(i+j).pitch;
				sum[8] += tpList.get(i+j).roll;
				cnt++;
			}
			newList.add(new TrailPoint(sum[0]/cnt, sum[1]/cnt, sum[2]/cnt, sum[3]/cnt, 
					sum[4]/cnt, sum[5]/cnt, sum[6]/cnt, sum[7]/cnt, sum[8]/cnt));
			
		}

		synchronized(tpList){
			for(int i=0; i<len; i++){
				tpList.clear();
				tpList.addAll(newList);
			}
		}
	}
}
