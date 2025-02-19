package org.mve.cross;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mve.cross.text.JSON;
import org.mve.invoke.common.JavaVM;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Configuration
{
	private static final String KEY_SERVER_ADDRESS = "server-address";
	private static final String KEY_SERVER_PORT = "server-port";
	private static final String KEY_MAPPING = "mapping";
	private static final String KEY_MAPPING_LISTEN_PORT = "listen-port";
	private static final String KEY_MAPPING_LOCALE_PORT = "locale-port";
	private static final String KEY_MAPPING_TIMEOUT = "timeout";
	private static final String KEY_COMMUNICATION = "communication";
	private static final String KEY_COMMUNICATION_CONNECT = "connect";
	public static final Map<Integer, AddressMapping> MAPPING = new HashMap<>();
	public static final String SERVER_ADDRESS;
	public static final int SERVER_PORT;
	public static final int COMMUNICATION_CONNECT;
	public static final int DEFAULT_BUFFER_SIZE = 4096;

	public static int mapping(int port)
	{
		AddressMapping mapping = Configuration.MAPPING.get(port);
		if (mapping == null) return 0;
		return mapping.LP;
	}

	public static int timeout(int port)
	{
		AddressMapping mapping = Configuration.MAPPING.get(port);
		if (mapping == null) return 0;
		return mapping.timeout;
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
					MAPPING.put(listen, new AddressMapping(listen, locale, timeout));
					CrossNet.LOG.config("Mapping " + listen + "(S) - " + locale + "(C)");
				}
			}
			SERVER_ADDRESS = CrossNet.PROPERTIES.get(KEY_SERVER_ADDRESS).getAsString();
			SERVER_PORT = CrossNet.PROPERTIES.get(KEY_SERVER_PORT).getAsInt();
			COMMUNICATION_CONNECT = CrossNet
				.PROPERTIES
				.get(KEY_COMMUNICATION)
				.getAsJsonObject()
				.get(KEY_COMMUNICATION_CONNECT)
				.getAsInt();
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
