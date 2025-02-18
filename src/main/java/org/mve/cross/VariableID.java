package org.mve.cross;

import java.util.concurrent.locks.ReentrantLock;

public class VariableID
{
	private final int maximum;
	private final int[] queue;
	private int head = 0;
	private int tail = 0;
	private int current;
	private final ReentrantLock lock = new ReentrantLock();

	public VariableID(int minValue, int maxValue)
	{
		this.maximum = maxValue + 1;
		this.queue = new int[this.maximum];
		this.current = minValue;
	}

	public int get()
	{
		int value = -1;
		this.lock.lock();
		if (this.head != this.tail)
		{
			value = this.queue[this.head++];
			this.head %= this.maximum;
		}
		else if (this.current < this.maximum) value = this.current++;
		this.lock.unlock();
		return value;
	}

	public int free(int value)
	{
		int retVal = 0;
		this.lock.lock();
		if (((this.tail + 1) % this.maximum) != this.head)
		{
			this.queue[this.tail++] = value;
			this.tail %= this.maximum;
		}
		else retVal = -1;
		this.lock.unlock();
		return retVal;
	}
}
