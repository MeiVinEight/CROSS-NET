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
	public static final int FORMAT_TYPE_CONSOLE = 0;
	public static final int FORMAT_TYPE_FILE = 1;
	private final int type;
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

	public CrossFormatter(int type)
	{
		this.type = type;
	}

	public CrossFormatter()
	{
		this(FORMAT_TYPE_CONSOLE);
	}

	@Override
	public String format(LogRecord record)
	{
		StringBuilder builder = new StringBuilder();

		boolean console = type == FORMAT_TYPE_CONSOLE;
		String level = record.getLevel().toString();
		String clear = "\033[0m";
		String color = clear;
		if (record.getLevel() == Level.SEVERE)
		{
			color = ("\u001B[1m\u001B[91m");
			level = "ERRO";
		}
		else if (record.getLevel() == Level.WARNING)
		{
			color = ("\u001B[1m\u001B[33m");
			level = "WARN";
		}
		else if (record.getLevel() == Level.INFO)
		{
			color = ("\u001B[1m\u001B[32m");
		}
		else if (record.getLevel() == Level.FINE)
		{
			color = ("\u001B[1m\u001B[94m");
		}
		else if (record.getLevel() == Level.CONFIG)
		{
			color = ("\u001B[1m\u001B[35m");
			level = "CONF";
		}
		builder
			.append('[')
			.append(DATE_FORMAT.format(new Date(record.getMillis()))).append("] [")
			.append(console ? color : "")
			.append(level)
			.append(console ? clear : "")
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
		while ((!builder.isEmpty()) && builder.charAt(builder.length() - 1) <= 0x20)
		{
			builder.deleteCharAt(builder.length() - 1);
		}
		if (console) builder.append("\u001B[0m");
		return builder.append("\n").toString();
	}
}
