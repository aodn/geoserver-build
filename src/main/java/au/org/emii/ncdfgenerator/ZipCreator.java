
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
			InputStream is = encoder.get();
			while( is != null ) 
			{
				String filenameToUse = "file" + count + ".nc";
				zipStream.putNextEntry( new ZipEntry(filenameToUse) ); 
				int bytesCopied = IOUtils.copy( is, zipStream); 
				is.close();
				is = encoder.get();
				++count;
			}

		} finally { 
			zipStream.close();
		}
	}
}

