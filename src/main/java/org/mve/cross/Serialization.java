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
	public static byte R1(InputStream in) throws IOException
	{
		int next = in.read();
		if (next < 0) throw new EOFException();
		return (byte) next;
	}

	public static void W1(OutputStream out, byte b) throws IOException
	{
		out.write(b);
	}

	public static short R2(InputStream in) throws IOException
	{
		short b1 = (short) (Serialization.R1(in) & 0xFF);
		short b2 = (short) (Serialization.R1(in) & 0xFF);
		return (short) ((b1 << 8) | (b2 << 0));
	}

	public static void W2(OutputStream out, short s) throws IOException
	{
		Serialization.W1(out, (byte) (s >> 8));
		Serialization.W1(out, (byte) (s >> 0));
	}

	public static int R4(InputStream in) throws IOException
	{
		int s1 = (Serialization.R2(in) & 0xFFFF);
		int s2 = (Serialization.R2(in) & 0xFFFF);
		return (s1 << 16) | (s2);
	}

	public static void W4(OutputStream out, int i) throws IOException
	{
		Serialization.W2(out, (short) (i >> 16));
		Serialization.W2(out, (short) (i >>  0));
	}

	public static long R8(InputStream in) throws IOException
	{
		long s1 = (((long) Serialization.R4(in)) & 0xFFFFFFFFL);
		long s2 = (((long) Serialization.R4(in)) & 0xFFFFFFFFL);
		return (s1 << 32) | (s2);
	}

	public static void W8(OutputStream out, long i) throws IOException
	{
		Serialization.W4(out, (int) (i >> 32));
		Serialization.W4(out, (int) (i >>  0));
	}

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
