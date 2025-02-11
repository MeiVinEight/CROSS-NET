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
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ConnectionWaiting extends Synchronize
{
	private final NetworkManager network;
	private final ConnectionID[] waiting = new ConnectionID[65536];
	private final Queue<Integer> connection = new ConcurrentLinkedQueue<>();
	private final ReentrantLock lock = new ReentrantLock();

	public ConnectionWaiting(NetworkManager network)
	{
		this.network = network;
	}

	@Override
	public void run()
	{
		// Not allowed on server side
		if (this.network.type == CrossNet.SIDE_SERVER)
		{
			this.cancel();
			return;
		}

		Integer objid = this.connection.poll();
		if (objid == null) return;
		ConnectionID cid = this.waiting[objid];
		if (cid == null) return;

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
			pack.RP = (short) cid.client.socket().getPort();
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
		ConnectionID exchange = conn;

		this.lock.lock();
		if (this.waiting[conn.ID] == null)
		{
			this.waiting[conn.ID] = exchange;
			exchange = null;
		}
		this.lock.unlock();

		if (exchange != null)
		{
			CrossNet.LOG.warning("Waiting already exists: " + conn.ID);
			return;
		}

		this.connection.offer(conn.ID);
		CrossNet.LOG.info(
			"Connection waiting for [" +
			conn.ID +
			"] " +
			conn.server +
			", " +
			this.connection.size() +
			" connection(s)"
		);
	}

	public void poll(ConnectionManager cm, int id)
	{
		ConnectionID cid = null;
		this.lock.lock();
		if (this.waiting[id] != null)
		{
			cid = this.waiting[id];
			this.waiting[id] = null;
		}
		this.lock.unlock();
		if (cid == null)
		{
			CrossNet.LOG.warning("Unknown waiting ID: " + id);
			return;
		}

		CrossNet.LOG.info(
			"Transfer connection " +
			cid.client.socket().getRemoteSocketAddress() +
			" - " +
			cm.address +
			", " +
			this.connection.size() +
			" connection(s)"
		);
		ConnectionMapping mapping = new ConnectionMapping(cm, cid.client);
		this.network.connection(id, mapping);
		TransferManager tm = new TransferManager(cm.network, id);
	}

	public boolean occupy(int id)
	{
		return this.waiting[id] != null;
	}

	public void close()
	{
		this.cancel();
	}
}
