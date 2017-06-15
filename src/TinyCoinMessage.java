
public class TinyCoinMessage {
	public int type;
	public Object message;

	public static final int MINED       = 0;
    public static final int TRANSACTION = 1;
    public static final int BLOCK       = 2;
    	
	public TinyCoinMessage(int t, Object o) {
		type = t;
		message = o;
	}
}
