package org.mve.cross;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

public class Communication implements Runnable
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
			if (this.network.communication == null)
			{
				// Waiting 5s
				LockSupport.parkNanos(NetworkManager.COMMUNICATION_CONNECT * 1_000_000L);
				if (this.network.type == CrossNet.SIDE_CLIENT)
				{
					SocketChannel channel = null;
					try
					{
						InetAddress addr = InetAddress.getByName(NetworkManager.SERVER_IP);
						CrossNet.LOG.info("Communication to " + addr + ":" + NetworkManager.SERVER_PORT);
						channel = SocketChannel.open();
						channel.configureBlocking(false);
						channel.connect(new InetSocketAddress(addr, NetworkManager.SERVER_PORT));
						while (!channel.finishConnect()) Thread.yield();
						ConnectionManager conn = new ConnectionManager(this.network, channel);
						CrossNet.LOG.info("Communication handshake");
						Handshake handshake = new Handshake();
						conn.send(handshake);
						Datapack datapack = conn.receive();
						if (!(datapack instanceof Handshake))
						{
							conn.close();
						}
						else
						{
							datapack.accept(conn);
							if (!conn.socket.isConnected())
							{
								this.network.communication = null;
							}
						}
						if (this.network.communication == null)
						{
							throw new IOException("Connection refused");
						}
					}
					catch (IOException e)
					{
						CrossNet.LOG.warning("Communication failed");
						CrossNet.LOG.log(Level.WARNING, null, e);
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
				}
				Thread.yield();
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
