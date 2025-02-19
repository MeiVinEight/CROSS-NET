package org.mve.cross;

import org.mve.cross.concurrent.Synchronize;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.pack.Connection;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Listen;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

public class Communication extends Synchronize
{
	private final NetworkManager network;

	public Communication(NetworkManager network)
	{
		this.network = network;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Communication");
		while (this.network.status() == NetworkManager.NETWORK_STAT_RUNNING)
		{
			Thread.yield();
			if (this.network.communication == null)
			{
				if (this.network.type == CrossNet.SIDE_CLIENT)
				{
					SocketChannel channel = null;
					InetAddress addr = null;
					ConnectionManager cm;
					try
					{
						addr = InetAddress.getByName(Configuration.SERVER_ADDRESS);
						CrossNet.LOG.info("Connect to " + addr + ":" + Configuration.SERVER_PORT + " for communication");
						channel = SocketChannel.open();
						channel.configureBlocking(false);
						channel.connect(new InetSocketAddress(addr, Configuration.SERVER_PORT));
						while (!channel.finishConnect()) Thread.yield();
						cm = new ConnectionManager(this.network, channel);
						cm.send(new Connection());

						for (Map.Entry<Integer, AddressMapping> entry : Configuration.MAPPING.entrySet())
						{
							int listenPort = entry.getKey();
							CrossNet.LOG.info("Communication listen " + listenPort);
							Listen listen = new Listen();
							listen.ON = (short) listenPort;
							try
							{
								cm.send(listen);
							}
							catch (IOException e)
							{
								CrossNet.LOG.warning("Communicate connection failed");
								CrossNet.LOG.log(Level.WARNING, null, e);
							}
						}
						cm.blocking = false;
					}
					catch (IOException e)
					{
						CrossNet.LOG.warning("Communication failed");
						CrossNet.LOG.log(Level.WARNING, null, e);
						cm = null;
						if (channel != null && channel.isConnected())
						{
							try
							{
								channel.close();
							}
							catch (IOException ex)
							{
								CrossNet.LOG.warning("Cannot close channel");
								CrossNet.LOG.log(Level.WARNING, null, ex);
							}
						}
					}
					this.network.communication = cm;
					CrossNet.LOG.info("Communication to " + addr + ":" + Configuration.SERVER_PORT);
				}
				if ((this.network.communication == null) && (this.network.type == CrossNet.SIDE_CLIENT))
				{
					// Waiting 5s
					LockSupport.parkNanos(Configuration.COMMUNICATION_CONNECT * 1_000_000L);
				}
				// CrossNet.LOG.info("Communication is null");
				continue;
			}

			Datapack datapack;
			try
			{
				datapack = this.network.communication.receive();
			}
			catch (IOException e)
			{
				CrossNet.LOG.warning("Communication " + this.network.communication.address + " closed");
				CrossNet.LOG.log(Level.WARNING, null, e);
				this.network.communication.close();
				this.network.communication = null;
				continue;
			}
			if (datapack == null) continue;
			try
			{
				datapack.accept(this.network.communication);
			}
			catch (Throwable e)
			{
				CrossNet.LOG.log(Level.WARNING, null, e);
			}
		}
		this.network.communication.network.close();
	}
}
