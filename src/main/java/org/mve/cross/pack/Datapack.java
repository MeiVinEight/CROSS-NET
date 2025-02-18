package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;
import java.util.function.Consumer;

public abstract class Datapack implements Consumer<ConnectionManager>
{
	public abstract void read(DynamicArray buffer) throws IOException;

	public abstract void write(DynamicArray buffer) throws IOException;

	public abstract int ID();
}
