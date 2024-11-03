package org.mve.cross;

import org.mve.cross.pack.Datapack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionManager
{
	public final NetworkManager network;
	public final Socket socket;

	public ConnectionManager(NetworkManager network, Socket socket)
	{
		this.network = network;
		this.socket = socket;
	}

	public Datapack receive() throws IOException
	{
		InputStream in = this.socket.getInputStream();
		byte id = Serialization.R1(in);
		if (id >= ProtocolManager.CONSTRUCTOR.length)
		{
			throw new IOException("UNKNOWN DATAPACK ID: " + id);
		}
		Datapack pack = ProtocolManager.CONSTRUCTOR[id].invoke();
		pack.read(in);
		return pack;
	}

	public void send(Datapack pack) throws IOException
	{
		OutputStream out = this.socket.getOutputStream();
		Serialization.W1(out, (byte) pack.ID());
		pack.write(out);
	}

	public void close() throws IOException
	{
		this.socket.close();
	}
}
