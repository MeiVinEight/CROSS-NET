package org.mve.cross.transfer;

import org.mve.cross.CrossNet;
import org.mve.cross.Serialization;
import org.mve.cross.nio.DynamicArray;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Transfer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class TransferS2C implements Runnable
{
	private final TransferManager transfer;

	public TransferS2C(TransferManager transfer)
	{
		this.transfer = transfer;
	}

	@Override
	public void run()
	{
		DynamicArray buffer = new DynamicArray();
		Thread.currentThread().setName("Transfer-" + this.transfer.RP());
		try
		{
			{
				String info = "Transfer " + this.transfer.connection.address + " -> " +
					this.transfer.address + " S2C";
				CrossNet.LOG.info(info);
			}
			while (this.transfer.running())
			{
				Datapack pack = this.transfer.connection.receive();
				if (pack instanceof Transfer transfer)
				{
					buffer.expand(transfer.payload.length);
					buffer.put(transfer.payload);
					buffer.flip();
					Serialization.transfer((WritableByteChannel) this.transfer.socket, buffer);
					buffer.clear();
					System.gc();
				}
				else
				{
					pack.accept(this.transfer.connection);
				}
			}
		}
		catch (IOException e)
		{
			this.transfer.exception(e);
		}
	}
}
