import java.util.ArrayList;
import java.util.Iterator;

public class Block {
	
	public Block(int minerID, int prevID) {
		this.minerID = minerID;
		prevBlockID = prevID;
		blockID = -1;
		extraReward = 0;
		extraLatency = 0;
		transactions = new ArrayList<>();
		height = 0;
	}
	
	public int blockID, prevBlockID;
	public int minerID;
	public int extraReward, extraLatency;
	public int height;
	public ArrayList<Transaction> transactions;
	public void addTransaction(Transaction t) {
	
		transactions.add(t);
		extraReward += SharedInfo.addRewardPerTransaction;
		extraLatency += SharedInfo.addLatencyPerTransaction;
		
	}
	public String toJSON() {
		return "{ "
				+ "blockID:" + blockID + ","
				+ "prevBlockID:" + prevBlockID + ","
				+ "minerID:" + minerID + ","
				+ "height:" + height + ","
				+ "extraReward:" + extraReward + ","
				+ "extraLatency:" + extraLatency + "," 
				+ "transactions:" + transactionsToJSON()
				+ "}";
	}
	private String transactionsToJSON() {
		String res = "[";
		Iterator<Transaction> it = transactions.iterator();
		while(it.hasNext()) {
			res += it.next().toJSON();
			if(it.hasNext()) res += ",";
		}		
		return res + "]";
	}

	@Override
	public Object clone() {
		Block b = new Block(minerID, prevBlockID);
		b.extraReward = extraReward;
		b.extraLatency = extraLatency;
		b.height = height;
		b.transactions = (ArrayList<Transaction>) transactions.clone();

		return b;
	}
}
