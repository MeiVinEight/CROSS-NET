package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;

public class Ping extends Datapack
{
	public static final int ID = 0x03;
	public long timestamp;

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
		this.timestamp = buffer.getLong();
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
		buffer.putLong(this.timestamp);
	}

	@Override
	public int ID()
	{
		return 0;
	}

	@Override
	public int length()
	{
		return 8;
	}

	@Override
	public void accept(ConnectionManager connectionManager)
	{
	}
}
