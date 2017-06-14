import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


// Singleton class to keep information about nodes, their computational power and other things
// Set up by the initializer

public class SharedInfo {
    private static SharedInfo instance = null;

    public static final int MINED       = 0;
    public static final int TRANSACTION = 1;
    public static final int BLOCK       = 2;
 
    public static final int blockReward = 10;
    public static final int addLatencyPerTransaction = 1;
    public static final int addRewardPerTransaction = 1;
    
    // Gaussian distribution for the oracle
    public static final int oracleMean = 5, oracleVariance = 3;
    // Probability to CREATE one type of miner
    public static final int cpu = 25, gpu = 50, fpga = 75, asic = 100;     
    // Probability to CHOOSE one type of miner
    public static final int cpuPower = 5, gpuPower = 15, fpgaPower = 45, asicPower = 100;
 
    //----------------------------------------------------------------------------------//
    
    public HashMap<Integer, Integer> miners; // nodeId --> cpu, gpu, fpga, asic;
    public List<Integer> cpus;
    public List<Integer> gpus;
    public List<Integer> fpgas;
    public List<Integer> asics;
    
    private SharedInfo() {
    	miners = new HashMap<>();
    	cpus  = new ArrayList<Integer>();
    	gpus  = new ArrayList<Integer>();
    	fpgas  = new ArrayList<Integer>();
    	asics  = new ArrayList<Integer>();
    }

    public static synchronized SharedInfo getSharedInfo() {
        if (instance == null) {
            instance = new SharedInfo();
        }
        return instance;
    }
}