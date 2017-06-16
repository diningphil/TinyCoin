import java.util.HashMap;

public class Block {
	
	public Block(long minerID, long prevID) {
		this.minerID = minerID;
		prevBlockID = prevID;
		blockID = SharedInfo.getNextBlockID();
		reward = SharedInfo.blockReward;
		extraReward = 0;
		extraLatency = 0;
		confirmed = false;
		//inputTransactions = new HashMap<>();
		//outputTransactions = new HashMap<>();	
		transactions = new HashMap<>();	
		
		height = -1;
	}
	
	public long blockID, prevBlockID;
	public long minerID;
	public int reward, extraReward, extraLatency;
	public int height;
	
	//public HashMap<Long,Transaction> inputTransactions;
	//public HashMap<Long,Transaction> outputTransactions;
	public HashMap<Long,Transaction> transactions;
	
	public boolean confirmed;
	
	public void addTransaction(long transactionID, int amount, long src, long dest) {
		
		// This would better resembles the actual implementation, but there are problems
		// When checking if an input transaction exists ( because of assumptions of single input/output)
		//Transaction input = new Transaction(transactionID, amount, src);
		//Transaction output = new Transaction(transactionID, amount, dest);
		//inputTransactions.put(transactionID, input);
		//outputTransactions.put(transactionID, output);
				
		Transaction t = new Transaction(transactionID, amount, src, dest);
		transactions.put(transactionID, t);
		
		extraReward += SharedInfo.addRewardPerTransaction;
		extraLatency += SharedInfo.addLatencyPerTransaction;
		
	}
}
