package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class ConnectionMonitor implements Runnable
{
	private final NetworkManager network;
	private final ServerSocket server;

	public ConnectionMonitor(NetworkManager network, ServerSocket server)
	{
		this.network = network;
		this.server = server;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Connection-" + this.server.getLocalPort());
		while (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
		{
			Socket socket = null;
			try
			{
				CrossNet.LOG.info("Waiting for connection at " + this.server.getLocalPort());
				socket = this.server.accept();
			}
			catch (IOException e)
			{
				if (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
				{
					CrossNet.LOG.log(Level.SEVERE, null, e);
				}
			}
			if (socket == null) continue;


			int lp = socket.getLocalPort();
			CrossNet.LOG.info("Connection from " + socket.getRemoteSocketAddress() + " at " + lp);
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
			this.network.waiting[this.server.getLocalPort()].offer(socket);
		}
	}

	public void close()
	{
		CrossNet.LOG.info("Connection server " + this.server.getLocalPort() + " closing");
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
