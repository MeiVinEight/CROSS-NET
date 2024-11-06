package org.mve.cross.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class CrossNetLoggerFile extends StreamHandler
{
	public CrossNetLoggerFile(String path) throws FileNotFoundException
	{
		super(new FileOutputStream(
			new File(
				path,
				new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".log"
			)
		), new CrossFormatter(CrossFormatter.FORMAT_TYPE_FILE));
		this.setLevel(Level.ALL);
	}

	@Override
	public synchronized void publish(LogRecord record)
	{
		super.publish(record);
		this.flush();
	}
}
