package org.mve.cross;

import org.mve.cross.pack.Datapack;
import org.mve.cross.pack.Transfer;

import java.io.IOException;
import java.util.logging.Level;

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
			int rp = this.transfer.RP();
			int lp = this.transfer.LP();
			CrossNet.LOG.severe("Transfer " + lp + " - " + rp);
			CrossNet.LOG.log(Level.SEVERE, null, e);
			this.transfer.close();
		}
	}
}
