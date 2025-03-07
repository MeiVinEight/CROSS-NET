package org.mve.cross.transfer;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TransferManager
{
	public final NetworkManager network;
	public final int UID;
	public final ConnectionManager connection;
	public final SocketChannel socket;
	public final InetSocketAddress address;
	private final int LP;
	private final int RP;
	public final Thread S2C;
	public final Thread C2S;
	private boolean running = true;

	public TransferManager(NetworkManager network, int id)
	{
		this.network = network;
		this.UID = id;
		ConnectionMapping mapping = this.network.connection(id);
		this.connection = mapping.server;
		this.socket = mapping.client;
		this.address = (InetSocketAddress) this.socket.socket().getRemoteSocketAddress();
		if (this.connection.network.type == CrossNet.SIDE_SERVER)
		{
			this.LP = this.socket.socket().getLocalPort();
			this.RP = this.connection.address.getPort();
		}
		else
		{
			this.LP = this.socket.socket().getPort();
			this.RP = this.connection.LP;
		}
		this.S2C = new Thread(new TransferS2C(this));
		this.C2S = new Thread(new TransferC2S(this));
		this.S2C.start();
		this.C2S.start();
	}

	public int RP()
	{
		return this.RP;
	}

	public int LP()
	{
		return this.LP;
	}

	public boolean running()
	{
		return this.running;
	}

	public void close()
	{
		boolean prev = false;
		synchronized (this)
		{
			if (this.running)
			{
				prev = this.running;
				this.running = false;
			}
		}
		if (!prev) return;
		CrossNet.LOG.info("Transfer close");
		ConnectionMapping mapping = this.network.connection(this.UID);
		this.network.free(this.UID);
		mapping.close();
	}

	public void exception(Throwable e)
	{
		if (this.running())
		{
			int rp = this.RP();
			int lp = this.LP();
			CrossNet.LOG.warning("Transfer " + lp + " - " + rp + " closed");
			// CrossNet.LOG.log(Level.WARNING, null, e);
			this.close();
		}
	}
}
