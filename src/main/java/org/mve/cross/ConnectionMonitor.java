package org.mve.cross;

import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class ConnectionMonitor implements Runnable
{
	private final NetworkManager network;
	private final ServerSocket server;
	private ConnectionWaiting waiting = null;

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
				CrossNet.LOG.log(Level.SEVERE, null, e);
			}
			if (socket == null) continue;


			int rp = 0;
			int lp = socket.getLocalPort();
			CrossNet.LOG.info("Connection from " + socket.getRemoteSocketAddress() + " at " + lp);

			Connection conn = new Connection();
			conn.LP = (short) lp;
			// Assume only one thread monitor this port
			if (this.waiting != null) this.waiting.close();
			this.waiting = new ConnectionWaiting(socket);
			try
			{
				CrossNet.LOG.info("Waiting transfer connection for " + socket.getRemoteSocketAddress());
				this.network.communication.send(conn);
				ConnectionManager cm = this.waiting.get();
				if (cm == null) throw new NullPointerException();

				CrossNet.LOG.info(
					"Transfer connection " +
						socket.getRemoteSocketAddress() +
						" - " +
						cm.socket.getRemoteSocketAddress()
				);
				TransferManager transfer = new TransferManager(cm, socket);
				this.network.connection(transfer.RP(), transfer.LP(), transfer);
			}
			catch (Throwable e)
			{
				CrossNet.LOG.severe("Connection waiting error");
				CrossNet.LOG.log(Level.SEVERE, null, e);
				this.waiting.close();
			}
			this.waiting = null;
		}
	}

	public ConnectionWaiting waiting()
	{
		return this.waiting;
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
