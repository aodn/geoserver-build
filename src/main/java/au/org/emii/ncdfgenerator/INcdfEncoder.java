
package au.org.emii.ncdfgenerator;

import java.io.InputStream;

public interface INcdfEncoder
{
	public void prepare() throws Exception; 

	public InputStream get() throws Exception; 
}	

