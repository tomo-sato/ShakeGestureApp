package jp.dcworks.android.shakegestureapp.util.arrayFilter;

import jp.dcworks.android.shakegestureapp.util.common.CommonInfo;

public class LaplacianArrayFilter implements ArrayFilter {
	private static double[] filterBase= {-1.0, 2, -1.0};
	
	@Override
	public Object[] convert(final Object[] target) {
		for(int idx = 1; idx < target.length - 1; idx++){
			target[idx-1] = (Double)target[idx-1] * filterBase[0];
			target[idx] = (Double)target[idx] * filterBase[1];
			target[idx+1] = (Double)target[idx+1] * filterBase[2];
		}
		return target;
	}

	@Override
	public boolean validation(final Object[] target) {
		if(target.length > filterBase.length){
			return false;
		}
		if(!CommonInfo.getInstance().isNumber(target[0])){
			return false;
		}
		return true;
	}

}
