package org.mve.cross;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.mve.cross.pack.Connection;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;
import org.mve.invoke.common.JavaVM;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class NetworkManager
{
	private static final String KEY_SERVER_IP = "server-ip";
	private static final String KEY_SERVER_PORT = "server-port";
	private static final String KEY_MAPPING = "mapping";
	private static final String KEY_MAPPING_LISTEN_PORT = "listen-port";
	private static final String KEY_MAPPING_LOCALE_PORT = "locale-port";
	private static final Map<Integer, Integer> MAPPING = new HashMap<>();
	public static final String SERVER_IP;
	public static final int SERVER_PORT;
	public static final int NETWORK_STAT_READY = 0;
	public static final int NETWORK_STAT_COMMUNICATION = 1;
	public static final int NETWORK_STAT_RUNNING = 2;
	public static final int NETWORK_STAT_STOPPED = 3;
	public final int type;
	// Server listen connections from frp client
	private final TransferMonitor transfer;
	// Communication connect between FRP server and client
	public ConnectionManager communication;
	// Server listen connections from all users
	public ConnectionMonitor[][] server = new ConnectionMonitor[256][];
	private int status = NetworkManager.NETWORK_STAT_READY;
	public Object[][][][] connection = new Object[256][][][];

	public NetworkManager(int type)
	{
		this.type = type;
		this.status = NETWORK_STAT_COMMUNICATION;
		try
		{
			if (this.type == CrossNet.SIDE_SERVER)
			{
				// Create remote listen server
				ServerSocket remote = new ServerSocket(SERVER_PORT);
				// this.transfer = new ServerSocket(SERVER_PORT);
				// Create a connection with frp client
				while (this.communication == null)
				{
					CrossNet.LOG.info("Waiting communication at " + SERVER_PORT);
					this.communication = new ConnectionManager(this, remote.accept());
					CrossNet.LOG.info("Handshake with " + this.communication.socket.getRemoteSocketAddress());
					try
					{
						Handshake handshake = new Handshake(); // Communication handshake
						this.communication.send(handshake);
						Datapack datapack = this.communication.receive();
						if (!(datapack instanceof Handshake))
						{
							this.communication.close();
							this.communication = null;
						}
						datapack.accept(this.communication);
						if (this.communication.socket.isClosed()) this.communication = null;
					}
					catch (Throwable t)
					{
						CrossNet.LOG.log(Level.WARNING, null, t);
					}
				}
				this.status = NETWORK_STAT_RUNNING;
				this.transfer = new TransferMonitor(this, remote);
				new Thread(new Communication(communication)).start();
				new Thread(this.transfer).start();
			}
			else // CrossNet.SIDE_CLIENT
			{
				this.transfer = null;
				InetAddress addr = InetAddress.getByName(SERVER_IP);
				CrossNet.LOG.info("Communication to " + addr + ":" + SERVER_PORT);
				this.communication = new ConnectionManager(this, new Socket(addr, SERVER_PORT));
				CrossNet.LOG.info("Communication handshake");
				try
				{
					Handshake handshake = new Handshake();
					this.communication.send(handshake);
					Datapack datapack = this.communication.receive();
					if (!(datapack instanceof Handshake))
					{
						this.communication.close();
						this.communication = null;
					}
					else
					{
						datapack.accept(this.communication);
						if (this.communication.socket.isClosed())
						{
							this.communication = null;
						}
					}
					if (this.communication == null)
					{
						throw new IOException("Connection refused");
					}
				}
				catch (IOException e)
				{
					CrossNet.LOG.severe("Handshake failed");
					CrossNet.LOG.log(Level.SEVERE, null, e);
					this.close();
					return;
				}
				this.status = NETWORK_STAT_RUNNING;
				new Thread(new Communication(communication)).start();
				for (Map.Entry<Integer, Integer> entry : NetworkManager.MAPPING.entrySet())
				{
					int listenPort = entry.getKey();
					CrossNet.LOG.info("Communication listen " + listenPort);
					Connection conn = new Connection();
					conn.LP = (short) listenPort;
					this.communication.send(conn);
				}
			}
		}
		catch (Throwable t)
		{
			this.status = NetworkManager.NETWORK_STAT_STOPPED;
			CrossNet.LOG.log(Level.SEVERE, null, t);
			JavaVM.exception(t);
			throw new RuntimeException(t);
		}
	}

	public int status()
	{
		return this.status;
	}

	public void close()
	{
		if (this.status == NETWORK_STAT_STOPPED) return;

		CrossNet.LOG.warning("CLOSE");
		this.status = NetworkManager.NETWORK_STAT_STOPPED;
		if (this.communication != null)
		{
			try
			{
				CrossNet.LOG.info("Close comminucation");
				this.communication.close();
			}
			catch (IOException e)
			{
				e.printStackTrace(System.out);
			}
		}

		if (this.transfer != null)
		{
			CrossNet.LOG.info("Close proxy server");
			this.transfer.close();
		}

		for (ConnectionMonitor[] s2 : this.server)
		{
			if (s2 != null)
			{
				for (ConnectionMonitor monitor : s2)
				{
					if (monitor != null)
					{
						monitor.close();
					}
				}
			}
		}
	}

	public static int mapping(int port)
	{
		Integer local = MAPPING.get(port);
		if (local == null) return 0;
		return local;
	}

	public Object connection(int rp, int lp)
	{
		int idx0 = (rp >> 8) & 0xFF;
		int idx1 = (rp >> 0) & 0xFF;
		int idx2 = (lp >> 8) & 0xFF;
		int idx3 = (lp >> 0) & 0xFF;
		Object[][][][] s0 = this.connection;
		if (s0[idx0] == null) s0[idx0] = new Object[256][][];
		Object[][][] s1 = s0[idx0];
		if (s1[idx1] == null) s1[idx1] = new Object[256][];
		Object[][] s2 = s1[idx1];
		if (s2[idx2] == null) s2[idx2] = new Object[256];
		Object[] s3 = s2[idx2];
		return s3[idx3];
	}

	public void connection(int rp, int lp, Object obj)
	{
		int idx0 = (rp >> 8) & 0xFF;
		int idx1 = (rp >> 0) & 0xFF;
		int idx2 = (lp >> 8) & 0xFF;
		int idx3 = (lp >> 0) & 0xFF;
		Object[][][][] s0 = this.connection;
		if (s0[idx0] == null) s0[idx0] = new Object[256][][];
		Object[][][] s1 = s0[idx0];
		if (s1[idx1] == null) s1[idx1] = new Object[256][];
		Object[][] s2 = s1[idx1];
		if (s2[idx2] == null) s2[idx2] = new Object[256];
		Object[] s3 = s2[idx2];
		s3[idx3] = obj;
	}

	static
	{
		try
		{
			JsonArray array = CrossNet.PROPERTIES.get(KEY_MAPPING).getAsJsonArray();
			for (int i = 0; i < array.size(); i++)
			{
				JsonObject object = array.get(i).getAsJsonObject();
				int listen = Integer.parseInt(object.get(KEY_MAPPING_LISTEN_PORT).getAsString());
				int locale = Integer.parseInt(object.get(KEY_MAPPING_LOCALE_PORT).getAsString());
				MAPPING.put(listen, locale);
				CrossNet.LOG.config("Mapping " + listen + "(S) - " + locale + "(C)");
			}
			SERVER_IP = CrossNet.PROPERTIES.get(KEY_SERVER_IP).getAsString();
			SERVER_PORT = CrossNet.PROPERTIES.get(KEY_SERVER_PORT).getAsInt();
		}
		catch (Throwable e)
		{
			CrossNet.LOG.log(Level.SEVERE, "Couldn't read properties file");
			CrossNet.LOG.log(Level.SEVERE, null, e);
			JavaVM.exception(e);
			throw new RuntimeException(e);
		}
	}
}
