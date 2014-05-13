import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Hashtable;


public interface RemoteInterface extends Remote{
	
	public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID);
	void remoteInitiateElection();
	void remoteBroadcastNewNodeInfo(String IP,int port, int nodeID);
	void remoteBroadcastCoordinatorNodeID(int port);
}
