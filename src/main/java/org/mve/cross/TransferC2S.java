package org.mve.cross;

import org.mve.cross.pack.Transfer;

import java.io.EOFException;
import java.io.IOException;

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
			{
				String info = "Transfer " + this.transfer.socket.getRemoteSocketAddress() + " -> " +
					this.transfer.connection.socket.getRemoteSocketAddress();
				CrossNet.LOG.info(info);
			}
			while (this.transfer.running())
			{
				int length = 1024;
				byte[] payload = new byte[length];
				int read = this.transfer.socket.getInputStream().read(payload, 0, length);
				if (read < 0)
				{
					// Connection closed
					CrossNet.LOG.info("Connection " + this.transfer.socket.getRemoteSocketAddress() + " closed");
					throw new EOFException();
				}
				Transfer transfer = new Transfer();
				transfer.payload = new byte[read];
				System.arraycopy(payload, 0, transfer.payload, 0, read);
				this.transfer.connection.send(transfer);
			}
		}
		catch (IOException e)
		{
			this.transfer.exception(e);
		}
	}
}
