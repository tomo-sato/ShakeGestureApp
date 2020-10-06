package jp.dcworks.android.shakegestureapp.util.interpolation;

import jp.dcworks.android.shakegestureapp.util.model.MatchingArray;

/**
 * 元データを最小にする。
 * 	結果は結構取れそう？
 * @author t.sato
 *
 */
public final class FixInterpolation implements Interpolation {

	@Override
	public MatchingArray calcurate(final MatchingArray array) {
		int lenA = array.getArrayA().length;
		int lenB = array.getArrayB().length;
		int maxLen = Math.min(lenA, lenB);
		MatchingArray tmpArr = new MatchingArray(new Object[maxLen], new Object[maxLen]);
		for(int idx = 0; idx < maxLen ; idx++){
			tmpArr.getArrayA()[idx] = array.getArrayA()[idx];
			tmpArr.getArrayB()[idx] = array.getArrayB()[idx];
		}
		return tmpArr;
	}

	@Override
	public boolean validation(final MatchingArray array) {
		return !(array.getArrayA().length == 0 || array.getArrayB().length == 0);
	}
}
