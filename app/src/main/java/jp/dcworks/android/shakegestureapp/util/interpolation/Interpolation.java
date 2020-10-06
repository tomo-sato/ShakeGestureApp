package jp.dcworks.android.shakegestureapp.util.interpolation;

import jp.dcworks.android.shakegestureapp.util.model.MatchingArray;

/**
 * 配列の保管の仕方
 * @author tomomichi
 *
 */
public interface Interpolation {
	public boolean validation(final MatchingArray array);
	public MatchingArray calcurate(final MatchingArray array);
}
