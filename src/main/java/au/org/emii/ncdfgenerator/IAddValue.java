
package au.org.emii.ncdfgenerator;

public interface IAddValue
{
	public void prepare();
	// change name to put(), or append? and class to IBufferAddValue
	public void addValueToBuffer( Object value );
}


