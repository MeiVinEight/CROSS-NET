package org.mve.cross;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mve.cross.concurrent.SynchronizeNET;
import org.mve.cross.connection.ConnectionManager;
import org.mve.cross.connection.ConnectionMapping;
import org.mve.cross.connection.ConnectionMonitor;
import org.mve.cross.connection.ConnectionWaiting;
import org.mve.cross.pack.Connection;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Handshake;
import org.mve.cross.text.JSON;
import org.mve.cross.transfer.TransferManager;
import org.mve.cross.transfer.TransferMonitor;
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
	private static final String KEY_MAPPING_TIMEOUT = "timeout";
	private static final Map<Integer, ConnectionMapping> MAPPING = new HashMap<>();
	public static final String SERVER_IP;
	public static final int SERVER_PORT;
	public static final int NETWORK_STAT_COMMUNICATION = 1;
	public static final int NETWORK_STAT_RUNNING = 2;
	public static final int NETWORK_STAT_STOPPED = 3;
	public final int type;
	// Server listen connections from frp client
	private final TransferMonitor transfer;
	// Communication connect between FRP server and client
	public ConnectionManager communication;
	// Server listen connections from all users
	public final ConnectionMonitor[] server = new ConnectionMonitor[65536];
	public final ConnectionWaiting[] waiting = new ConnectionWaiting[65536];
	private int status;
	public TransferManager[][][][] connection = new TransferManager[256][][][];
	public final SynchronizeNET synchronize = new SynchronizeNET(this);

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
						this.communication = null;
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
				new Thread(new Communication(this)).start();
				for (Map.Entry<Integer, ConnectionMapping> entry : NetworkManager.MAPPING.entrySet())
				{
					int listenPort = entry.getKey();
					CrossNet.LOG.info("Communication listen " + listenPort);
					ConnectionWaiting waiting = new ConnectionWaiting(this, listenPort, entry.getValue().timeout);
					this.waiting[listenPort] = waiting;
					this.synchronize.offer(waiting);
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
			this.close();
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
		CrossNet.LOG.info("Synchronize");
		this.synchronize.close();
		CrossNet.LOG.info("Communication");
		this.communication.close();

		if (this.transfer != null)
		{
			CrossNet.LOG.info("Transfer waiting");
			this.transfer.close();
		}

		CrossNet.LOG.info("Transfer connection");
		for (ConnectionMonitor monitor : this.server)
		{
			if (monitor != null)
			{
				monitor.close();
			}
		}

		CrossNet.LOG.close();
	}

	public static int mapping(int port)
	{
		ConnectionMapping mapping = MAPPING.get(port);
		if (mapping == null) return 0;
		return mapping.LP;
	}

	public static int timeout(int port)
	{
		ConnectionMapping mapping = MAPPING.get(port);
		if (mapping == null) return 0;
		return mapping.timeout;
	}

	public TransferManager connection(int rp, int lp)
	{
		int idx0 = (rp >> 8) & 0xFF;
		int idx1 = (rp >> 0) & 0xFF;
		int idx2 = (lp >> 8) & 0xFF;
		int idx3 = (lp >> 0) & 0xFF;
		TransferManager[][][][] s0 = this.connection;
		if (s0[idx0] == null) s0[idx0] = new TransferManager[256][][];
		TransferManager[][][] s1 = s0[idx0];
		if (s1[idx1] == null) s1[idx1] = new TransferManager[256][];
		TransferManager[][] s2 = s1[idx1];
		if (s2[idx2] == null) s2[idx2] = new TransferManager[256];
		TransferManager[] s3 = s2[idx2];
		return s3[idx3];
	}

	public void connection(int rp, int lp, TransferManager obj)
	{
		int idx0 = (rp >> 8) & 0xFF;
		int idx1 = (rp >> 0) & 0xFF;
		int idx2 = (lp >> 8) & 0xFF;
		int idx3 = (lp >> 0) & 0xFF;
		TransferManager[][][][] s0 = this.connection;
		if (s0[idx0] == null) s0[idx0] = new TransferManager[256][][];
		TransferManager[][][] s1 = s0[idx0];
		if (s1[idx1] == null) s1[idx1] = new TransferManager[256][];
		TransferManager[][] s2 = s1[idx1];
		if (s2[idx2] == null) s2[idx2] = new TransferManager[256];
		TransferManager[] s3 = s2[idx2];
		s3[idx3] = obj;
	}

	static
	{
		try
		{
			JsonElement mappingObject = CrossNet.PROPERTIES.get(KEY_MAPPING);
			if (mappingObject != null)
			{
				JsonArray array = CrossNet.PROPERTIES.get(KEY_MAPPING).getAsJsonArray();
				for (int i = 0; i < array.size(); i++)
				{
					JsonObject object = array.get(i).getAsJsonObject();
					int listen = Integer.parseInt(object.get(KEY_MAPPING_LISTEN_PORT).getAsString());
					int locale = Integer.parseInt(object.get(KEY_MAPPING_LOCALE_PORT).getAsString());
					int timeout = JSON.as(object.get(KEY_MAPPING_TIMEOUT), int.class, 0);
					MAPPING.put(listen, new ConnectionMapping(listen, locale, timeout));
					CrossNet.LOG.config("Mapping " + listen + "(S) - " + locale + "(C)");
				}
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
