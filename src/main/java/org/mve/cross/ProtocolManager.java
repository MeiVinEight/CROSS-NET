package org.mve.cross;

import org.mve.cross.pack.Transfer;
import org.mve.invoke.ConstructorAccessor;
import org.mve.invoke.ReflectionFactory;
import org.mve.cross.pack.Connection;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;

import java.lang.invoke.MethodType;

public class ProtocolManager
{
	public static final ConstructorAccessor<? extends Datapack>[] CONSTRUCTOR;

	static
	{
		CONSTRUCTOR = (ConstructorAccessor<? extends Datapack>[]) new ConstructorAccessor<?>[3];
		CONSTRUCTOR[Handshake.ID] = ReflectionFactory.access(Handshake.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Connection.ID] = ReflectionFactory.access(Connection.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Transfer.ID] = ReflectionFactory.access(Transfer.class, MethodType.methodType(void.class));
	}
}
