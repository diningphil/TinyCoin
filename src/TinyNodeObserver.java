import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;


public class TinyNodeObserver implements Control
{

	private final String PAR_PROTID = "protocol";
	private final int pid;

	private int minLength,maxLength;
	private float avgLength, size, forkCounter, blockchainHeight, rewardedSelfishBlocks;

	public TinyNodeObserver(String name)
	{
		size = Network.size();
		minLength = 1; maxLength = 0;
		forkCounter = 0; avgLength = 0;
		rewardedSelfishBlocks = 0;
		blockchainHeight = 0;
		pid = Configuration.getPid(name+"."+PAR_PROTID);
	}

	/**
	 * Computes the network average, and the standard deviation.
	 * Terminates when the standard deviation goes below a threshold.
	 */
	@Override
	public boolean execute() 
	{
		/**
		 *  Ho bisogno di :
		 * 	- numero di fork di un nodo (a convergenza dovrebbero essere le stesse per tutti)
		 *  - percentuale di "selfish blocks" nella blockchain (guardando la catena più lunga ovviamente)
		 *  - TODO percentuale di fork vinte da selfish miners
		 *         (guardo i blocchi non nella catena principale con prevID corrispodente a punti di biforcazione, e ispeziono minerID)
		 *  - il tempo necessario per risolvere le fork (max, min, avg)
		 */
		System.err.println("Observer started...");
		for(int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			TinyNode protocol = (TinyNode) node.getProtocol(pid);
			blockchainHeight += protocol.publicBlockchain.head.height;
			computeBlockchainStatistics(protocol.publicBlockchain);
			System.out.println("Node " + node.getID() + " public forks:" + protocol.publicBlockchain.numberOfForks + " private forks:" + protocol.privateBlockchain.numberOfForks);
		}

		// Selfish
		TinyNode protocol0 = (TinyNode) Network.get(0).getProtocol(pid);
		System.out.println((protocol0.publicBlockchain.longestPathToJSON(protocol0.publicBlockchain.head)));
		System.out.println((protocol0.privateBlockchain.longestPathToJSON(protocol0.privateBlockchain.head)));

		// Another node
		TinyNode protocol300 = (TinyNode) Network.get(300).getProtocol(pid);
		System.out.println((protocol300.publicBlockchain.longestPathToJSON(protocol300.publicBlockchain.head)));
		System.out.println((protocol300.privateBlockchain.longestPathToJSON(protocol300.privateBlockchain.head)));


		avgLength /= forkCounter;
		blockchainHeight /= size;

		System.out.println("Min fork length: " + minLength + " Max fork length: " + maxLength +
				" Avg fork length: " + avgLength + " public blockchain height: " + blockchainHeight + " rewarded blocks belonging to selfish miners: " + rewardedSelfishBlocks);

		return false;
	}

	/**
	* Per ogni blocco non appartenente alla longest chain
	* o non visto dall'algoritmo (partendo da ID più alti) , cerca il più vicino
	* punto di biforcazione, e assegna la max lunghezza del ramo forked che ha perso
	**/
	public void computeBlockchainStatistics(CachedBlockchain blockchain) {

		SharedInfo sI = SharedInfo.getSharedInfo();

		// Exploit the fact that block ids are unique
		HashSet<Integer> seenIds = new HashSet<>();
		TreeSet<Integer> allIds = blockchain.getBlockIDs();

		Block curr = blockchain.head;
		seenIds.add(curr.blockID);

		// Registra i nodi della longest chain
		while(curr.blockID != -1) {
			seenIds.add(curr.blockID);
			if(sI.selfish.contains(curr.minerID)) {
				rewardedSelfishBlocks += 1.0 / size; // Average over the nodes
			}
			curr = blockchain.getBlockWithID(curr.prevBlockID);
		}

		seenIds.add(-1);

		Iterator descendingIt = allIds.descendingIterator(); // Guardo i blocchi a partire dall' ID più alto (di quelli che non appartengono alla longest chain)
		while(descendingIt.hasNext()) {
			int id = (int) descendingIt.next();


			if(!seenIds.contains(id)) { // It belongs to a fork
				int length = 0;
				Block forkEnd = blockchain.getBlockWithID(id);
				while(!seenIds.contains(forkEnd.blockID)) {
					length++;
					seenIds.add(forkEnd.blockID);
					forkEnd = blockchain.getBlockWithID(forkEnd.prevBlockID);
				}
				// A bifurcation point has been reached. Update stats
				if(maxLength < length ) maxLength = length;
				if(minLength > length ) minLength = length;
				avgLength += ((float)length) ;
				forkCounter++;
			}
		}
	}

}