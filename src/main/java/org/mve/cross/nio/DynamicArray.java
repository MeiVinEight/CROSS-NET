package org.mve.cross.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class DynamicArray
{
	private ByteBuffer buffer;

	public DynamicArray(int capacity)
	{
		this.expand(capacity);
	}

	public DynamicArray()
	{
		this(0);
	}

	public int capacity()
	{
		if (this.buffer != null) return this.buffer.capacity();
		return 0;
	}

	public void expand(int capacity)
	{
		if (capacity <= this.capacity()) return;

		ByteBuffer oldBuffer = this.buffer;
		this.buffer = ByteBuffer.allocateDirect(capacity);
		if (oldBuffer != null)
		{
			oldBuffer.flip();
			this.buffer.put(oldBuffer);
		}
		oldBuffer = null;
		System.gc();
	}

	public void acquire(int capacity)
	{
		if (this.buffer.remaining() >= capacity) return;
		if (this.buffer.position() + capacity <= this.buffer.capacity())
		{
			this.buffer.limit(this.buffer.capacity());
			return;
		}
		this.expand(this.capacity() * 2);
	}

	public int read(ReadableByteChannel channel) throws IOException
	{
		return channel.read(this.buffer);
	}

	public int write(WritableByteChannel channel) throws IOException
	{
		return channel.write(this.buffer);
	}

	public int remaining()
	{
		return this.buffer.remaining();
	}

	public void limit(int newLimit)
	{
		this.buffer.limit(newLimit);
	}

	public void clear()
	{
		this.buffer.clear();
	}

	public void flip()
	{
		this.buffer.flip();
	}

	public void get(byte[] bytes)
	{
		this.buffer.get(bytes);
	}

	public byte get()
	{
		return this.buffer.get();
	}

	public short getShort()
	{
		return this.buffer.getShort();
	}

	public int getInt()
	{
		return this.buffer.getInt();
	}

	public long getLong()
	{
		return this.buffer.getLong();
	}

	public void put(byte[] bytes)
	{
		this.buffer.put(bytes);
	}

	public void put(byte x)
	{
		this.buffer.put(x);
	}

	public void putShort(short x)
	{
		this.buffer.putShort(x);
	}

	public void putInt(int x)
	{
		this.buffer.putInt(x);
	}

	public void putLong(long x)
	{
		this.buffer.putLong(x);
	}
}
