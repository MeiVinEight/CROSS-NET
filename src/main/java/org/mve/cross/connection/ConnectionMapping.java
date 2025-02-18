package org.mve.cross.connection;

import org.mve.cross.CrossNet;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class ConnectionMapping
{
	public static final int WAITING   = 0;
	public static final int CONNECTED = 1;
	public static final int CLOSEING  = 2;
	public static final int CLOSED    = 3;
	public final int UID;
	public int status =  ConnectionMapping.WAITING;
	public ConnectionManager server;
	public SocketChannel client;

	public ConnectionMapping(int uid)
	{
		UID = uid;
	}

	public void close()
	{
		this.status = ConnectionMapping.CLOSEING;
		CrossNet.LOG.info(
			"Transfer connection server " +
			this.server.address +
			" closing"
		);
		this.server.close();
		CrossNet.LOG.info(
			"Transfer connection client " +
			this.client.socket().getRemoteSocketAddress() +
			" closing"
		);
		try
		{
			this.client.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.warning("Cannot close client connection");
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
		this.status = ConnectionMapping.CLOSED;
	}
}
