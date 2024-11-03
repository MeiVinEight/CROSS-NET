package org.mve.cross;

import org.mve.cross.pack.Transfer;

import java.io.IOException;
import java.util.logging.Level;

public class TransferC2S implements Runnable
{
	private final TransferManager transfer;

	public TransferC2S(TransferManager transfer)
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
				int length = 1024;
				byte[] payload = new byte[length];
				int read = this.transfer.socket.getInputStream().read(payload, 0, length);
				Transfer transfer = new Transfer();
				transfer.payload = new byte[read];
				System.arraycopy(payload, 0, transfer.payload, 0, read);
				this.transfer.connection.send(transfer);
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
