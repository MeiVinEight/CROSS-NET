package org.mve.cross;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class CrossNetTest
{
	@Test
	public void test() throws Throwable
	{
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.bind(new InetSocketAddress(12138));

		SocketChannel socket1 = SocketChannel.open();
		socket1.configureBlocking(false);
		socket1.connect(new InetSocketAddress(12138));

		SocketChannel socket2 = null;
		while (socket2 == null) socket2 = server.accept();
		socket2.configureBlocking(false);

		while (!socket1.finishConnect()) Thread.yield();

		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(12138);
		buffer.flip();
		// socket1.write(buffer);

		/*
		buffer.clear();
		int read = socket2.read(buffer);
		buffer.flip();
		Assertions.assertEquals(4, read);
		Assertions.assertEquals(12138, buffer.getInt());
		 */

		// socket2.close();
		socket1.close();

		buffer.clear();
		buffer.limit(0);
		Assertions.assertEquals(0, socket2.read(buffer));

		socket2.close();

		server.close();
	}
}
