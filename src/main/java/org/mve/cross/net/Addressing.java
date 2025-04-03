package org.mve.cross.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class Addressing
{
	public static SocketAddress address(SocketChannel channel)
	{
		if (channel == null) return null;

		try
		{
			return channel.getRemoteAddress();
		}
		catch (IOException ignored)
		{
		}

		return channel.socket().getRemoteSocketAddress();
	}
}
