package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Disconnect extends Datapack
{
	public static final int ID = 0x02;

	@Override
	public void read(ReadableByteChannel in) throws IOException
	{
	}

	@Override
	public void write(WritableByteChannel out) throws IOException
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
