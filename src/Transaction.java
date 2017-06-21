
public class Transaction implements Comparable<Transaction>{
	
	public int transID;
	public int bitcoins;
	//public long address;
	public int srcAddress;
	public int destAddress;

	
	//public Transaction(long id, int b, long addr) {
	public Transaction(int id, int b, int src, int dest) {
		transID = id;
		bitcoins = b;
		//address = addr;
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
	public int compareTo(Transaction t) {
		return (transID - t.transID);
	}
	
}
