package org.mve.cross.pack;

import org.mve.cross.CrossNet;
import org.mve.cross.Serialization;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;

public class Transfer extends Datapack
{
	public static final int ID = 0x06;
	private static final DynamicArray BUFFER = new DynamicArray();
	public int UID;
	public byte[] payload;

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
		this.UID = buffer.getInt();
		int length = buffer.getInt();
		this.payload = new byte[length];
		buffer.get(this.payload);
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
		buffer.putInt(this.UID);
		buffer.putInt(this.payload.length);
		buffer.put(this.payload);
	}

	@Override
	public int ID()
	{
		return Transfer.ID;
	}

	@Override
	public void accept(ConnectionManager conn)
	{
		ConnectionMapping mapping = conn.network.connection(this.UID);
		if (mapping == null) return;
		synchronized (Transfer.BUFFER)
		{
			Transfer.BUFFER.clear();
			Transfer.BUFFER.acquire(this.payload.length);
			Transfer.BUFFER.put(this.payload);
			Transfer.BUFFER.flip();
			try
			{
				Serialization.transfer((WritableByteChannel) mapping.client, Transfer.BUFFER);
			}
			catch (IOException e)
			{
				CrossNet.LOG.log(Level.WARNING, null, e);
			}
		}
	}
}
