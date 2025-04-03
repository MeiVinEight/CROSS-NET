package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.nio.Selection;
import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class ConnectionMonitor implements Selection
{
	private final NetworkManager network;
	private final ServerSocketChannel server;
	private SelectionKey key;
	public final int ID;

	public ConnectionMonitor(NetworkManager network, ServerSocketChannel server)
	{
		this.network = network;
		this.server = server;
		this.ID = server.socket().getLocalPort();
	}

	public void close()
	{
		CrossNet.LOG.info("Connection server " + this.ID + " closing");
		if (this.key != null) this.key.cancel();
		try
		{
			server.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
	}

	@Override
	public void register(NetworkManager network) throws ClosedChannelException
	{
		this.key = network.register(this.server, SelectionKey.OP_ACCEPT, this);
	}

	@Override
	public void select(SelectionKey key)
	{
		SocketChannel socket = null;
		try
		{
			socket = this.server.accept();
		}
		catch (IOException e)
		{
			if (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
			{
				CrossNet.LOG.log(Level.SEVERE, null, e);
			}
		}
		if (socket == null) return;


		try
		{
			socket.configureBlocking(false);
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, "Cannot configure blocking", e);
			try
			{
				socket.close();
			}
			catch (IOException ex)
			{
				CrossNet.LOG.log(Level.WARNING, null, ex);
			}
			return;
		}
		int lp = socket.socket().getLocalPort();
		ConnectionMapping mapping = this.network.mapping();
		if (mapping == null)
		{
			CrossNet.LOG.warning("Connection overflow!");
			try
			{
				socket.close();
			}
			catch (IOException e)
			{
				CrossNet.LOG.log(Level.WARNING, null, e);
			}
			return;
		}
		int id = mapping.UID;

		mapping.client = socket;

		ConnectionID cid = new ConnectionID();
		cid.ID = id;
		this.network.waiting.offer(cid);

		try
		{
			Connection conn = new Connection();
			conn.RP = (short) lp;
			conn.UID = id;
			conn.type = Connection.TYPE_CONNECTION;
			if (this.network.communication == null)
			{
				throw new IOException("Communication is null");
			}
			this.network.communication.send(conn);
		}
		catch (IOException e)
		{
			CrossNet.LOG.severe("Connection send failed");
			CrossNet.LOG.log(Level.SEVERE, null, e);
			this.network.free(id);
			this.network.communication.close();
			this.network.communication = null;
			try
			{
				socket.close();
			}
			catch (IOException ex)
			{
				CrossNet.LOG.log(Level.SEVERE, null, ex);
			}
			mapping.close();
		}
	}
}
