package org.mve.cross;

import org.mve.cross.pack.*;
import org.mve.invoke.ConstructorAccessor;
import org.mve.invoke.ReflectionFactory;

import java.lang.invoke.MethodType;

public class ProtocolManager
{
	public static final ConstructorAccessor<? extends Datapack>[] CONSTRUCTOR;

	static
	{
		CONSTRUCTOR = (ConstructorAccessor<? extends Datapack>[]) new ConstructorAccessor<?>[7];
		CONSTRUCTOR[Handshake.ID] = ReflectionFactory.access(Handshake.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Acknowledge.ID] = ReflectionFactory.access(Acknowledge.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Disconnect.ID] = ReflectionFactory.access(Disconnect.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Ping.ID] = ReflectionFactory.access(Ping.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Listen.ID] = ReflectionFactory.access(Listen.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Connection.ID] = ReflectionFactory.access(Connection.class, MethodType.methodType(void.class));
		CONSTRUCTOR[Transfer.ID] = ReflectionFactory.access(Transfer.class, MethodType.methodType(void.class));
	}
}
