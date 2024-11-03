package org.mve.cross.main;

import org.mve.cross.CrossNet;

public class ServerMain
{
	public static void main(String[] args)
	{
		CrossNet.start(CrossNet.SIDE_SERVER);
	}
}
