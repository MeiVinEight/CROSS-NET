package org.mve.cross.connection;

import org.mve.cross.Communication;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.concurrent.Synchronize;
import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
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
		if (!mapping.waiting()) return;
		if (this.network.type == CrossNet.SIDE_SERVER)
		{
			this.connection.offer(cid);
			return;
		}

		boolean continu;
		try
		{
			continu = cid.connect(this.network, mapping);
		}
		catch (IOException e)
		{
			String msg = MessageFormat.format("[{0}] Connection {1} {2}", cid.ID, cid.locale, e.getMessage());
			CrossNet.LOG.warning(msg);
			cid.status = ConnectionID.CLOSED;
			mapping.close();
			Connection signal = new Connection();
			signal.type = Connection.TYPE_CLOSE;
			signal.UID = cid.ID;
			Communication com = this.network.communication;
			try
			{
				com.send(signal);
			}
			catch (IOException ex)
			{
				CrossNet.LOG.log(Level.WARNING, "Could not send signal close", ex);
			}
			return;
		}


		if (continu)
		{
			this.connection.offer(cid);
			return;
		}
		this.poll(cid.connection, cid.ID);
	}

	public void offer(ConnectionID conn)
	{
		if (conn == null) return;

		this.connection.offer(conn);
		SocketAddress addr = conn.locale;
		int count = this.connection.size();
		String msg = MessageFormat.format("[{0}] Waiting for {1} ({2})", conn.ID, addr, count);
		CrossNet.LOG.info(msg);
	}

	public void poll(ConnectionManager cm, int id)
	{
		cm.blocking = false;
		ConnectionMapping mapping = this.network.connection(id);
		if (!mapping.waiting()) return;
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
