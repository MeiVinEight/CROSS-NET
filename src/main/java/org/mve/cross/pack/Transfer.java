package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;

public class Transfer extends Datapack
{
	public static final int ID = 0x06;
	public byte[] payload;

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
		int length = buffer.getInt();
		this.payload = new byte[length];
		buffer.get(this.payload);
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
		buffer.putInt(this.payload.length);
		buffer.put(this.payload);
	}

	@Override
	public int ID()
	{
		return Transfer.ID;
	}

	@Override
	public void accept(ConnectionManager connectionManager)
	{
	}
}
