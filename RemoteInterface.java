
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;


public interface RemoteInterface extends Remote{
	
	public boolean remoteAccess(int nodeID) throws RemoteException
	public int remoteLeave(int nodeID) throws RemoteException;
	public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException, NotBoundException;
	void remoteInitiateElection() throws RemoteException;
	void remoteBroadcastNewNodeInfo(String IP, int port, int nodeID) throws RemoteException;
	void remoteBroadcastCoordinatorNodeID(int nodeID) throws RemoteException;
	public int getCoordinatorID();
	public HashMap<Integer, String> remoteInsertNode(int nodeID, String nodeIP);
}


