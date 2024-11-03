package org.mve.cross;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CrossLogger extends Logger
{
	public static final String LOGGER_NAME = "CROSS-NET";

	public CrossLogger()
	{
		super(CrossLogger.LOGGER_NAME, null);
		this.setUseParentHandlers(false);
		this.addHandler(new CrossNetLoggerHandler());
		this.setLevel(Level.ALL);
	}
}
