package org.mve.cross.connection;

public class ConnectionMapping
{
	public final int RP;
	public final int LP;
	public final int timeout;

	public ConnectionMapping(int rp, int lp, int timeout)
	{
		RP = rp;
		LP = lp;
		this.timeout = timeout;
	}
}
