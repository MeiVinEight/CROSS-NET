package org.mve.cross.pack;

import org.mve.cross.CrossNet;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMonitor;
import org.mve.cross.nio.DynamicArray;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;

public class Listen extends Datapack
{
	public static final short STAT_OK = 0;
	public static final short STAT_DENIED = 1;
	public static final int ID = 0x04;
	public short ON;
	public short status;

	@Override
	public void read(DynamicArray buffer) throws IOException
	{
		this.ON = buffer.getShort();
		this.status = buffer.getShort();
	}

	@Override
	public void write(DynamicArray buffer) throws IOException
	{
		buffer.putShort(this.ON);
		buffer.putShort(this.status);
	}

	@Override
	public int ID()
	{
		return Listen.ID;
	}

	@Override
	public void accept(ConnectionManager conn)
	{
		if (conn.network.type == CrossNet.SIDE_CLIENT)
		{
			if (this.status == STAT_OK)
			{
				CrossNet.LOG.info("Server listening on " + this.ON + " \u001B[1m\u001B[32mOK");
			}
			else if (this.status == STAT_DENIED)
			{
				CrossNet.LOG.info("Server listening on " + this.ON + " \u001B[1m\u001B[91mDENIED");
			}
			return;
		}

		Listen response = new Listen();
		response.ON = this.ON;
		response.status = STAT_OK;
		int port = (this.ON & 0xFFFF);
		LISTEN:
		{
			if (conn.network.server[port] != null)
			{
				CrossNet.LOG.warning("Server is already listening on " + port);
				response.status = STAT_DENIED;
				break LISTEN;
			}

			try
			{
				ServerSocketChannel ssc = ServerSocketChannel.open();
				ssc.bind(new InetSocketAddress(port));
				ssc.configureBlocking(false);
				conn.network.server[port] = new ConnectionMonitor(conn.network, ssc);
				conn.network.server[port].register(conn.network);
				CrossNet.LOG.info("Listening on " + this.ON + " \u001B[1m\u001B[32mOK");
			}
			catch (IOException e)
			{
				CrossNet.LOG.warning("Cannot listen on " + port);
				CrossNet.LOG.log(Level.WARNING, null, e);
				response.status = STAT_DENIED;
				CrossNet.LOG.info("Server listening on " + this.ON + " \u001B[1m\u001B[91mDENIED");
			}
		}
		try
		{
			conn.send(response);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
