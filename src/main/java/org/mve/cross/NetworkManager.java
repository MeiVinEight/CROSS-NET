package org.mve.cross;

import org.mve.cross.concurrent.SynchronizeNET;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
import org.mve.cross.connection.ConnectionMonitor;
import org.mve.cross.connection.ConnectionWaiting;
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
	public final ConnectionMapping[] connection = new ConnectionMapping[65536];
	public final SynchronizeNET synchronize = new SynchronizeNET(this);
	public final ConnectionWaiting waiting = new ConnectionWaiting(this);
	private final VariableID identifier = new VariableID(1, 65535);
	private int status;

	public NetworkManager(int type)
	{
		this.type = type;
		try
		{
			if (this.type == CrossNet.SIDE_SERVER)
			{
				CrossNet.LOG.info("Opening server on " + Configuration.SERVER_PORT);
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
				this.synchronize.offer(this.waiting);
			}
			else // CrossNet.SIDE_CLIENT
			{
				CrossNet.LOG.info("Connecting server on " + Configuration.SERVER_PORT);
				this.transfer = null;
				this.status = NetworkManager.NETWORK_STAT_RUNNING;
				new Thread(new Communication(this)).start();
				this.synchronize.offer(this.waiting);
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

	public ConnectionMapping mapping()
	{
		return this.mapping(this.identifier.get());
	}

	public ConnectionMapping mapping(int id)
	{
		if (id == -1) return null;
		this.connection(id, new ConnectionMapping(id));
		return this.connection(id);
	}

	public ConnectionMapping connection(int id)
	{
		return this.connection[id];
	}

	public void connection(int id, ConnectionMapping obj)
	{
		synchronized (this.connection)
		{
			this.connection[id] = obj;
		}
	}
}
