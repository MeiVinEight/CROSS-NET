package org.mve.cross;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

public class TransferManager
{
	public final ConnectionManager connection;
	public final Socket socket;
	public final Thread S2C;
	public final Thread C2S;
	private boolean running = true;

	public TransferManager(ConnectionManager connection, Socket socket)
	{
		this.connection = connection;
		this.socket = socket;
		this.S2C = new Thread(new TransferS2C(this));
		this.C2S = new Thread(new TransferC2S(this));
		this.S2C.start();
		this.C2S.start();
	}

	public int RP()
	{
		if (this.connection.network.type == CrossNet.SIDE_SERVER) return this.connection.socket.getPort();
		else return this.connection.socket.getLocalPort();
	}

	public int LP()
	{
		if (this.connection.network.type == CrossNet.SIDE_SERVER) return this.socket.getLocalPort();
		else return this.socket.getPort();
	}

	public boolean running()
	{
		return this.running;
	}

	public void close()
	{
		CrossNet.LOG.info("Transfer close");
		this.running = false;
		this.connection.network.connection(this.RP(), this.LP(), null);
		try
		{
			CrossNet.LOG.info(
				"Transfer connection " +
					this.connection.socket.getRemoteSocketAddress() +
					" closing"
			);
			this.connection.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
		try
		{
			CrossNet.LOG.info(
				"Transfer connection " +
					this.socket.getRemoteSocketAddress() +
					" closing"
			);
			this.socket.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
	}

	public void exception(Throwable e)
	{
		if (this.running())
		{
			int rp = this.RP();
			int lp = this.LP();
			CrossNet.LOG.warning("Transfer " + lp + " - " + rp + " closed");
			CrossNet.LOG.log(Level.WARNING, null, e);
			this.close();
		}
	}
}
