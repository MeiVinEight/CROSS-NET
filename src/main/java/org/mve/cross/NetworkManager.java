package org.mve.cross;

import org.mve.cross.concurrent.SynchronizeNET;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
import org.mve.cross.connection.ConnectionMonitor;
import org.mve.cross.connection.ConnectionWaiting;
import org.mve.cross.transfer.TransferManager;
import org.mve.cross.transfer.TransferMonitor;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;

public class NetworkManager
{
	public static final int NETWORK_STAT_RUNNING = 2;
	public static final int NETWORK_STAT_STOPPED = 3;
	public final int type;
	// Server listen connections from frp client
	private final TransferMonitor transfer;
	// Communication connect between FRP server and client
	public ConnectionManager communication;
	// Server listen connections from all users
	public final ConnectionMonitor[] server = new ConnectionMonitor[65536];
	public final ConnectionWaiting[] waiting = new ConnectionWaiting[65536];
	private int status;
	public TransferManager[][][][] connection = new TransferManager[256][][][];
	public final SynchronizeNET synchronize = new SynchronizeNET(this);

	public NetworkManager(int type)
	{
		this.type = type;
		try
		{
			if (this.type == CrossNet.SIDE_SERVER)
			{
				// Create remote listen server
				// ServerSocket remote = new ServerSocket(SERVER_PORT);
				ServerSocketChannel remote = ServerSocketChannel.open();
				// remote.configureBlocking(false);
				remote.bind(new InetSocketAddress(Configuration.SERVER_PORT));
				// this.transfer = new ServerSocket(SERVER_PORT);
				// Create a connection with frp client
				this.status = NetworkManager.NETWORK_STAT_RUNNING;
				this.transfer = new TransferMonitor(this, remote);
				new Thread(new Communication(this)).start();
				new Thread(this.transfer).start();
			}
			else // CrossNet.SIDE_CLIENT
			{
				this.transfer = null;
				this.status = NetworkManager.NETWORK_STAT_RUNNING;
				new Thread(new Communication(this)).start();
			}
		}
		catch (Throwable t)
		{
			this.status = NetworkManager.NETWORK_STAT_STOPPED;
			CrossNet.LOG.log(Level.SEVERE, null, t);
			this.close();
			throw new RuntimeException(t);
		}
	}

	public int status()
	{
		return this.status;
	}

	public void close()
	{
		if (this.status == NETWORK_STAT_STOPPED) return;

		CrossNet.LOG.warning("CLOSE");
		this.status = NetworkManager.NETWORK_STAT_STOPPED;
		CrossNet.LOG.info("Synchronize");
		this.synchronize.close();
		CrossNet.LOG.info("Communication");
		this.communication.close();

		if (this.transfer != null)
		{
			CrossNet.LOG.info("Transfer waiting");
			this.transfer.close();
		}

		CrossNet.LOG.info("Transfer connection");
		for (ConnectionMonitor monitor : this.server)
		{
			if (monitor != null)
			{
				monitor.close();
			}
		}

		CrossNet.LOG.close();
	}

	public static int mapping(int port)
	{
		ConnectionMapping mapping = Configuration.MAPPING.get(port);
		if (mapping == null) return 0;
		return mapping.LP;
	}

	public static int timeout(int port)
	{
		ConnectionMapping mapping = Configuration.MAPPING.get(port);
		if (mapping == null) return 0;
		return mapping.timeout;
	}

	public TransferManager connection(int rp, int lp)
	{
		int idx0 = (rp >> 8) & 0xFF;
		int idx1 = (rp >> 0) & 0xFF;
		int idx2 = (lp >> 8) & 0xFF;
		int idx3 = (lp >> 0) & 0xFF;
		TransferManager[][][][] s0 = this.connection;
		if (s0[idx0] == null) s0[idx0] = new TransferManager[256][][];
		TransferManager[][][] s1 = s0[idx0];
		if (s1[idx1] == null) s1[idx1] = new TransferManager[256][];
		TransferManager[][] s2 = s1[idx1];
		if (s2[idx2] == null) s2[idx2] = new TransferManager[256];
		TransferManager[] s3 = s2[idx2];
		return s3[idx3];
	}

	public void connection(int rp, int lp, TransferManager obj)
	{
		int idx0 = (rp >> 8) & 0xFF;
		int idx1 = (rp >> 0) & 0xFF;
		int idx2 = (lp >> 8) & 0xFF;
		int idx3 = (lp >> 0) & 0xFF;
		TransferManager[][][][] s0 = this.connection;
		if (s0[idx0] == null) s0[idx0] = new TransferManager[256][][];
		TransferManager[][][] s1 = s0[idx0];
		if (s1[idx1] == null) s1[idx1] = new TransferManager[256][];
		TransferManager[][] s2 = s1[idx1];
		if (s2[idx2] == null) s2[idx2] = new TransferManager[256];
		TransferManager[] s3 = s2[idx2];
		s3[idx3] = obj;
	}
}
