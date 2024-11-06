package org.mve.cross.concurrent;

import org.mve.cross.NetworkManager;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

public class SynchronizeNET implements Runnable
{
	private static final long PERIOD_MS = 50;
	public final NetworkManager network;
	private final Queue<Synchronized> queue = new ConcurrentLinkedQueue<>();
	private boolean running = true;

	public SynchronizeNET(NetworkManager network)
	{
		this.network = network;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Synchronize");
		long nextTime = System.nanoTime() + (SynchronizeNET.PERIOD_MS * 1000000);
		while (this.running)
		{
			this.synchronize();
			long now = System.nanoTime();
			if (now < nextTime)  LockSupport.parkNanos(nextTime - now);
			nextTime += (SynchronizeNET.PERIOD_MS * 1000000);
		}
	}

	private void synchronize()
	{
		int count = this.queue.size();
		while (count --> 0)
		{
			Synchronized task = this.queue.poll();
			if (task == null) continue;

			if (task.cancelled) continue;
			if (task.delay > 0)
			{
				task.delay--;
				this.queue.offer(task);
				continue;
			}
			task.run();
			if (task.period > 0)
			{
				task.delay = task.period;
				this.queue.offer(task);
			}
		}
	}

	public void offer(Synchronized task)
	{
		this.queue.offer(task);
	}

	public void close()
	{
		this.running = false;
	}
}
