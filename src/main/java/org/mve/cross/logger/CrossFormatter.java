package org.mve.cross.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class CrossFormatter extends Formatter
{
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

	@Override
	public String format(LogRecord record)
	{
		StringBuilder builder = new StringBuilder();
		if (record.getLevel() == Level.SEVERE)
		{
			builder.append("\u001B[1m\u001B[91m");
		}
		else if (record.getLevel() == Level.WARNING)
		{
			builder.append("\u001B[1m\u001B[33m");
		}
		else if (record.getLevel() == Level.INFO)
		{
			builder.append("\u001B[0m");
		}
		else if (record.getLevel() == Level.FINE)
		{
			builder.append("\u001B[1m\u001B[94m");
		}
		builder
			.append('[')
			.append(DATE_FORMAT.format(new Date(record.getMillis()))).append("] [")
			.append(record.getLevel())
			.append("] [")
			.append(Thread.currentThread().getName())
			.append("]");
		boolean hasMsg = false;
		if (record.getMessage() != null && !record.getMessage().isEmpty())
		{
			hasMsg = true;
			builder.append(' ').append(record.getMessage());
		}
		if (record.getThrown() != null)
		{
			StringWriter writer = new StringWriter();
			PrintWriter ps = new PrintWriter(writer);
			record.getThrown().printStackTrace(ps);
			builder.append(hasMsg ? '\n' : ' ').append(writer);
		}
		while (builder.length() > 0 && builder.charAt(builder.length() - 1) <= 0x20)
		{
			builder.deleteCharAt(builder.length() - 1);
		}
		return builder.append("\u001B[0m\n").toString();
	}
}
