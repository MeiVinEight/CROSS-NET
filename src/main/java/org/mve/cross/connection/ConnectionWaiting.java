package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.concurrent.Synchronized;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;
import org.mve.cross.transfer.TransferManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class ConnectionWaiting extends Synchronized
{
	private final NetworkManager network;
	private final int RP;
	private final int timeout;
	private final Queue<Socket> connection = new ConcurrentLinkedQueue<>();

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
			Socket client = this.connection.peek();
			if (client == null) return;
			Socket server;
			CrossNet.LOG.info("Connection waiting " + this.RP + " for " + client.getRemoteSocketAddress());
			try
			{
				String sip = NetworkManager.SERVER_IP;
				int sp = NetworkManager.SERVER_PORT;
				server = new Socket(/*NetworkManager.SERVER_IP, NetworkManager.SERVER_PORT*/);
				server.connect(new InetSocketAddress(sip, sp), this.timeout); // 5s timeout
				CrossNet.LOG.info("Connection to FRP " + sip + ":" + sp + " at " + server.getLocalPort());
				server.setSoTimeout(this.timeout);
			}
			catch (IOException e)
			{
				String sip = NetworkManager.SERVER_IP;
				int sp = NetworkManager.SERVER_PORT;
				CrossNet.LOG.severe("Cannot connect to FRP " + sip + ":" + sp);
				CrossNet.LOG.log(Level.SEVERE, null, e);
				this.connection.offer(client);
				return;
			}

			ConnectionManager cm = new ConnectionManager(this.network, server);
			Handshake handshake = new Handshake();
			handshake.listen = (short) this.RP;
			try
			{
				CrossNet.LOG.info("Handshake to " + server.getRemoteSocketAddress() + " at " + handshake.listen);
				cm.send(handshake);

				// Check port
				CrossNet.LOG.info("Handshake from " + server.getRemoteSocketAddress() + " at " + handshake.listen);
				Datapack pack = cm.receive();
				if (!(pack instanceof Handshake))
				{
					CrossNet.LOG.warning("Handshake required: " + server.getRemoteSocketAddress());
					cm.close();
					this.connection.offer(client);
					return;
				}
				pack.accept(cm);
				// Default no timeout at transfer
				server.setSoTimeout(0);

			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Handshake failed with " + server.getRemoteSocketAddress());
				CrossNet.LOG.log(Level.SEVERE, null, e);
				cm.close();
			}
		}
	}

	public void offer(Socket socket)
	{
		if (socket == null) return;
		this.connection.offer(socket);
		CrossNet.LOG.info(
			"Connection waiting for " +
			socket.getRemoteSocketAddress() +
			", " +
			this.connection.size() +
			" connection(s)"
		);
	}

	public void poll(ConnectionManager cm)
	{
		if (cm == null) return;
		Socket socket = this.connection.poll();
		if (socket == null)
		{
			cm.close();
			return;
		}

		CrossNet.LOG.info(
			"Transfer connection " +
			socket.getRemoteSocketAddress() +
			" - " +
			cm.socket.getRemoteSocketAddress() +
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
