import java.util.ArrayList;
import java.util.Iterator;

public class Block {
	
	public Block(int minerID, long prevID) {
		this.minerID = minerID;
		prevBlockID = prevID;
		blockID = -1;
		extraReward = 0;
		extraLatency = 0;	
		
		//inputTransactions = new HashMap<>();
		//outputTransactions = new HashMap<>();	
		transactions = new ArrayList<>();	
		
		height = 0;
	}
	
	public long blockID, prevBlockID;
	public int minerID;
	public int extraReward, extraLatency;
	public int height;
	
	//public HashMap<Long,Transaction> inputTransactions;
	//public HashMap<Long,Transaction> outputTransactions;
	public ArrayList<Transaction> transactions;
	public void addTransaction(Transaction t) {
		
		// This would better resembles the actual implementation, but there are problems
		// When checking if an input transaction exists ( because of assumptions of single input/output)
		//Transaction input = new Transaction(transactionID, amount, src);
		//Transaction output = new Transaction(transactionID, amount, dest);
		//inputTransactions.put(transactionID, input);
		//outputTransactions.put(transactionID, output);		
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
}
