package org.mve.cross;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mve.invoke.common.JavaVM;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.LogManager;

public class CrossNet
{
	private static final String PROPERTIES_FILE = "cross-net.json";
	public static final JsonObject PROPERTIES;
	public static final int SIDE_CLIENT = 1;
	public static final int SIDE_SERVER = 2;
	public static final CrossLogger LOG;

	public static void start(int side)
	{
		if (side == CrossNet.SIDE_SERVER)
		{
			CrossNet.LOG.info("Cross-Net starting on server");
		}
		else
		{
			CrossNet.LOG.info("Cross-Net starting on client");
		}
		try
		{
			NetworkManager nm = new NetworkManager(side);
		}
		catch (Throwable ignored)
		{
		}
	}

	static
	{
		LogManager.getLogManager().addLogger(new CrossLogger());
		LOG = (CrossLogger) LogManager.getLogManager().getLogger(CrossLogger.LOGGER_NAME);
		JsonObject property = null;
		try (FileInputStream propertyFile = new FileInputStream(PROPERTIES_FILE))
		{
			InputStreamReader reader = new InputStreamReader(propertyFile);

			// Resolve json properties
			property = (JsonObject) JsonParser.parseReader(reader);
		}
		catch (Throwable t)
		{
			JavaVM.exception(t);
		}
		PROPERTIES = property;
	}
}
