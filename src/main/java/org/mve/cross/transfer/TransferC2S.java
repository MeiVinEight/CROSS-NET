package org.mve.cross.transfer;

import org.mve.cross.Configuration;
import org.mve.cross.CrossNet;
import org.mve.cross.pack.Transfer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

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
		Thread.currentThread().setName("Transfer-" + this.transfer.RP());
		try
		{
			{
				String info = "Transfer " + this.transfer.address + " -> " +
					this.transfer.connection.address + " C2S";
				CrossNet.LOG.info(info);
			}
			while (this.transfer.running())
			{
				ByteBuffer buffer = ByteBuffer.allocateDirect(Configuration.DEFAULT_BUFFER_SIZE);
				int read = this.transfer.socket.read(buffer);
				if (read < 0)
				{
					// Connection closed
					CrossNet.LOG.info("Connection " + this.transfer.address + " closed");
					throw new EOFException();
				}
				if (read > 0)
				{
					Transfer transfer = new Transfer();
					transfer.payload = new byte[read];
					buffer.flip();
					buffer.get(transfer.payload);
					this.transfer.connection.send(transfer);
				}
			}
		}
		catch (IOException e)
		{
			this.transfer.exception(e);
		}
	}
}
