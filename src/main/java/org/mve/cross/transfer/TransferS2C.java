package org.mve.cross.transfer;

import org.mve.cross.CrossNet;
import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Transfer;

import java.io.IOException;

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
		Thread.currentThread().setName("Transfer");
		try
		{
			{
				String info = "Transfer " + this.transfer.connection.socket.getRemoteSocketAddress() + " -> " +
					this.transfer.socket.getRemoteSocketAddress();
				CrossNet.LOG.info(info);
			}
			while (this.transfer.running())
			{
				Datapack pack = this.transfer.connection.receive();
				if (pack instanceof Transfer)
				{
					Transfer transfer = (Transfer) pack;
					this.transfer.socket.getOutputStream().write(transfer.payload);
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
