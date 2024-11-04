package org.mve.cross;

import org.mve.cross.pack.Datapack;

import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;

public class Communication implements Runnable
{
	private final ConnectionManager connection;

	public Communication(ConnectionManager connection)
	{
		this.connection = connection;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Communication");
		try
		{
			while (this.connection.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
			{
				Datapack datapack = this.connection.receive();
				try
				{
					datapack.accept(this.connection);
				}
				catch (Throwable e)
				{
					CrossNet.LOG.log(Level.WARNING, null, e);
				}
			}
		}
		catch (EOFException eof)
		{
			CrossNet.LOG.warning("Communication " + this.connection.socket.getRemoteSocketAddress() + " closed");
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.SEVERE, null, e);
		}
		this.connection.network.close();
	}
}
