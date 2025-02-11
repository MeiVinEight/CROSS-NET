package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Acknowledge extends Datapack
{
	public static final int ID = 0x01;

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
		return 0;
	}

	@Override
	public void accept(ConnectionManager connectionManager)
	{
	}
}
