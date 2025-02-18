package org.mve.cross;

import java.util.concurrent.locks.ReentrantLock;

public class VariableID
{
	public static final int ERROR_NOT_IN_USE = 1;
	public static final int ERROR_OVERFLOW   = 2;
	private final int minimum;
	private final int maximum;
	private final int[] queue;
	private final boolean[] using;
	private int head = 0;
	private int tail = 0;
	private int current;
	private final ReentrantLock lock = new ReentrantLock();

	public VariableID(int minValue, int maxValue)
	{
		if (minValue > maxValue) throw new IllegalArgumentException("Minimum value > Maximum value");
		if (minValue < 0) throw new IllegalArgumentException("Minimum value < 0");
		this.minimum = minValue;
		this.maximum = maxValue + 1;
		this.queue = new int[this.maximum];
		this.using = new boolean[this.maximum];
		this.current = this.minimum;
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
		if (value != -1) this.using[value] = true;
		this.lock.unlock();
		return value;
	}

	public int free(int value)
	{
		if (value < this.minimum || value >= this.maximum) return -1;
		int retVal = 0;
		this.lock.lock();
		FREE:
		{
			if (!this.using[value])
			{
				retVal = VariableID.ERROR_NOT_IN_USE;
				break FREE;
			}
			this.using[value] = false;
			if (((this.tail + 1) % this.maximum) != this.head)
			{
				this.queue[this.tail++] = value;
				this.tail %= this.maximum;
			}
			else retVal = VariableID.ERROR_OVERFLOW;
		}
		this.lock.unlock();
		return retVal;
	}
}
