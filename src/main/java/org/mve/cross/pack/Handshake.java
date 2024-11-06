package org.mve.cross.pack;

import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionWaiting;
import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;

public class Handshake extends Datapack
{
	public static final int ID = 0x01; // HANDSHAKE
	// Random generated uuid
	public static final UUID SIGNATURE = new UUID(2684175183751368306L, -4907171764388432698L);

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
			CrossNet.LOG.warning("UNKNOWN CONNECTION: " + conn.socket.getRemoteSocketAddress());
			conn.close();
			return;
		}
		// TODO Next generation
		InetSocketAddress addr = (InetSocketAddress) conn.socket.getRemoteSocketAddress();
		if (conn.network.status() == NetworkManager.NETWORK_STAT_COMMUNICATION)
		{
			String info = "Communication connection from " + addr.getAddress().getHostAddress() + ":" + addr.getPort()
				+ " at " + conn.socket.getLocalPort();
			CrossNet.LOG.info(info);
		}
		else
		{
			// TODO Transfer handshake
			CrossNet.LOG.info("Transfer connection with " + addr + " at " + this.listen);
			if (conn.network.type == CrossNet.SIDE_SERVER)
			{
				ConnectionWaiting waiting = conn.network.waiting[this.listen];
				if (waiting == null)
				{
					CrossNet.LOG.severe("NO CONNECTION AT " + this.listen);
					conn.close();
					return;
				}
				waiting.poll(conn);
			}
			/*
			else // SIDE_CLIENT
			{
				// TODO Nothing to do
			}
			*/
		}
	}

	@Override
	public void read(InputStream in) throws IOException
	{
		this.most = Serialization.R8(in);
		this.least = Serialization.R8(in);
		this.listen = Serialization.R2(in);
	}

	@Override
	public void write(OutputStream out) throws IOException
	{
		Serialization.W8(out, this.most);
		Serialization.W8(out, this.least);
		Serialization.W2(out, this.listen);
	}

	@Override
	public int ID()
	{
		return Handshake.ID;
	}
}
