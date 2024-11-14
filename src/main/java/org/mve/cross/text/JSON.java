package org.mve.cross.text;

import com.google.gson.JsonElement;

public class JSON
{
	public static <T> T as(JsonElement element, Class<T> type, T defaultValue)
	{
		if (element == null) return defaultValue;

		if (type == int.class)
		{
			return (T) Integer.valueOf(element.getAsInt());
		}
		else if (type == long.class)
		{
			return (T) Long.valueOf(element.getAsLong());
		}
		return defaultValue;
	}
}
