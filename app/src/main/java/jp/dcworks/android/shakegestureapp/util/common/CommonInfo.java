package jp.dcworks.android.shakegestureapp.util.common;

/**
 * singletone
 * @author lportal
 *
 */
public final class CommonInfo {
	private static CommonInfo _instance = new CommonInfo();
	
	public static CommonInfo getInstance(){
		return CommonInfo._instance;
	}
	
	public boolean isNumber(final Object a, final Object b){
		return isNumber(a) && isNumber(b);
	}
	
	public boolean isNumber(final Object a){
		return (a instanceof Integer) || (a instanceof Float)
				|| (a instanceof Double) || (a instanceof Long);
	}
	
}
