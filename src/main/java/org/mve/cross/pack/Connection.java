package org.mve.cross.pack;

import org.mve.cross.Communication;
import org.mve.cross.Configuration;
import org.mve.cross.CrossNet;
import org.mve.cross.connection.ConnectionID;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

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

			ConnectionMapping mapping = conn.network.mapping(this.UID);

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
			Communication com = new Communication(conn.network, conn);
			try
			{
				com.register(conn.network);
				conn.network.communication = com;
			}
			catch (ClosedChannelException e)
			{
				CrossNet.LOG.log(Level.WARNING, null, e);
			}
		}
	}

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
		this.RP = buffer.getShort();
		this.UID = buffer.getInt();
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
		buffer.putShort(this.RP);
		buffer.putInt(this.UID);
	}

	@Override
	public int ID()
	{
		return Connection.ID;
	}
}
