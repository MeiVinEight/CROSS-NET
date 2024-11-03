package org.mve.cross.pack;

import org.mve.cross.ConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public abstract class Datapack implements Consumer<ConnectionManager>
{
	public abstract void read(InputStream in) throws IOException;

	public abstract void write(OutputStream out) throws IOException;

	public abstract int ID();
}
