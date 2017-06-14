import java.util.HashMap;

public class Block {
	public int ID, prevID;
	public int minerID;
	public int reward, extraReward;
	public HashMap<Long,Transaction> transactions;
	public boolean confirmed;
}
