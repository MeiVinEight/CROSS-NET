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
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

public class Connection extends Datapack
{
	public static final int ID = 0x05;
	public static final short TYPE_COMMUNICATION = 0;
	public static final short TYPE_CONNECTION = 1;
	public static final short TYPE_CLOSE = 2;

	public short RP; // Local port
	public short type;
	public int UID;

	public Connection()
	{
	}

	@Override
	public void accept(ConnectionManager conn)
	{
		if (!conn.established())
		{
			CrossNet.LOG.severe("Connection not established");
			return;
		}

		switch (this.type)
		{
			case TYPE_COMMUNICATION:
			{
				// Communication connection
				if (conn.network.communication != null)
				{
					CrossNet.LOG.warning("Communication is in use");
					return;
				}
				Communication com = new Communication(conn.network, conn, this.UID);
				try
				{
					com.register(conn.network);
					conn.network.communication = com;
				}
				catch (IOException e)
				{
					CrossNet.LOG.log(Level.WARNING, null, e);
				}
				break;
			}
			case TYPE_CONNECTION:
			{
				if (conn.network.type == CrossNet.SIDE_CLIENT)
				{
					int localPort = Configuration.mapping(this.RP);
					boolean failed = true;
					Connection signal = new Connection();
					signal.type = Connection.TYPE_CLOSE;
					signal.UID = this.UID;

					CONNECTING:
					{
						if (localPort == 0)
						{
							CrossNet.LOG.warning("Unknown connection RP: " + this.RP);
							break CONNECTING;
						}

						if (conn.network.connection(this.UID) != null)
						{
							CrossNet.LOG.warning("Connection already exists: " + this.UID);
							break CONNECTING;
						}

						ConnectionMapping mapping = conn.network.mapping(this.UID);

						SocketChannel client = ConnectionManager.connect(new InetSocketAddress(localPort));
						if (client == null)
						{
							CrossNet.LOG.warning("Could not connect to " + localPort);
							mapping.close();
							break CONNECTING;
						}
						failed = false;
						mapping.client = client;

						ConnectionID cid = new ConnectionID();
						cid.server = conn.address;
						cid.ID = this.UID;
						conn.network.waiting.offer(cid);
					}
					if (failed)
					{
						Communication com = conn.network.communication;
						if (com != null)
						{
							try
							{
								com.send(signal);
							}
							catch (IOException e)
							{
								CrossNet.LOG.log(Level.WARNING, "Could not send signal close", e);
							}
						}
					}
				}
				else
				{
					conn.network.waiting.poll(conn, this.UID);
				}
				break;
			}
			case TYPE_CLOSE:
			{
				ConnectionMapping mapping = conn.network.connection(this.UID);
				if (mapping != null) mapping.close();
				break;
			}
		}
	}

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
		this.RP = buffer.getShort();
		this.type = buffer.getShort();
		this.UID = buffer.getInt();
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
		buffer.putShort(this.RP);
		buffer.putShort(this.type);
		buffer.putInt(this.UID);
	}

	@Override
	public int ID()
	{
		return Connection.ID;
	}
}
