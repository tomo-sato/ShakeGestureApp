package jp.dcworks.android.shakegestureapp.util.interpolation;

import jp.dcworks.android.shakegestureapp.util.model.MatchingArray;

/**
 *
 * @author tomomichi
 *
 */
public final class CopyInterpolation implements Interpolation {

	@Override
	public MatchingArray calcurate(final MatchingArray array) {
		int lenA = array.getArrayA().length;
		int lenB = array.getArrayB().length;
		int diff   = Math.abs(lenA - lenB);

		boolean aIsMaxLen = (lenA > lenB);
		int maxLen = aIsMaxLen ? lenA: lenB;
		Object[] longer  = aIsMaxLen ? array.getArrayA() : array.getArrayB();
		Object[] shorter = aIsMaxLen ? array.getArrayB() : array.getArrayA();
		int step = (longer.length / diff);

		MatchingArray tmpArr = new MatchingArray(new Object[maxLen], new Object[maxLen]);
		int shortIdx = 0, addCnt = 0;
		for(int idx = 0; idx < maxLen ; idx++){
			tmpArr.getArrayA()[idx] = longer[idx];
			addCnt   = (addCnt == step) ? 0 : addCnt + 1;
			shortIdx += (addCnt == step) ? 0 : 1;
			int curIdx = shortIdx - 1;
			tmpArr.getArrayB()[idx] = shorter[ (curIdx >= shorter.length) ? shorter.length - 1 : curIdx ];
		}
		return tmpArr;
	}

	@Override
	public boolean validation(final MatchingArray array) {
		return !(array.getArrayA().length == 0 || array.getArrayB().length == 0);
	}

}
