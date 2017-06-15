import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.vector.SingleValueHolder;

public class TinyNode extends SingleValueHolder implements EDProtocol {

	
	// TODO un'istanza di questo protocollo è associata ad ogni Node.
	// puoi sfruttarlo per mettere info del singolo nodo qui, come il tipo ad esempio
	// così eviti di usare alcune strutture dati in SharedInfo
	// Vedi Utils e TrafficGenerator in ChordSim
	
	public TinyNode(String prefix) { super(prefix); }
	// Normal node behaviour
	@Override
	public void processEvent(Node node, int pid, Object event) {

		System.out.println("Received message from " + node.getID());
		
	}

	public Object clone() { return null; }
}
