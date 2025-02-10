package org.mve.cross;

public class AddressMapping
{
	public final int RP;
	public final int LP;
	public final int timeout;

	public AddressMapping(int rp, int lp, int timeout)
	{
		RP = rp;
		LP = lp;
		this.timeout = timeout;
	}
}
