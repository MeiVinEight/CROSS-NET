package org.mve.cross.transfer;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class TransferMonitor implements Runnable
{
	public final NetworkManager network;
	public final ServerSocket server;

	public TransferMonitor(NetworkManager network, ServerSocket server)
	{
		this.network = network;
		this.server = server;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Transfer-" + this.server.getLocalPort());
		try
		{
			while (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
			{
				CrossNet.LOG.info("Waiting for transfer connection at " + this.server.getLocalPort());
				Socket socket = this.server.accept();
				socket.setSoTimeout(5000);
				ConnectionManager cm = new ConnectionManager(this.network, socket);
				try
				{
					// TODO Check connection source (Handshake key)
					Datapack pack = cm.receive();
					if (!(pack instanceof Handshake))
					{
						CrossNet.LOG.warning("Handshake required: " + socket.getRemoteSocketAddress());
						cm.close();
						continue;
					}
					Handshake handshake = new Handshake();
					handshake.listen = ((Handshake) pack).listen;
					cm.send(handshake);
					pack.accept(cm);
					if (cm.socket.isClosed())
					{
						CrossNet.LOG.warning("Connection closed: " + socket.getRemoteSocketAddress());
					}
				}
				catch (IOException e)
				{
					CrossNet.LOG.severe("Handshake error");
					CrossNet.LOG.log(Level.SEVERE, null, e);
					socket.close();
					cm.close();
				}
			}
		}
		catch (IOException e)
		{
			if (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
			{
				CrossNet.LOG.severe("Transfer waiting error");
				CrossNet.LOG.log(Level.SEVERE, null, e);
			}
		}
		this.network.close();
	}

	public void close()
	{
		this.network.close();
		try
		{
			this.server.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.severe("Close transfer monitor");
			CrossNet.LOG.log(Level.SEVERE, null, e);
		}

	}
}
