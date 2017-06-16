
public class Transaction {
	
	public long transID;
	public int bitcoins;
	//public long address;
	public int srcAddress;
	public int destAddress;
	public boolean confirmed;

	
	//public Transaction(long id, int b, long addr) {
	public Transaction(long id, int b, int src, int dest) {
		transID = id;
		bitcoins = b;
		//address = addr;
		srcAddress = src;
		destAddress = dest;
		confirmed = false;
	}
}
