package org.mve.cross.main;

import org.mve.cross.CrossNet;

public class ClientMain
{
	public static void main(String[] args)
	{
		// UUID uuid = UUID.randomUUID();
		// System.out.println("UUID(" + uuid.getMostSignificantBits() + "L, " + uuid.getLeastSignificantBits() + "L)");
		CrossNet.start(CrossNet.SIDE_CLIENT);
	}
}
