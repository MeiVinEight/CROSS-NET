package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;

public abstract class Datapack implements Consumer<ConnectionManager>
{
	public abstract void read(ReadableByteChannel in) throws IOException;

	public abstract void write(WritableByteChannel out) throws IOException;

	public abstract int ID();
}
