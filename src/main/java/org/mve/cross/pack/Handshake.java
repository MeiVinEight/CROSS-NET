package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.Serialization;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

public class Handshake extends Datapack
{
	public static final int ID = 0x00; // HANDSHAKE
	// Random generated uuid
	public static final UUID SIGNATURE = new UUID(-740609665978645947L, -8246978237028082185L);

	public long most;
	public long least;

	public Handshake()
	{
		this.most = SIGNATURE.getMostSignificantBits();
		this.least = SIGNATURE.getLeastSignificantBits();
	}

	@Override
	public void accept(ConnectionManager conn)
	{
	}

	@Override
	public void read(ReadableByteChannel in) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(16);
		Serialization.transfer(in, buffer);
		buffer.flip();
		this.most = buffer.getLong();
		this.least = buffer.getLong();
	}

	@Override
	public void write(WritableByteChannel out) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(16);
		buffer.putLong(this.most);
		buffer.putLong(this.least);
		buffer.flip();
		Serialization.transfer(out, buffer);
	}

	@Override
	public int ID()
	{
		return Handshake.ID;
	}
}
