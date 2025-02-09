package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
import org.mve.cross.connection.ConnectionWaiting;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.Serialization;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Handshake extends Datapack
{
	public static final int ID = 0x00; // HANDSHAKE
	// Random generated uuid
	public static final UUID SIGNATURE = new UUID(-740609665978645947L, -8246978237028082185L);

	public long most;
	public long least;
	public short listen;

	public Handshake()
	{
		this.most = SIGNATURE.getMostSignificantBits();
		this.least = SIGNATURE.getLeastSignificantBits();
	}

	@Override
	public void accept(ConnectionManager conn)
	{
		if (!new UUID(this.most, this.least).equals(SIGNATURE))
		{
			CrossNet.LOG.warning("UNKNOWN CONNECTION: " + conn.socket.socket().getRemoteSocketAddress());
			conn.close();
			return;
		}
		// TODO Next generation
		InetSocketAddress addr = conn.address;
		if (this.listen == 0)
		{
			String info = "Communication connection from " + addr.getAddress().getHostAddress() + ":" + addr.getPort()
				+ " at " + conn.LP;
			CrossNet.LOG.info(info);
			conn.network.communication = conn;
			if (conn.network.type == CrossNet.SIDE_CLIENT)
			{
				for (Map.Entry<Integer, ConnectionMapping> entry : NetworkManager.MAPPING.entrySet())
				{
					int listenPort = entry.getKey();
					CrossNet.LOG.info("Communication listen " + listenPort);
					ConnectionWaiting waiting = new ConnectionWaiting(conn.network, listenPort, entry.getValue().timeout);
					conn.network.waiting[listenPort] = waiting;
					conn.network.synchronize.offer(waiting);
					Connection connl = new Connection();
					connl.LP = (short) listenPort;
					try
					{
						conn.network.communication.send(connl);
					}
					catch (IOException e)
					{
						CrossNet.LOG.warning("Communicate connection failed");
						CrossNet.LOG.log(Level.WARNING, null, e);
					}
				}
			}
		}
		else
		{
			// TODO Transfer handshake
			ConnectionWaiting waiting = conn.network.waiting[this.listen];
			if (waiting == null)
			{
				CrossNet.LOG.severe("NO CONNECTION AT " + this.listen);
				conn.close();
				return;
			}
			waiting.poll(conn);
		}
	}

	@Override
	public void read(ReadableByteChannel in) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(18);
		Serialization.transfer(in, buffer);
		buffer.flip();
		this.most = buffer.getLong();
		this.least = buffer.getLong();
		this.listen = buffer.getShort();
	}

	@Override
	public void write(WritableByteChannel out) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(18);
		buffer.putLong(this.most);
		buffer.putLong(this.least);
		buffer.putShort(this.listen);
		buffer.flip();
		Serialization.transfer(out, buffer);
	}

	@Override
	public int ID()
	{
		return Handshake.ID;
	}
}
