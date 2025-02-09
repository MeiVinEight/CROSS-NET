package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.ProtocolManager;
import org.mve.cross.Serialization;
import org.mve.cross.pack.Datapack;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ConnectionManager
{
	public final NetworkManager network;
	public final SocketChannel socket;
	private final ReentrantLock lock = new ReentrantLock();
	public final InetSocketAddress address;
	// local port
	public final int LP;

	public ConnectionManager(NetworkManager network, SocketChannel socket)
	{
		this.network = network;
		this.socket = socket;
		SocketAddress address;
		try
		{
			address = socket.getRemoteAddress();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
			address = socket.socket().getRemoteSocketAddress();
		}
		this.address = (InetSocketAddress) address;
		int lp;
		try
		{
			lp = ((InetSocketAddress) this.socket.getLocalAddress()).getPort();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
			lp = this.socket.socket().getLocalPort();
		}
		this.LP = lp;
	}

	public Datapack receive() throws IOException
	{
		// InputStream in = this.socket.getInputStream();
		// this.lock.lock();
		// byte id = Serialization.R1(in);
		ByteBuffer buffer = ByteBuffer.allocateDirect(1);
		Serialization.transfer((ReadableByteChannel) this.socket, buffer);
		buffer.flip();
		byte id = buffer.get();
		if (id >= ProtocolManager.CONSTRUCTOR.length)
		{
			throw new IOException("UNKNOWN DATAPACK ID: " + id);
		}
		Datapack pack = ProtocolManager.CONSTRUCTOR[id].invoke();
		pack.read(this.socket);
		// this.lock.unlock();
		return pack;
	}

	public void send(Datapack pack) throws IOException
	{
		// OutputStream out = this.socket.getOutputStream();
		this.lock.lock();
		// Serialization.W1(out, (byte) pack.ID());
		ByteBuffer buffer = ByteBuffer.allocateDirect(1);
		buffer.put((byte) pack.ID());
		buffer.flip();
		Serialization.transfer((WritableByteChannel) this.socket, buffer);
		pack.write(this.socket);
		this.lock.unlock();
	}

	public void close()
	{
		try
		{
			this.socket.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
	}
}
