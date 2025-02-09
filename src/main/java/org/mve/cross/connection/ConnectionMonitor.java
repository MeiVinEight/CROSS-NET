package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class ConnectionMonitor implements Runnable
{
	private final NetworkManager network;
	private final ServerSocketChannel server;
	public final int ID;

	public ConnectionMonitor(NetworkManager network, ServerSocketChannel server)
	{
		this.network = network;
		this.server = server;
		this.ID = server.socket().getLocalPort();
		CrossNet.LOG.info("Communication listen " + this.ID);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Connection-" + this.ID);
		while (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
		{
			CrossNet.LOG.info("Waiting for connection at " + this.ID);
			SocketChannel socket = null;
			try
			{
				socket = server.accept();
			}
			catch (IOException e)
			{
				if (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
				{
					CrossNet.LOG.log(Level.SEVERE, null, e);
				}
			}
			if (socket == null) continue;
			if (this.network.communication == null)
			{
				try
				{
					socket.close();
				}
				catch (IOException e)
				{
					CrossNet.LOG.log(Level.WARNING, null, e);
				}
				continue;
			}


			int lp = socket.socket().getLocalPort();
			CrossNet.LOG.info("Connection from " + socket.socket().getRemoteSocketAddress() + " at " + lp);
			Connection conn = new Connection();
			conn.LP = (short) lp;
			try
			{
				this.network.communication.send(conn);
			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Connection send failed");
				CrossNet.LOG.log(Level.SEVERE, null, e);
				try
				{
					socket.close();
				}
				catch (IOException ex)
				{
					CrossNet.LOG.log(Level.SEVERE, null, ex);
				}
				continue;
			}
			this.network.waiting[this.ID].offer(socket);
		}
	}

	public void close()
	{
		CrossNet.LOG.info("Connection server " + this.ID + " closing");
		try
		{
			server.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
	}
}
