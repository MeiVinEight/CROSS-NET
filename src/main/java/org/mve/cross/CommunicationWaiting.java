package org.mve.cross;

import org.mve.cross.concurrent.Synchronize;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.pack.Connection;
import org.mve.cross.pack.Listen;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.logging.Level;

public class CommunicationWaiting extends Synchronize
{
	private static final int WAITING     = 0;
	private static final int CONNECTING  = 1;
	private static final int FINISHING   = 2;
	private static final int MANAGING    = 3;
	private static final int HANDSHAKING = 4;
	private static final int REGISTERING = 5;
	private final NetworkManager network;
	private final long period;
	private long timestamp;
	private int status = CommunicationWaiting.WAITING;
	private SocketChannel channel;
	private ConnectionManager connection;

	public CommunicationWaiting(NetworkManager network, long period)
	{
		if (period <= 0) period = 1;
		this.network = network;
		this.period = period;
		super.period = this.period;
	}

	@Override
	public void run()
	{
		if (this.network.type == CrossNet.SIDE_SERVER)
		{
			this.cancel();
			return;
		}


		super.period = 1;
		try
		{
			switch (this.status)
			{
				case CommunicationWaiting.WAITING:
				if (this.network.communication != null) break;

				this.status = CommunicationWaiting.CONNECTING;
				case CommunicationWaiting.CONNECTING:
				InetAddress addr = InetAddress.getByName(Configuration.SERVER_ADDRESS);
				CrossNet.LOG.info("Connection " + addr.getCanonicalHostName() + ":" + Configuration.SERVER_PORT);
				this.channel = SocketChannel.open();
				this.channel.configureBlocking(false);
				this.channel.connect(new InetSocketAddress(addr, Configuration.SERVER_PORT));
				this.timestamp = System.currentTimeMillis();

				this.status = CommunicationWaiting.FINISHING;
				case CommunicationWaiting.FINISHING:
				if (System.currentTimeMillis() > (this.timestamp + Configuration.COMMUNICATION_CONNECT))
				{
					CrossNet.LOG.warning("Connection timeout");
					this.reset();
					break;
				}
				if (!this.channel.finishConnect()) break;
				this.status = CommunicationWaiting.MANAGING;
				case CommunicationWaiting.MANAGING:
				this.connection = new ConnectionManager(this.network);
				this.connection.blocking = false;
				CrossNet.LOG.info("Connection creating");
				this.connection.connect(this.channel);

				this.status = CommunicationWaiting.HANDSHAKING;
				case CommunicationWaiting.HANDSHAKING:
				if (System.currentTimeMillis() > (this.timestamp + Configuration.COMMUNICATION_CONNECT))
				{
					CrossNet.LOG.warning("Connection timeout");
					this.reset();
					break;
				}
				if (!this.connection.finish()) break;

				this.status = CommunicationWaiting.REGISTERING;
				case CommunicationWaiting.REGISTERING:
				this.connection.send(new Connection());
				for (Map.Entry<Integer, AddressMapping> entry : Configuration.MAPPING.entrySet())
				{
					int listenPort = entry.getKey();
					CrossNet.LOG.info("Communication listening on " + listenPort);
					Listen listen = new Listen();
					listen.ON = (short) listenPort;
					this.connection.send(listen);
				}
				Communication com = new Communication(this.network, this.connection, 0);
				com.register(this.network);
				this.network.communication = com;
				this.channel = null;
				this.connection = null;
				this.status = CommunicationWaiting.WAITING;
			}
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
			this.reset();
		}
	}

	private void reset()
	{
		super.period = this.period;
		this.status = CommunicationWaiting.WAITING;
		if (this.channel != null)
		{
			try
			{
				this.channel.close();
			}
			catch (IOException e)
			{
				CrossNet.LOG.log(Level.WARNING, null, e);
			}
			this.channel = null;
		}
		if (this.connection != null)
		{
			this.connection.close();
			this.connection = null;
		}
	}
}
