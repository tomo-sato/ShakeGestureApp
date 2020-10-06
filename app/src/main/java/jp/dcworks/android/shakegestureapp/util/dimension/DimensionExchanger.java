package jp.dcworks.android.shakegestureapp.util.dimension;

import jp.dcworks.android.shakegestureapp.util.model.Acceleration3dPoint;

/**
 * ３つの点を平面化
 * @author tomomichi
 */
public interface DimensionExchanger {
	public double[] exchange(final Acceleration3dPoint[] points);
}
