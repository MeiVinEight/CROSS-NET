package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.concurrent.Synchronized;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;
import org.mve.cross.transfer.TransferManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class ConnectionWaiting extends Synchronized
{
	private final NetworkManager network;
	private final int RP;
	private final int timeout;
	private final Queue<SocketChannel> connection = new ConcurrentLinkedQueue<>();

	public ConnectionWaiting(NetworkManager network, int rp, int timeout)
	{
		this.network = network;
		this.RP = rp;
		this.timeout = timeout;
	}

	@Override
	public void run()
	{
		if (this.network.type == CrossNet.SIDE_CLIENT)
		{
			SocketChannel client = this.connection.peek();
			if (client == null) return;
			SocketChannel server;
			CrossNet.LOG.info("Connection waiting " + this.RP + " for " + client.socket().getRemoteSocketAddress());
			try
			{
				String sip = NetworkManager.SERVER_IP;
				int sp = NetworkManager.SERVER_PORT;
				// server = new Socket(/*NetworkManager.SERVER_IP, NetworkManager.SERVER_PORT*/);
				server = SocketChannel.open();
				server.configureBlocking(false);
				server.connect(new InetSocketAddress(sip, sp)); // 5s timeout
				while (!server.finishConnect()) Thread.yield();
				CrossNet.LOG.info("Connection to FRP " + sip + ":" + sp + " at " + server.socket().getLocalPort());
			}
			catch (IOException e)
			{
				String sip = NetworkManager.SERVER_IP;
				int sp = NetworkManager.SERVER_PORT;
				CrossNet.LOG.severe("Cannot connect to FRP " + sip + ":" + sp);
				CrossNet.LOG.log(Level.SEVERE, null, e);
				this.offer(client);
				return;
			}

			ConnectionManager cm = new ConnectionManager(this.network, server);
			Handshake handshake = new Handshake();
			handshake.listen = (short) this.RP;
			try
			{
				CrossNet.LOG.info("Handshake to " + cm.address + " at " + handshake.listen);
				cm.send(handshake);

				// Check port
				CrossNet.LOG.info("Handshake from " + cm.address + " at " + handshake.listen);
				Datapack pack = cm.receive();
				if (!(pack instanceof Handshake))
				{
					CrossNet.LOG.warning("Handshake required: " + cm.address);
					cm.close();
					this.offer(client);
					return;
				}
				pack.accept(cm);
			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Handshake failed with " + cm.address);
				CrossNet.LOG.log(Level.SEVERE, null, e);
				cm.close();
			}
		}
	}

	public void offer(SocketChannel socket)
	{
		if (socket == null) return;
		this.connection.offer(socket);
		CrossNet.LOG.info(
			"Connection waiting for " +
			socket.socket().getRemoteSocketAddress() +
			", " +
			this.connection.size() +
			" connection(s)"
		);
	}

	public void poll(ConnectionManager cm)
	{
		if (cm == null) return;
		SocketChannel socket = this.connection.poll();
		if (socket == null)
		{
			cm.close();
			return;
		}

		CrossNet.LOG.info(
			"Transfer connection " +
			socket.socket().getRemoteSocketAddress() +
			" - " +
			cm.address +
			", " +
			this.connection.size() +
			" connection(s)"
		);
		TransferManager transfer = new TransferManager(cm, socket);
		this.network.connection(transfer.RP(), transfer.LP(), transfer);
	}

	public void close()
	{
		this.cancel();
	}
}
