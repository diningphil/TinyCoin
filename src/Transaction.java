
public class Transaction implements Comparable<Transaction>{
	
	public int transID;
	public int bitcoins;
	public int srcAddress;
	public int destAddress;

	
	public Transaction(int id, int b, int src, int dest) {
		transID = id;
		bitcoins = b;
		srcAddress = src;
		destAddress = dest;
	}
	
	public String toJSON() {
		return "{ "
				+ "transID:" + transID + ","
				+ "bitcoins:" + bitcoins + ","
				+ "srcAddress:" + srcAddress + ","
				+ "destAddress:" + destAddress
				+ "}";
	}

	@Override
	public Object clone() {
		return new Transaction(transID, bitcoins, srcAddress, destAddress);
	}

	@Override
	public int compareTo(Transaction t) {
		return (transID - t.transID);
	}
	
}
