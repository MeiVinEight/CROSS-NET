package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.concurrent.Synchronize;
import org.mve.cross.pack.Connection;
import org.mve.cross.transfer.TransferManager;

import java.io.IOException;
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
		// Not allowed on server side
		/*
		if (this.network.type == CrossNet.SIDE_SERVER)
		{
			this.cancel();
			return;
		}
		*/

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
			CrossNet.LOG.info(MessageFormat.format("[{0}] connect to server {1}", cid.ID, cid.server));
			serverChannel = SocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.connect(cid.server);
			while (!serverChannel.finishConnect()) Thread.yield();
			CrossNet.LOG.info(MessageFormat.format("[{0}] Handshake with server {1}", cid.ID, cid.server));
			cm = new ConnectionManager(this.network, serverChannel);
			CrossNet.LOG.info(MessageFormat.format("[{0}] Talk to server {1}", cid.ID, cid.server));
			Connection pack = new Connection();
			pack.RP = (short) mapping.client.socket().getPort();
			pack.UID = cid.ID;
			cm.send(pack);
		}
		catch (IOException e)
		{
			CrossNet.LOG.info(MessageFormat.format("[{0}] failed handshake with server {1}", cid.ID, cid.server));
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

		if (cm == null) return;
		this.poll(cm, cid.ID);
	}

	public void offer(ConnectionID conn)
	{
		if (conn == null) return;

		this.connection.offer(conn);
		String message = "Connection waiting for [" + conn.ID + "]";
		if (conn.server != null)
		{
			message += " " + conn.server;
		}
		message += ", " + this.connection.size() + " connection(s)";
		CrossNet.LOG.info(message);
	}

	public void poll(ConnectionManager cm, int id)
	{
		ConnectionMapping mapping = this.network.connection(id);
		mapping.status = ConnectionMapping.CONNECTED;
		mapping.server = cm;
		CrossNet.LOG.info(
			"Transfer connection " +
			mapping.client.socket().getRemoteSocketAddress() +
			" - " +
			cm.address +
			", " +
			this.connection.size() +
			" connection(s)"
		);
		new TransferManager(cm.network, id);
	}

	public void close()
	{
		this.cancel();
	}
}
