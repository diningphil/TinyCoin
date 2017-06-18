
public class TinyCoinMessage {
	public int type;
	public Object message;
	public long sender;

	public static final int MINED       = 0;
    public static final int TRANSACTION = 1;
    public static final int BLOCK       = 2;
    	
	public TinyCoinMessage(int t, Object o, long senderID) {
		sender = senderID;
		type = t;
		message = o;
	}
}
