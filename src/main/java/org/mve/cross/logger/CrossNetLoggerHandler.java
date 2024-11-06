package org.mve.cross.logger;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class CrossNetLoggerHandler extends StreamHandler
{
	public CrossNetLoggerHandler()
	{
		super(System.out, new CrossFormatter());
		this.setLevel(Level.ALL);
	}

	@Override
	public synchronized void publish(LogRecord record)
	{
		super.publish(record);
		this.flush();
	}

	@Override
	public synchronized void close() throws SecurityException
	{
		this.flush();
	}
}
