package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.ProtocolManager;
import org.mve.cross.Serialization;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ConnectionManager
{
	public static final int STAT_CLOSED      = 0;
	public static final int STAT_CONNECTING  = 1;
	public static final int STAT_HANDSHAKE1  = 2;
	public static final int STAT_HANDSHAKE2  = 3;
	public static final int STAT_ESTABLISHED = 4;
	public static final int STAT_CLOSING     = 5;
	private long ID = 0;
	public final NetworkManager network;
	public SocketChannel socket;
	private final ReentrantLock lock = new ReentrantLock();
	public InetSocketAddress address;
	// local port
	public int LP;
	private int status = ConnectionManager.STAT_CLOSED;

	public ConnectionManager(NetworkManager network)
	{
		this.network = network;
	}

	public ConnectionManager(NetworkManager network, SocketChannel socket) throws IOException
	{
		this(network);
		this.connect(socket);
	}

	public void connect(SocketChannel channel) throws IOException
	{
		try
		{
			this.status = ConnectionManager.STAT_CONNECTING;
			this.socket = channel;
			this.address = (InetSocketAddress) channel.getRemoteAddress();
			this.LP = ((InetSocketAddress) this.socket.getLocalAddress()).getPort();

			this.status = ConnectionManager.STAT_HANDSHAKE1;
			Handshake handshake = new Handshake();
			this.send(handshake);

			this.status = ConnectionManager.STAT_HANDSHAKE2;
			Datapack pack = this.receive();
			if (!(pack instanceof Handshake))
			{
				throw new ConnectException("Wrong Handshake");
			}
			handshake = (Handshake) pack;
			if (!new UUID(handshake.most, handshake.least).equals(Handshake.SIGNATURE))
			{
				throw new ConnectException("Wrong Signature");
			}

			this.status = ConnectionManager.STAT_ESTABLISHED;
		}
		finally
		{
			if (this.status != ConnectionManager.STAT_ESTABLISHED)
			{
				this.status = ConnectionManager.STAT_CLOSED;
				this.socket = null;
				this.address = null;
				this.LP = 0;
			}
		}
	}

	public Datapack receive() throws IOException
	{
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
		if (this.status == ConnectionManager.STAT_CLOSED) return;

		this.status = ConnectionManager.STAT_CLOSING;
		try
		{
			this.socket.close();
			this.status = ConnectionManager.STAT_CLOSED;
		}
		catch (IOException e)
		{
			CrossNet.LOG.warning("Close connection failed " + this.address);
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
	}

	public int status()
	{
		return this.status;
	}

	public boolean established()
	{
		return this.status == ConnectionManager.STAT_ESTABLISHED;
	}

	public void ID(long id)
	{
		if (this.ID == 0)
		{
			this.ID = id;
		}
	}

	public long ID()
	{
		return this.ID;
	}

	public static SocketChannel connect(SocketAddress address)
	{
		try
		{
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(address);
			while (!channel.finishConnect()) Thread.yield();
			return channel;
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
		return null;
	}
}
