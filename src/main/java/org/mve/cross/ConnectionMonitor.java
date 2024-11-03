package org.mve.cross;

import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.LockSupport;
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
		Thread.currentThread().setName("SERVER");
		while (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
		{
			Socket socket = null;
			try
			{
				socket = this.server.accept();
			}
			catch (IOException e)
			{
				CrossNet.LOG.log(Level.SEVERE, null, e);
			}
			if (socket == null) continue;


			int rp = 0;
			int lp = socket.getLocalPort();
			CrossNet.LOG.info("Connection from " + socket.getRemoteSocketAddress() + " at " + lp);

			Connection conn = new Connection();
			conn.LP = (short) lp;
			// Assume only one thread monitor this port
			while (network.connection(rp, lp) != null)
			{
				// Waiting
				LockSupport.park();
			}
			network.connection(rp, lp, socket);
			try
			{
				this.network.communication.send(conn);
			}
			catch (IOException e)
			{
				e.printStackTrace(System.out);
				network.connection(rp, lp, null);
				try
				{
					socket.close();
				}
				catch (IOException ex)
				{
					ex.printStackTrace(System.out);
				}
			}
		}
	}
}