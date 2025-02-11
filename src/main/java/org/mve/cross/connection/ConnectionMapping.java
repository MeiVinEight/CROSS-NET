package org.mve.cross.connection;

import java.nio.channels.SocketChannel;

public class ConnectionMapping
{
	public final ConnectionManager server;
	public final SocketChannel client;

	public ConnectionMapping(ConnectionManager server, SocketChannel client)
	{
		this.server = server;
		this.client = client;
	}
}
