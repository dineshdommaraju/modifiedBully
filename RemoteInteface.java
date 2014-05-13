import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Hashtable;


public interface RemoteInterface extends Remote{
	
	public HashMap<Integer,String> remoteInsertNode(String IP,Integer port, Integer nodeID);
	void remoteInitiateElection();
	void remoteBroadcastNewNodeID();
	void remoteBroadcastCoordinatorNodeID();
}
