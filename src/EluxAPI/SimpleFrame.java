package EluxAPI;

public class SimpleFrame {
	private double X, Y, Z, A, B, C;
	
	public SimpleFrame(double x, double y, double z, double a, double b, double c) {
		this.build(x, y, z, a, b, c);
	}
	
	public SimpleFrame(double x, double y, double z) {
		this.build(x,y,z);
	}
	
	public SimpleFrame() {
		this.build(0,0,0,0,0,0);
	}
	
	public void build(double x, double y, double z, double a, double b, double c) {
		X = x; Y = y; Z = z; A = a; B = b; C = c; }
	
	public void build(double x, double y, double z) { this.build(x, y, z,0,0,0); }
	
	public SimpleFrame getFrame() { return this; }
	
	public double[] toArray() {
		double[] trafo = {X,Y,Z,A,B,C};
		return trafo;
	}
	public String toString() {
		String frame = "{"+X+","+Y+","+Z+","+A+","+B+","+C+"}";
		return frame;
	}
	
	public double getX() { return X; }
	public double getY() { return Y; }
	public double getZ() { return Z; }
	public double getA() { return A; }
	public double getB() { return B; }
	public double getC() { return C; }
}