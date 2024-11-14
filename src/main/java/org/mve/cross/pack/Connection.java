package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMonitor;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.Serialization;
import org.mve.cross.connection.ConnectionWaiting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class Connection extends Datapack
{
	public static final int ID = 0x02;

	public short LP; // Local  port

	@Override
	public void accept(ConnectionManager conn)
	{
		// Apply a new connection
		NetworkManager network = conn.network;
		if (network.type == CrossNet.SIDE_SERVER)
		{
			int port = (this.LP & 0xFFFF);
			try
			{
				int timeout = NetworkManager.timeout(port);
				if (network.waiting[port] == null) network.waiting[port] = new ConnectionWaiting(network, port, timeout);
				if (network.server[port] == null)
				{
					network.server[port] = new ConnectionMonitor(conn.network, new ServerSocket(port));
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
			int localePort = NetworkManager.mapping(this.LP & 0xFFFF);
			CrossNet.LOG.info("Connection at " + this.LP + ", create transfer connection to " + localePort);
			Socket client; // Connection with FRP endpoint
			try
			{
				// TODO Use properties ip
				client = new Socket("127.0.0.1", localePort);
				CrossNet.LOG.info("Connection to endpoint 127.0.0.1:" + localePort + " at " + client.getLocalPort());
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
	public void read(InputStream in) throws IOException
	{
		this.LP = Serialization.R2(in);
	}

	@Override
	public void write(OutputStream out) throws IOException
	{
		Serialization.W2(out, this.LP);
	}

	@Override
	public int ID()
	{
		return Connection.ID;
	}
}
