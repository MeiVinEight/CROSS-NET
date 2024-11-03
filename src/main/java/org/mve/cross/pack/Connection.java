package org.mve.cross.pack;

import org.mve.cross.*;

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
			int idx0 = (port >> 8) & 0xFF;
			int idx1 = (port >> 0) & 0xFF;
			if (network.server[idx0] == null) network.server[idx0] = new ServerSocket[256];
			try
			{
				network.server[idx0][idx1] = new ServerSocket(port);
				CrossNet.LOG.info("Listening at " + port);
			}
			catch (IOException e)
			{
				CrossNet.LOG.log(Level.SEVERE, null, e);
			}
		}
		else // SIDE_CLIENT
		{
			int localePort = NetworkManager.mapping(this.LP & 0xFFFF);
			CrossNet.LOG.info("New connection at " + this.LP + ", create transfer connection to " + localePort);
			Socket client;
			Socket server;
			try
			{
				client = new Socket("127.0.0.1", localePort);
			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Cannot connect to 127.0.0.1:" + localePort);
				CrossNet.LOG.log(Level.SEVERE, null, e);
				return;
			}
			try
			{
				server = new Socket(NetworkManager.SERVER_IP, NetworkManager.SERVER_PORT);
			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Cannot connect to " + NetworkManager.SERVER_IP + ":" + localePort);
				CrossNet.LOG.log(Level.SEVERE, null, e);
				try
				{
					client.close();
				}
				catch (IOException e1)
				{
					CrossNet.LOG.log(Level.SEVERE, null, e1);
				}
				return;
			}
			ConnectionManager cm = new ConnectionManager(conn.network, server);
			Handshake handshake = new Handshake();
			handshake.listen = this.LP;
			try
			{
				CrossNet.LOG.info("Handshake with " + server.getRemoteSocketAddress() + " at " + handshake.listen);
				cm.send(handshake);
			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Handshake failed with " + server.getRemoteSocketAddress());
				CrossNet.LOG.log(Level.SEVERE, null, e);
				return;
			}
			// TODO Dual-transfer
			// new Thread(new TransferS2C(cm, client)).start();
			// new Thread(new TransferC2S(cm, client)).start();
			TransferManager tm = new TransferManager(cm, client);
			conn.network.connection(tm.RP(), tm.LP(), tm);
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
