package org.mve.cross.pack;

import org.mve.cross.Serialization;
import org.mve.cross.connection.ConnectionManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Ping extends Datapack
{
	public static final int ID = 0x03;
	public long timestamp;

	@Override
	public void read(ReadableByteChannel in) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(8);
		Serialization.transfer(in, buffer);
		buffer.flip();
		this.timestamp = buffer.getLong();
	}

	@Override
	public void write(WritableByteChannel out) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(this.timestamp);
		buffer.flip();
		Serialization.transfer(out, buffer);
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
