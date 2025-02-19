package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;
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

	public boolean verify()
	{
		return new UUID(this.most, this.least).equals(Handshake.SIGNATURE);
	}

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
		this.most = buffer.getLong();
		this.least = buffer.getLong();
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
		buffer.putLong(this.most);
		buffer.putLong(this.least);
	}

	@Override
	public int ID()
	{
		return Handshake.ID;
	}
}
