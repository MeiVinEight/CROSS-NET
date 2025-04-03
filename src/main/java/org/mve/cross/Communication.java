package org.mve.cross;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
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
	private final int UID;
	private SelectionKey selection;

	public Communication(NetworkManager network, ConnectionManager connection, int uid)
	{
		this.network = network;
		this.connection = connection;
		this.UID = uid;
	}

	public void close()
	{
		if (this.selection != null) this.selection.cancel();
		this.connection.close();
		ConnectionMapping mapping = this.network.connection(this.UID);
		if (mapping != null)
		{
			mapping.close();
		}
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
			if (this.UID == 0)
			{
				CrossNet.LOG.log(Level.WARNING, "Communication " + this.connection.address + " closed", e);
				this.network.communication = null;
			}
			this.close();
			return;
		}

		if (!this.connection.established())
		{
			this.close();
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
