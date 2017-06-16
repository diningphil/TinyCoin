
public class Transaction {
	
	public long transID;
	public int bitcoins;
	//public long address;
	public long srcAddress, destAddress;
	
	//public Transaction(long id, int b, long addr) {
	public Transaction(long id, int b, long src, long dest) {
		transID = id;
		bitcoins = b;
		//address = addr;
		srcAddress = src;
		destAddress = dest;
	}
}
