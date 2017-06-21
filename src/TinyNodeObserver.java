import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;


public class TinyNodeObserver implements Control
{

	private final String PAR_PROTID = "protocol";
	private final int pid;
	
	public TinyNodeObserver(String name)
	{
		pid = Configuration.getPid(name+"."+PAR_PROTID);
	}
	
	
	/**
	 * Computes the network average, and the standard deviation.
	 * Terminates when the standard deviation goes below a threshold.
	 */
	@Override
	public boolean execute() 
	{	
		
		/** DEBUGGING
		Node node15 = Network.get(15);
		Node node381 = Network.get(381); // MINER THAT BROADCASTS 100
		
		TinyNode protocol15 = (TinyNode) node15.getProtocol(pid);
		TinyNode protocol223 = (TinyNode) node381.getProtocol(pid);
		
		String blockchain15 = protocol15.localBlockchain.longestPathToJSON(protocol15.localBlockchain.head);
		String blockchain223 = protocol223.localBlockchain.longestPathToJSON(protocol223.localBlockchain.head);
		
		System.err.println("15 head ID is " + protocol15.localBlockchain.head.blockID);
		System.err.println("381 head ID is " + protocol223.localBlockchain.head.blockID);
		
		if(blockchain15.compareTo(blockchain223) == 0) {
			System.err.println("Blockchain are equals...");
		
			if(protocol15.localBlockchain.UTXO.equals(protocol223.localBlockchain.UTXO))
				System.err.println("So are UTXO...");
			else	
				System.err.println("UTXO ARE NOT!");
		} else {
			System.err.println("Blockchain are different...");
			
			System.err.println("15 is \n " + protocol15.localBlockchain.longestPathToJSON(protocol15.localBlockchain.head));
			for(int i = 0; i < protocol15.localBlockchain.UTXO.size(); i++)
				System.out.print(protocol15.localBlockchain.UTXO.get(i) + " ");
			System.out.println();
			
			System.err.println("223 is \n " + protocol223.localBlockchain.longestPathToJSON(protocol223.localBlockchain.head));
			for(int i = 0; i < protocol223.localBlockchain.UTXO.size(); i++)
				System.out.print(protocol223.localBlockchain.UTXO.get(i) + " ");
			System.out.println();
			
		}
		*/
				
		return false;
	}

}