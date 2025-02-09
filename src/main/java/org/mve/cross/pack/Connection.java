package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMonitor;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.Serialization;
import org.mve.cross.connection.ConnectionWaiting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;

public class Connection extends Datapack
{
	public static final int ID = 0x01;

	public short LP; // Local port

	@Override
	public void accept(ConnectionManager conn)
	{
		// Apply a new connection
		NetworkManager network = conn.network;
		if (network.type == CrossNet.SIDE_SERVER)
		{
			// Create server listen on LP
			int port = (this.LP & 0xFFFF);
			try
			{
				int timeout = NetworkManager.timeout(port);
				if (network.waiting[port] == null) network.waiting[port] = new ConnectionWaiting(network, port, timeout);
				if (network.server[port] == null)
				{
					ServerSocketChannel channel = ServerSocketChannel.open();
					// channel.configureBlocking(false);
					channel.bind(new InetSocketAddress(port));
					network.server[port] = new ConnectionMonitor(conn.network, channel);
					new Thread(network.server[port]).start();
				}
			}
			catch (IOException e)
			{
				CrossNet.LOG.log(Level.SEVERE, null, e);
			}
		}
		else // SIDE_CLIENT
		{
			// A new connection accepted at LP on server
			int localePort = NetworkManager.mapping(this.LP & 0xFFFF);
			CrossNet.LOG.info("Connection at " + this.LP + ", create transfer connection to " + localePort);
			SocketChannel client; // Connection with FRP endpoint
			try
			{
				// TODO Use properties ip
				// client = new Socket("127.0.0.1", localePort);
				client = SocketChannel.open();
				client.configureBlocking(false);
				client.connect(new InetSocketAddress(localePort));
				while (!client.finishConnect()) Thread.yield();
				int clp = client.socket().getLocalPort();
				CrossNet.LOG.info("Connection to endpoint 127.0.0.1:" + localePort + " at " + clp);
			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Cannot connect to endpoint 127.0.0.1:" + localePort);
				CrossNet.LOG.log(Level.SEVERE, null, e);
				return;
			}

			conn.network.waiting[this.LP].offer(client);
		}
	}

	@Override
	public void read(ReadableByteChannel in) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);
		Serialization.transfer(in, buffer);
		buffer.flip();
		this.LP = buffer.getShort();
	}

	@Override
	public void write(WritableByteChannel out) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);
		buffer.putShort(this.LP);
		buffer.flip();
		Serialization.transfer(out, buffer);
	}

	@Override
	public int ID()
	{
		return Connection.ID;
	}
}
