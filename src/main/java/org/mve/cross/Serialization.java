package org.mve.cross;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Serialization
{
	public static void transfer(InputStream in, byte[] buf, int length) throws IOException
	{
		int idx = 0;
		while (idx < length)
		{
			int read = in.read(buf, idx, length - idx);
			if (read < 0) throw new EOFException();
			idx += read;
		}
	}

	public static void transfer(OutputStream out, byte[] buf, int length) throws IOException
	{
		out.write(buf, 0, length);
	}

	public static void transfer(ReadableByteChannel channel, ByteBuffer buffer) throws IOException
	{
		while (buffer.hasRemaining())
		{
			int read = channel.read(buffer);
			if (read < 0) throw new EOFException();
			Thread.yield();
		}
	}

	public static void transfer(WritableByteChannel channel, ByteBuffer buffer) throws IOException
	{
		while (buffer.hasRemaining()) channel.write(buffer);
	}
}
