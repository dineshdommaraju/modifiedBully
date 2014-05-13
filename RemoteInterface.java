package modifiedBully1;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;


public interface RemoteInterface extends Remote{
	
	public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException;
	void remoteInitiateElection() throws RemoteException;
	void remoteBroadcastNewNodeInfo(String IP,int port, int nodeID) throws RemoteException;
	void remoteBroadcastCoordinatorNodeID(int port) throws RemoteException;
}

