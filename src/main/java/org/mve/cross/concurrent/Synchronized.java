package org.mve.cross.concurrent;

public abstract class Synchronized implements Runnable
{
	public long delay = 0;
	public long period = 1;
	public boolean cancelled = false;

	public void cancel()
	{
		this.cancelled = true;
	}
}
