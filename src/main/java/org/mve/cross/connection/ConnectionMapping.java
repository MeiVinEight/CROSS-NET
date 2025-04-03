package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.nio.DynamicArray;
import org.mve.cross.nio.Selection;
import org.mve.cross.pack.Transfer;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class ConnectionMapping implements Selection
{
	public static final int CLOSED    = 0;
	public static final int WAITING   = 1;
	public static final int CONNECTED = 2;
	public static final int CLOSING   = 3;
	private final DynamicArray array = new DynamicArray(1024 * 1024);
	private final NetworkManager network;
	private SelectionKey key;
	public final int UID;
	public int status = ConnectionMapping.CLOSED;
	public ConnectionManager server;
	public SocketChannel client;

	public ConnectionMapping(NetworkManager network, int uid)
	{
		this.network = network;
		UID = uid;
	}

	public boolean waiting()
	{
		return this.status == ConnectionMapping.WAITING;
	}

	public void close()
	{
		if (this.status == ConnectionMapping.CLOSED) return;
		this.status = ConnectionMapping.CLOSING;
		CrossNet.LOG.info("[" + this.UID + "] Connection closing");
		if (this.key != null) this.key.cancel();
		if (this.server != null) this.server.close();
		try
		{
			if (this.client != null) this.client.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.warning("Cannot close client connection");
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
		this.network.free(this.UID);
		this.status = ConnectionMapping.CLOSED;
	}

	@Override
	public void register(NetworkManager network) throws ClosedChannelException
	{
		if (this.status != ConnectionMapping.CONNECTED) throw new ClosedChannelException();
		this.key = network.register(this.client, SelectionKey.OP_READ, this);
	}

	@Override
	public void select(SelectionKey key)
	{
		this.array.clear();
		int read;
		try
		{
			read = this.array.read(this.client);
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
			read = -1;
		}
		if (read == -1)
		{
			this.close();
			return;
		}
		this.array.flip();
		if (this.array.remaining() == 0) return;

		Transfer transfer = new Transfer();
		transfer.UID = this.UID;
		transfer.payload = new byte[this.array.remaining()];
		this.array.get(transfer.payload);
		try
		{
			this.server.send(transfer);
		}
		catch (IOException e)
		{
			this.close();
		}
	}
}
