
package au.org.emii.ncdfgenerator;

import java.io.OutputStream;
import java.io.InputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import au.org.emii.ncdfgenerator.INcdfEncoder;

// may want to add a report file etc...

public class ZipCreator
{
	final private INcdfEncoder encoder; 

	public ZipCreator( INcdfEncoder encoder )
	{
		this.encoder = encoder; 
	}

	void doStreaming( OutputStream os ) throws Exception  
	{
		ZipOutputStream zipStream = null; 
		try { 
			// encoder.prepare() ...
			zipStream = new ZipOutputStream( os ); 
			int count = 0; 
			InputStream writer = encoder.get();
			while( writer != null ) 
			{
				String filenameToUse = "file" + count + ".nc";
				zipStream.putNextEntry( new ZipEntry(filenameToUse) ); 
				int bytesCopied = IOUtils.copy( writer, zipStream); 
				writer = encoder.get();
				++count;
			}

		} finally { 
			zipStream.close();
		}
	}
}

