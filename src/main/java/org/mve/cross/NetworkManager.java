package org.mve.cross;

import org.mve.cross.concurrent.Synchronize;
import org.mve.cross.concurrent.SynchronizeNET;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
import org.mve.cross.connection.ConnectionMonitor;
import org.mve.cross.connection.ConnectionWaiting;
import org.mve.cross.nio.Selection;
import org.mve.cross.transfer.TransferMonitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

public class NetworkManager extends Synchronize
{
	public static final int NETWORK_STAT_RUNNING = 2;
	public static final int NETWORK_STAT_STOPPED = 3;
	public final int type;
	// Server listen connections from frp client
	private final TransferMonitor transfer;
	// Communication connect between FRP server and client
	public Communication communication;
	// Server listen connections from all users
	public final ConnectionMonitor[] server = new ConnectionMonitor[65536];
	private final ConnectionMapping[] connection = new ConnectionMapping[65536];
	public final SynchronizeNET synchronize = new SynchronizeNET(this);
	public final ConnectionWaiting waiting = new ConnectionWaiting(this);
	private final Selector selector;
	private final VariableID identifier = new VariableID(1, 65535);
	private int status;

	public NetworkManager(int type)
	{
		this.type = type;
		Thread thread = new Thread(this);
		thread.setName("Network");
		try
		{
			this.selector = Selector.open();
			if (this.type == CrossNet.SIDE_SERVER)
			{
				CrossNet.LOG.info("Opening server on " + Configuration.SERVER_PORT);
				ServerSocketChannel remote = ServerSocketChannel.open();
				remote.configureBlocking(false);
				remote.bind(new InetSocketAddress(Configuration.SERVER_PORT));
				// Create a connection with frp client
				this.status = NetworkManager.NETWORK_STAT_RUNNING;
				this.transfer = new TransferMonitor(this, remote);
				this.transfer.register(this);
			}
			else // CrossNet.SIDE_CLIENT
			{
				CrossNet.LOG.info("Connecting server on " + Configuration.SERVER_PORT);
				this.transfer = null;
				this.status = NetworkManager.NETWORK_STAT_RUNNING;
				long period = Configuration.COMMUNICATION_CONNECT / SynchronizeNET.PERIOD_MS;
				this.synchronize.offer(new CommunicationWaiting(this, period));
			}
			this.synchronize.offer(this.waiting);
		}
		catch (Throwable t)
		{
			CrossNet.LOG.log(Level.SEVERE, null, t);
			this.close();
			throw new RuntimeException(t);
		}
		thread.start();
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
		ConnectionMapping mapping;
		synchronized (this.connection)
		{
			if (this.connection[id] == null)
			{
				this.connection[id] = new ConnectionMapping(this, id);
			}
			mapping = this.connection[id];
		}
		return mapping;
	}

	public ConnectionMapping connection(int id)
	{
		return this.connection[id];
	}

	public void free(int id)
	{
		ConnectionMapping mapping = null;
		synchronized (this.connection)
		{
			if (this.connection[id] != null)
			{
				mapping = this.connection[id];
				this.connection[id] = null;
			}
		}
		if (this.type == CrossNet.SIDE_CLIENT) return;
		if (mapping != null)
		{
			int stat = this.identifier.free(mapping.UID);
			if (stat != 0)
			{
				CrossNet.LOG.warning(MessageFormat.format("Wrong freeing [{0}]: {1}", id, stat));
			}
		}
	}

	public SelectionKey register(SelectableChannel channel, int interest, Object attachment)
		throws ClosedChannelException
	{
		SelectionKey key = channel.register(this.selector, interest, attachment);
		this.selector.wakeup();
		return key;
	}

	public SelectionKey register(ConnectionManager cm, int interest, Object attachment) throws ClosedChannelException
	{
		SelectionKey key = cm.register(this.selector, interest, attachment);
		this.selector.wakeup();
		return key;
	}

	@Override
	public void run()
	{
		CrossNet.LOG.info("Network selection thread start");
		while (this.status == NetworkManager.NETWORK_STAT_RUNNING)
		{
			Thread.yield();
			int selected = 0;
			try
			{
				selected = selector.select();
			}
			catch (IOException e)
			{
				CrossNet.LOG.log(Level.WARNING, null, e);
			}
			if (selected == 0) continue;
			Set<SelectionKey> keys = this.selector.selectedKeys();
			for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); )
			{
				SelectionKey key = it.next();
				it.remove();
				Selection selection = (Selection) key.attachment();
				try
				{
					selection.select(key);
				}
				catch (Throwable t)
				{
					CrossNet.LOG.log(Level.SEVERE, null, t);
				}
			}
		}
		CrossNet.LOG.info("Network selection thread exit");
	}
}
