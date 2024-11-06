package org.mve.cross.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrossLogger extends Logger
{
	public static final String LOGGER_NAME = "CROSS-NET";
	private CrossNetLoggerFile file;

	public CrossLogger(String logPath)
	{
		super(CrossLogger.LOGGER_NAME, null);
		this.setUseParentHandlers(false);
		this.addHandler(new CrossNetLoggerHandler());
		new File(logPath).mkdir();
		try
		{
			this.addHandler(this.file = new CrossNetLoggerFile(logPath));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace(System.out);
		}
		this.setLevel(Level.ALL);
	}

	@Override
	public void log(Level level, String msg)
	{
		super.log(level, msg);
	}

	public void close()
	{
		if (this.file != null) this.file.close();
	}
}
