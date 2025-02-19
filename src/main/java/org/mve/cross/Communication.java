package org.mve.cross;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.Selection;
import org.mve.cross.pack.Datapack;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;

public class Communication implements Selection
{
	private final NetworkManager network;
	private final ConnectionManager connection;
	private SelectionKey selection;

	public Communication(NetworkManager network, ConnectionManager connection)
	{
		this.network = network;
		this.connection = connection;
	}

	public void close()
	{
		if (this.selection != null) this.selection.cancel();
		this.connection.close();
	}

	@Override
	public void register(NetworkManager network) throws ClosedChannelException
	{
		this.selection = network.register(this.connection, SelectionKey.OP_READ, this);
	}

	@Override
	public void select(SelectionKey key)
	{
		Datapack datapack;
		try
		{
			datapack = this.connection.receive();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, "Communication " + this.connection.address + " closed", e);
			this.network.communication.close();
			this.network.communication = null;
			return;
		}

		try
		{
			if (datapack != null) datapack.accept(this.connection);
		}
		catch (Throwable t)
		{
			CrossNet.LOG.log(Level.WARNING, null, t);
		}
	}

	public void send(Datapack datapack) throws IOException
	{
		this.connection.send(datapack);
	}
}
