import java.util.ArrayList;

public class TempForkData {
	ArrayList<Integer> forkedUTXO;
	int bifurcationID;
	Block forkEnd;

	public TempForkData(ArrayList<Integer> a, Block f, int b) { forkedUTXO = a; bifurcationID = b; forkEnd = f; }
}

