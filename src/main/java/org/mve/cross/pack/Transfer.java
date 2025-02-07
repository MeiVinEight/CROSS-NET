package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Transfer extends Datapack
{
	public static final int ID = 0x02;
	public byte[] payload;

	@Override
	public void read(InputStream in) throws IOException
	{
		int length = Serialization.R2(in);
		this.payload = new byte[length];
		Serialization.R(in, this.payload, length);
	}

	@Override
	public void write(OutputStream out) throws IOException
	{
		Serialization.W2(out, (short) this.payload.length);
		Serialization.W(out, this.payload, this.payload.length);
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
