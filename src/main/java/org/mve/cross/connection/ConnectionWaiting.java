package org.mve.cross.connection;

import org.mve.cross.Communication;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.concurrent.Synchronize;
import org.mve.cross.net.Addressing;
import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class ConnectionWaiting extends Synchronize
{
	private final NetworkManager network;
	private final Queue<ConnectionID> connection = new ConcurrentLinkedQueue<>();

	public ConnectionWaiting(NetworkManager network)
	{
		this.network = network;
	}

	@Override
	public void run()
	{
		ConnectionID cid = this.connection.poll();
		if (cid == null) return;
		ConnectionMapping mapping = this.network.connection(cid.ID);
		if (mapping == null) return;
		if (mapping.status != ConnectionMapping.WAITING) return;
		if (this.network.type == CrossNet.SIDE_SERVER)
		{
			this.connection.offer(cid);
			return;
		}

		SocketChannel serverChannel = null;
		ConnectionManager cm;
		try
		{
			serverChannel = SocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.connect(cid.server);
			while (!serverChannel.finishConnect()) Thread.yield();
			cm = new ConnectionManager(this.network, serverChannel);
			Connection pack = new Connection();
			pack.RP = (short) mapping.client.socket().getPort();
			pack.UID = cid.ID;
			pack.type = Connection.TYPE_CONNECTION;
			cm.send(pack);
		}
		catch (IOException e)
		{
			String msg = MessageFormat.format("[{0}] Connecting {1} error", cid.ID, cid.server);
			CrossNet.LOG.log(Level.WARNING, msg, e);
			if (serverChannel != null)
			{
				try
				{
					serverChannel.close();
				}
				catch (IOException ex)
				{
					CrossNet.LOG.log(Level.WARNING, null, ex);
				}
			}
			cm = null;
		}

		if (cm == null)
		{
			this.connection.offer(cid);
			return;
		}
		this.poll(cm, cid.ID);
	}

	public void offer(ConnectionID conn)
	{
		if (conn == null) return;

		this.connection.offer(conn);
		SocketAddress addr = Addressing.address(this.network.connection(conn.ID).client);
		int count = this.connection.size();
		String msg = MessageFormat.format("[{0}] Waiting for {1} ({2})", conn.ID, addr, count);
		CrossNet.LOG.info(msg);
	}

	public void poll(ConnectionManager cm, int id)
	{
		cm.blocking = false;
		ConnectionMapping mapping = this.network.connection(id);
		mapping.status = ConnectionMapping.CONNECTED;
		mapping.server = cm;
		try
		{
			new Communication(this.network, mapping.server, id).register(this.network);
			mapping.register(this.network);
		}
		catch (ClosedChannelException e)
		{
			CrossNet.LOG.log(Level.WARNING, "Registration failed", e);
		}
		int count = this.connection.size();
		String msg = MessageFormat.format("[{0}] Connection {1} ({2})", id, cm.address, count);
		CrossNet.LOG.info(msg);
	}

	public void close()
	{
		this.cancel();
	}
}
