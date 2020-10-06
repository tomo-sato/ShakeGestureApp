package jp.dcworks.android.shakegestureapp.util.arrayFilter;

public interface ArrayFilter {
	public boolean validation(final Object[] target);
	public Object[] convert(final Object[] target);
}
