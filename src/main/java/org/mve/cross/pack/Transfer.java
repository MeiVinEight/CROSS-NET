package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.Serialization;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Transfer extends Datapack
{
	public static final int DEFAULT_BUFFER_SIZE = 4096;
	public static final int ID = 0x02;
	public byte[] payload;

	@Override
	public void read(ReadableByteChannel in) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(Transfer.DEFAULT_BUFFER_SIZE);
		buffer.limit(4);
		Serialization.transfer(in, buffer);
		buffer.flip();
		int length = buffer.getInt();

		if (length <= Transfer.DEFAULT_BUFFER_SIZE) buffer.clear();
		else buffer = ByteBuffer.allocateDirect(length);
		buffer.limit(length);

		Serialization.transfer(in, buffer);
		buffer.flip();
		this.payload = new byte[length];
		buffer.get(this.payload);
	}

	@Override
	public void write(WritableByteChannel out) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(this.payload.length + 4);
		buffer.putInt(this.payload.length);
		buffer.put(this.payload);
		buffer.flip();
		Serialization.transfer(out, buffer);
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
