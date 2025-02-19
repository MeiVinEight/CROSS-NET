package org.mve.cross.nio;

import org.mve.cross.NetworkManager;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface Selection
{
	public abstract void register(NetworkManager network) throws ClosedChannelException;
	public abstract void select(SelectionKey key);
}
