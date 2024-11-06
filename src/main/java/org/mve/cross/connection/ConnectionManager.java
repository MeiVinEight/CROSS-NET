package org.mve.cross.connection;

import org.mve.cross.CrossNet;
import org.mve.cross.NetworkManager;
import org.mve.cross.ProtocolManager;
import org.mve.cross.Serialization;
import org.mve.cross.pack.Datapack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ConnectionManager
{
	public final NetworkManager network;
	public final Socket socket;
	private final ReentrantLock lock = new ReentrantLock();

	public ConnectionManager(NetworkManager network, Socket socket)
	{
		this.network = network;
		this.socket = socket;
	}

	public Datapack receive() throws IOException
	{
		InputStream in = this.socket.getInputStream();
		// this.lock.lock();
		byte id = Serialization.R1(in);
		if (id >= ProtocolManager.CONSTRUCTOR.length)
		{
			throw new IOException("UNKNOWN DATAPACK ID: " + id);
		}
		Datapack pack = ProtocolManager.CONSTRUCTOR[id].invoke();
		pack.read(in);
		// this.lock.unlock();
		return pack;
	}

	public void send(Datapack pack) throws IOException
	{
		OutputStream out = this.socket.getOutputStream();
		this.lock.lock();
		Serialization.W1(out, (byte) pack.ID());
		pack.write(out);
		this.lock.unlock();
	}

	public void close()
	{
		try
		{
			this.socket.close();
		}
		catch (IOException e)
		{
			CrossNet.LOG.log(Level.WARNING, null, e);
		}
	}
}
