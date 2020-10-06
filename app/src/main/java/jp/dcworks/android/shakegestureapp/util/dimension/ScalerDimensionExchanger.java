package jp.dcworks.android.shakegestureapp.util.dimension;

import jp.dcworks.android.shakegestureapp.util.model.Acceleration3dPoint;

public final class ScalerDimensionExchanger implements DimensionExchanger {

	@Override
	public double[] exchange(final Acceleration3dPoint[] points) {
		if(points.length < 2){
			return new double[]{};//要素長が2未満であれば要素なし配列を戻す
		}
		int idx = 0;
		Acceleration3dPoint befPnt = null;
		double[] retArr = new double[points.length - 1];//最初の一個目を飛ばすため、-1する
		for(Acceleration3dPoint curPnt : points){
			if(befPnt == null){
				befPnt = curPnt;//最初の1要素
			}else{
				//3次元上の点同士の距離：√{(x-x1)^2 + (y-y1)^2 + (z-z1)^2 }
				double xdiff = Math.pow(befPnt.getX() - curPnt.getX(), 2.0);
				double ydiff = Math.pow(befPnt.getY() - curPnt.getY(), 2.0);
				double zdiff = Math.pow(befPnt.getZ() - curPnt.getZ(), 2.0);
				retArr[idx] = Math.sqrt(xdiff + ydiff + zdiff);
				idx++;
			}
		}
		return retArr;
	}
}
