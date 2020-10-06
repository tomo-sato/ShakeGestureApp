package jp.dcworks.android.shakegestureapp.util.model;

public final class Acceleration3dPoint {
	private double x = 0.0;
	private double y = 0.0;
	private double z = 0.0;
	
	public Acceleration3dPoint(double x, double y, double z){
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}
	
	
}
