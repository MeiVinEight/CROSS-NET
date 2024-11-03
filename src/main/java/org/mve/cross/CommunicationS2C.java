package org.mve.cross;

public class CommunicationS2C implements Runnable
{
	private final ConnectionManager connection;

	public CommunicationS2C(ConnectionManager connection)
	{
		this.connection = connection;
	}

	@Override
	public void run()
	{
	}
}
