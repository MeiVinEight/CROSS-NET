package org.mve.cross.transfer;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.Selection;
import org.mve.cross.pack.Connection;
import org.mve.cross.pack.Datapack;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class TransferMonitor implements Runnable, Selection
{
	private SelectionKey selection;
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
		while (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
		{
			Thread.yield();
			CrossNet.LOG.info("Waiting for transfer connection at " + this.ID);
			this.select(this.selection);
		}
	}

	public void close()
	{
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

	@Override
	public void register(NetworkManager network) throws ClosedChannelException
	{
		this.selection = network.register(this.server, SelectionKey.OP_ACCEPT, this);
	}

	@Override
	public void select(SelectionKey key)
	{
		SocketChannel socket = null;
		ConnectionManager cm = new ConnectionManager(this.network);
		try
		{
			socket = server.accept();
			socket.configureBlocking(false);
			SocketAddress sra = socket.getRemoteAddress();
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
			if (socket == null)
			{
				CrossNet.LOG.log(Level.WARNING, "Acceptance error", e);
				return;
			}

			SocketAddress sra = socket.socket().getRemoteSocketAddress();
			CrossNet.LOG.log(Level.WARNING, "Connection " + sra + " error", e);
			try
			{
				socket.close();
			}
			catch (IOException ex)
			{
				CrossNet.LOG.log(Level.WARNING, null, ex);
			}
			cm.close();
		}
	}
}
