package org.mve.cross;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ConnectionWaiting extends CompletableFuture<ConnectionManager>
{
	public final Socket socket;

	public ConnectionWaiting(Socket socket)
	{
		this.socket = socket;
	}

	public void close()
	{
		try
		{
			this.socket.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.SEVERE, null, e);
		}
	}
}
