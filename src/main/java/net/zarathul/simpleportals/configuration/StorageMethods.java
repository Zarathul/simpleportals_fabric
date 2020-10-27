package net.zarathul.simpleportals.configuration;

import java.lang.reflect.Method;

public class StorageMethods
{
	public Method load;
	public Method save;

	public StorageMethods(Method load, Method save)
	{
		this.load = load;
		this.save = save;
	}
}
