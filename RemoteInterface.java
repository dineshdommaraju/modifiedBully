
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;


public interface RemoteInterface extends Remote{
	
	public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException, NotBoundException;
	void remoteInitiateElection(int nodeId) throws RemoteException;
    void remoteGetElectionResponse(int nodeId) throws RemoteException;
	void remoteBroadcastNewNodeInfo(String IP, int port, int nodeID) throws RemoteException;
	void remoteBroadcastCoordinatorNodeID(int nodeID) throws RemoteException;
	public int remoteGetCoordinatorID();
	public HashMap<Integer, String> remoteInsertNode(int nodeID, String nodeIP);
    public void remoteSetCoordinator() throws RemoteException;
}


