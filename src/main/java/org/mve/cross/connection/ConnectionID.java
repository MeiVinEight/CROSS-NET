package org.mve.cross.connection;

import org.mve.cross.NetworkManager;
import org.mve.cross.pack.Connection;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class ConnectionID
{
	public static final int CLOSED    = 0;
	public static final int CLIENT    = 1;
	public static final int FINISH1   = 2;
	public static final int SERVER    = 3;
	public static final int FINISH2   = 4;
	public static final int HANDSHAKE = 5;
	public static final int FINISH3   = 6;

	public SocketAddress locale;
	public SocketAddress remote;
	public SocketChannel client;
	public SocketChannel server;
	public ConnectionManager connection;
	public int status = ConnectionID.CLOSED;
	public int ID;

	public boolean connect(NetworkManager network, ConnectionMapping mapping) throws IOException
	{
		boolean continu = true;
		switch (this.status)
		{
			case ConnectionID.CLOSED:
			{
				this.client = null;
				this.server = null;
				this.connection = null;
				if (mapping.client != null)
				{
					mapping.client.close();
					mapping.client = null;
				}
				if (mapping.server != null)
				{
					mapping.server.close();
					mapping.server = null;
				}
				this.status = ConnectionID.CLIENT;
			}
			case ConnectionID.CLIENT:
			{
				if (this.client == null) this.client = SocketChannel.open();
				this.client.configureBlocking(false);
				this.client.connect(this.locale);
				this.status = ConnectionID.FINISH1;
			}
			case ConnectionID.FINISH1:
			{
				if (!this.client.finishConnect()) break;
				mapping.client = this.client;
				this.status = ConnectionID.SERVER;
			}
			case ConnectionID.SERVER:
			{
				if (this.server == null) this.server = SocketChannel.open();
				this.server.configureBlocking(false);
				this.server.connect(this.remote);
				this.status = ConnectionID.FINISH2;
			}
			case ConnectionID.FINISH2:
			{
				if (!this.server.finishConnect()) break;
				this.status = ConnectionID.HANDSHAKE;
			}
			case ConnectionID.HANDSHAKE:
			{
				this.connection = new ConnectionManager(network, this.server);
				this.status = ConnectionID.FINISH3;
			}
			case ConnectionID.FINISH3:
			{
				if (!this.connection.finish()) break;
				Connection pack = new Connection();
				pack.RP = (short) mapping.client.socket().getPort();
				pack.UID = this.ID;
				pack.type = Connection.TYPE_CONNECTION;
				this.connection.send(pack);
				this.status = ConnectionID.CLOSED;
				continu = false;
			}
		}
		return continu;
	}
}
