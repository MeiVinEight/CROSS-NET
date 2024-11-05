package org.mve.cross.pack;

import org.mve.cross.ConnectionManager;
import org.mve.cross.ConnectionMonitor;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.Serialization;
import org.mve.cross.TransferManager;

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
			CrossNet.LOG.info("Communication listen " + this.LP);
			int port = (this.LP & 0xFFFF);
			try
			{
				ServerSocket ss = new ServerSocket(port);
				network.server[port] = new ConnectionMonitor(conn.network, ss);
				new Thread(network.server[port]).start();
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
			Socket server; // Connection with FRP server
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

			try
			{
				String sip = NetworkManager.SERVER_IP;
				int sp = NetworkManager.SERVER_PORT;
				server = new Socket(NetworkManager.SERVER_IP, NetworkManager.SERVER_PORT);
				CrossNet.LOG.info("Connection to FRP " + sip + ":" + sp + " at " + server.getLocalPort());
			}
			catch (IOException e)
			{
				CrossNet.LOG.severe("Cannot connect to FRP " + NetworkManager.SERVER_IP + ":" + NetworkManager.SERVER_PORT);
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

				// Check port
				Datapack pack = cm.receive();
				if (!(pack instanceof Handshake))
				{
					CrossNet.LOG.warning("Handshake required: " + server.getRemoteSocketAddress());
					cm.close();
					client.close();
					return;
				}
				pack.accept(cm);
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
