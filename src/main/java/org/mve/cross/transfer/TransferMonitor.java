package org.mve.cross.transfer;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.pack.Connection;
import org.mve.cross.pack.Datapack;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class TransferMonitor implements Runnable
{
	public final NetworkManager network;
	public final ServerSocketChannel server;
	public final int ID;

	public TransferMonitor(NetworkManager network, ServerSocketChannel server)
	{
		this.network = network;
		this.server = server;
		this.ID = server.socket().getLocalPort();
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Transfer-" + this.ID);
		try
		{
			while (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
			{
				Thread.yield();
				CrossNet.LOG.info("Waiting for transfer connection at " + this.ID);
				SocketChannel socket = server.accept();
				socket.configureBlocking(false);
				ConnectionManager cm = new ConnectionManager(this.network);
				SocketAddress sra = socket.getRemoteAddress();
				try
				{
					cm.connect(socket);
					Datapack datapack = cm.receive();
					if (!(datapack instanceof Connection))
					{
						throw new SocketException("Unknown connection " + sra);
					}
					cm.blocking = false;
					datapack.accept(cm);
				}
				catch (IOException e)
				{
					CrossNet.LOG.severe("Create connection error: " + sra);
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
