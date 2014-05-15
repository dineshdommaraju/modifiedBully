
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.*;
import java.util.HashMap;


public interface RemoteInterface extends Remote{
	
	public boolean remoteAccess(int nodeID) throws RemoteException,NotBoundException;
	public int remoteLeave(int nodeID) throws RemoteException,NotBoundException;
	public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException, NotBoundException;
	void remoteInitiateElection(int nodeId) throws RemoteException,NotBoundException;
    void remoteGetElectionResponse(int nodeId) throws RemoteException,NotBoundException;
	void remoteBroadcastNewNodeInfo(String IP, int port, int nodeID) throws RemoteException,NotBoundException;
	int remoteBroadcastCoordinatorNodeID(int nodeID) throws RemoteException,NotBoundException;
	public int remoteGetCoordinatorID() throws RemoteException,NotBoundException;
	//public HashMap<Integer, String> remoteInsertNode(int nodeID, String nodeIP) throws RemoteException,NotBoundException;
    public void remoteSetCoordinator() throws RemoteException,NotBoundException;
	int getCoordinatorID() throws RemoteException,NotBoundException;
	public void displayEnteringCS() throws RemoteException,NotBoundException;
	public int remoteGetMessageCount() throws RemoteException,NotBoundException;
	public int remoteGetCriticalSectionStatus() throws RemoteException,NotBoundException;
	public void remoteCheckAlive() throws RemoteException,NotBoundException;
}


