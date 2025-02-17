package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;

public class Acknowledge extends Datapack
{
	public static final int ID = 0x01;

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
	}

	@Override
	public int ID()
	{
		return Acknowledge.ID;
	}

	@Override
	public int length()
	{
		return 0;
	}

	@Override
	public void accept(ConnectionManager connectionManager)
	{
	}
}
