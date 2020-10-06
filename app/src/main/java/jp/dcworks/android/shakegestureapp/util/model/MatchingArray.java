package jp.dcworks.android.shakegestureapp.util.model;

public final class MatchingArray {
	private Object[] arrayA;
	private Object[] arrayB;

	public MatchingArray(){
		this.arrayA = new Object[]{};
		this.arrayB = new Object[]{};
	}
	
	public MatchingArray(final Object[] arrayA, final Object[] arrayB){
		this.arrayA = arrayA;
		this.arrayB = arrayB;
	}

	public Object[] getArrayA() {
		return arrayA;
	}

	public void setArrayA(Object[] arrayA) {
		this.arrayA = arrayA;
	}

	public Object[] getArrayB() {
		return arrayB;
	}

	public void setArrayB(Object[] arrayB) {
		this.arrayB = arrayB;
	}
	
}
