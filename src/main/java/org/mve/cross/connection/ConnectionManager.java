package org.mve.cross.connection;

import org.mve.cross.Configuration;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.ProtocolManager;
import org.mve.cross.Serialization;
import org.mve.cross.nio.DynamicArray;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Disconnect;
import org.mve.cross.pack.Handshake;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ConnectionManager
{
	public static final int STAT_CLOSED      = 0;
	public static final int STAT_CONNECTING  = 1;
	public static final int STAT_HANDSHAKE1  = 2;
	public static final int STAT_HANDSHAKE2  = 3;
	public static final int STAT_ESTABLISHED = 4;
	public static final int READ_OVERED = 0;
	public static final int READ_LENGTH = 1;
	public static final int READ_DATA   = 2;
	public final NetworkManager network;
	public SocketChannel socket;
	private final ReentrantLock lock = new ReentrantLock();
	private final ReentrantLock receive = new ReentrantLock();
	public InetSocketAddress address;
	// local port
	public int LP;
	private int status = ConnectionManager.STAT_CLOSED;
	private final DynamicArray RB = new DynamicArray(Configuration.DEFAULT_BUFFER_SIZE);
	private final DynamicArray LB = new DynamicArray(4);
	private final DynamicArray WB = new DynamicArray(Configuration.DEFAULT_BUFFER_SIZE);
	private int RS = ConnectionManager.READ_OVERED;
	public boolean blocking = true;
	private boolean reading = false;
	private boolean writing = false;

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
			this.receive.lock();
			if (this.status != ConnectionManager.STAT_CLOSED) throw new AlreadyConnectedException();
			this.reading = false;
			this.writing = false;
			this.status = ConnectionManager.STAT_CONNECTING;
			this.socket = channel;
			this.address = (InetSocketAddress) channel.getRemoteAddress();
			this.LP = ((InetSocketAddress) this.socket.getLocalAddress()).getPort();

			this.status = ConnectionManager.STAT_HANDSHAKE1;
			this.writing = true;
			Handshake handshake = new Handshake();
			this.send(handshake);

			this.status = ConnectionManager.STAT_HANDSHAKE2;
			this.reading = true;
			this.finish();
		}
		finally
		{
			this.receive.unlock();
		}
	}

	public boolean finish() throws IOException
	{
		try
		{
			this.receive.lock();

			if (this.status > ConnectionManager.STAT_HANDSHAKE2) return true;
			if (this.status == ConnectionManager.STAT_CLOSED) throw new ClosedChannelException();
			if (this.status < ConnectionManager.STAT_HANDSHAKE2) throw new NoConnectionPendingException();

			Datapack datapack = this.receive();
			while (this.blocking && (datapack == null)) this.receive();
			if (datapack == null) return false;
			if (!(datapack instanceof Handshake)) throw new ConnectException("Wrong Handshake");
			if (!((Handshake) datapack).verify()) throw new ConnectException("Wrong Signature");
			this.status = ConnectionManager.STAT_ESTABLISHED;
		}
		catch (IOException | RuntimeException e)
		{
			this.status = ConnectionManager.STAT_CLOSED;
			this.socket = null;
			this.address = null;
			this.LP = 0;
			throw e;
		}
		finally
		{
			this.receive.unlock();
		}
		return true;
	}

	public Datapack receive() throws IOException
	{
		Datapack pack = null;
		try
		{
			this.receive.lock();

			if (!this.reading) throw new ClosedChannelException();
			switch (this.RS)
			{
				case ConnectionManager.READ_OVERED:
				{
					// Clear buffer and limit by 4 for read length
					this.RB.clear();
					this.RB.limit(4);
					this.RS = ConnectionManager.READ_LENGTH;
				}
				case ConnectionManager.READ_LENGTH:
				{
					// Read length
					if (this.blocking) Serialization.transfer((ReadableByteChannel) this.socket, this.RB);
					else
					{
						int read = this.RB.read(this.socket);
						if (read == -1) this.close();
					}
					if (this.RB.remaining() > 0) break;
					// Decode length
					RB.flip();
					int length = this.RB.getInt();
					// Clear read buffer and limit by length for read pack data
					this.RB.clear();
					this.RB.acquire(length);
					this.RB.limit(length);
					this.RS = ConnectionManager.READ_DATA;
				}
				case ConnectionManager.READ_DATA:
				{
					// Read pack data
					if (this.blocking) Serialization.transfer((ReadableByteChannel) this.socket, this.RB);
					else
					{
						int read = this.RB.read(this.socket);
						if (read == -1) this.close();
					}
					if (this.RB.remaining() > 0) break;
					this.RB.flip();
					// Get pack id
					byte id = RB.get();
					if (id >= ProtocolManager.CONSTRUCTOR.length)
					{
						throw new IOException("UNKNOWN DATAPACK ID: " + id);
					}
					// Construct datapack
					Datapack tmp = ProtocolManager.CONSTRUCTOR[id].invoke();
					tmp.read(RB);
					pack = tmp;
					this.RS = ConnectionManager.READ_OVERED;
				}
			}
		}
		finally
		{
			this.receive.unlock();
		}

		if (pack != null && pack.ID() == Disconnect.ID)
		{
			pack.accept(this);
			pack = null;
		}
		return pack;
	}

	public void send(Datapack pack) throws IOException
	{
		try
		{
			this.lock.lock();

			if (!this.writing) throw new ClosedChannelException();
			// Write pack
			this.WB.clear();
			// Write pack id
			this.WB.put((byte) pack.ID());
			// Write pack data
			pack.write(this.WB);
			this.WB.flip();

			// Write pack length
			this.LB.clear();
			this.LB.putInt(this.WB.remaining());
			this.LB.flip();

			Serialization.transfer((WritableByteChannel) this.socket, this.LB);
			Serialization.transfer((WritableByteChannel) this.socket, this.WB);
		}
		finally
		{
			this.lock.unlock();
		}
	}

	public void shutdown(boolean reading) throws IOException
	{
		try
		{
			this.lock.lock();
			if (reading) this.reading = false;
			else
			{
				if (this.writing)
				{
					Disconnect dis = new Disconnect();
					// this.send(dis);
					this.writing = false;
				}
			}
		}
		finally
		{
			this.lock.unlock();
		}
	}

	public void close()
	{
		try
		{
			this.receive.lock();

			if (this.status == ConnectionManager.STAT_CLOSED) return;

			try
			{
				this.shutdown(false);
			}
			catch (IOException ignored)
			{
				this.writing = false;
			}

			this.shutdown(true);
			if (this.reading) return;

			this.socket.close();
			this.status = ConnectionManager.STAT_CLOSED;
		}
		catch (IOException e)
		{
			CrossNet.LOG.warning("Close connection failed " + this.address);
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
		finally
		{
			this.receive.unlock();
		}
	}

	public boolean established()
	{
		return this.status == ConnectionManager.STAT_ESTABLISHED;
	}

	public SelectionKey register(Selector selector, int interest, Object attachement) throws ClosedChannelException
	{
		if (this.status != ConnectionManager.STAT_ESTABLISHED)
		{
			throw new ClosedChannelException();
		}
		return this.socket.register(selector, interest, attachement);
	}
}
