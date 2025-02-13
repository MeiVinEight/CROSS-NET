package org.mve.cross.pack;

import org.mve.cross.Configuration;
import org.mve.cross.CrossNet;
import org.mve.cross.connection.ConnectionID;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.Serialization;
import org.mve.cross.connection.ConnectionMapping;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class Connection extends Datapack
{
	public static final int ID = 0x05;

	public short RP; // Local port
	public int UID;

	@Override
	public void accept(ConnectionManager conn)
	{
		if (!conn.established())
		{
			CrossNet.LOG.severe("Connection: REQUIRED ESTABLISHED");
			return;
		}
		if (conn.network.type == CrossNet.SIDE_CLIENT)
		{
			int localPort = Configuration.mapping(this.RP);
			if (localPort == 0)
			{
				CrossNet.LOG.warning("Unknown connection RP: " + this.RP);
				return;
			}

			if (conn.network.connection(this.UID) != null)
			{
				CrossNet.LOG.warning("Connection already exists: " + this.UID);
				return;
			}

			ConnectionMapping mapping = new ConnectionMapping();
			conn.network.connection(this.UID, mapping);

			CrossNet.LOG.info("Connection at " + this.RP + ", create transfer connection to " + localPort);
			SocketChannel client = ConnectionManager.connect(new InetSocketAddress(localPort));
			if (client == null)
			{
				CrossNet.LOG.warning("Could not connect to " + localPort);
				return;
			}
			mapping.client = client;

			SocketAddress server = conn.address;
			ConnectionID cid = new ConnectionID();
			cid.server = server;
			cid.ID = this.UID;
			conn.network.waiting.offer(cid);
		}
		else
		{
			if (this.UID != 0)
			{
				// Transfer connection
				conn.network.waiting.poll(conn, this.UID);
				return;
			}

			// Communication connection
			if (conn.network.communication != null)
			{
				CrossNet.LOG.warning("Communication is in use");
				return;
			}
			CrossNet.LOG.info("Connection register to communication: " + conn.address);
			conn.network.communication = conn;
		}
	}

	@Override
	public void read(ReadableByteChannel in) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(6);
		Serialization.transfer(in, buffer);
		buffer.flip();
		this.RP = buffer.getShort();
		this.UID = buffer.getInt();
	}

	@Override
	public void write(WritableByteChannel out) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(6);
		buffer.putShort(this.RP);
		buffer.putInt(this.UID);
		buffer.flip();
		Serialization.transfer(out, buffer);
	}

	@Override
	public int ID()
	{
		return Connection.ID;
	}
}
