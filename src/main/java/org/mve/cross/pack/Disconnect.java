package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;

public class Disconnect extends Datapack
{
	public static final int ID = 0x02;

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
		return Disconnect.ID;
	}

	@Override
	public void accept(ConnectionManager connectionManager)
	{
	}
}
