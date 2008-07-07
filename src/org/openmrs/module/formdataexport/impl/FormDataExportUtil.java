package org.openmrs.module.formdataexport.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.FileCopyUtils;

public class FormDataExportUtil {
	/* Logger */
	private static Log log = LogFactory.getLog(FormDataExportUtil.class);

	/**
	 * Converts the separator in the file from any separator to any other 
	 * separator.  This method also adds quotes around all columns.  
	 * 
	 * TODO This should be configurable.
	 * 
	 * @param fromFilename		full path to the input file
	 * @param fromSeparator		string representing the separator to convert from
	 * @param toFilename		full path to the output file
	 * @param toSeparator		string representing the separator to convert to
	 */
	public static boolean convertFormat(String fromFilename, String fromSeparator, String toFilename, String toSeparator) { 
		
		DataOutputStream output = null;
		BufferedReader input = null;

		try {

			StringBuffer buffer = new StringBuffer();
			input = new BufferedReader(new FileReader(fromFilename));
			output = new DataOutputStream(new FileOutputStream(toFilename));
		
			// Iterate over the lines in the file 
			String line = null;
			while((line = input.readLine()) != null) {
				
				// Get all column values for a single line
				String[] columns = line.split(fromSeparator);
				if ( columns != null ) { 
					
					// Iterate over all columns for a single line
					for(int i=0; i<columns.length; i++) { 
						// Add leading and trailing quotes
						buffer.append("\"").append(columns[i].trim()).append("\"");
						
						// If not the last column, add the new separator (comma, pipe, etc)
						if (i < columns.length-1) buffer.append(toSeparator);
					}
					// Add a new line and process next line
					buffer.append("\n");
				}
			}
			output.writeBytes(buffer.toString());
		
		} catch(IOException e) {			
			log.error("Could not convert file ", e);
			return false;
		} finally { 
			try { input.close(); } catch (IOException e) { /* ignore */ }
			try { output.close(); } catch (IOException e) { /* ignore */ }
		}
		return true;
	}
	
	/**
	 * 
	 * @param filename
	 * @param extension
	 * @return
	 */
	public static String changeExtension(String filename, String extension) { 
		File file = null;
		try { 
			file = new File(filename);
			//file.createNewFile();	// if one does not already exist
			
			// Get the filename with the new extension
			StringBuffer buffer = new StringBuffer();
			buffer.append(file.getParent()).
				append(File.separator).
				append(removeExtension(file.getName())).
				append(".").
				append(extension.replace(".", ""));
			
			file = new File(buffer.toString());
			
		} catch (Exception e) { 
			e.printStackTrace(System.out);
		}		
		return file.getAbsolutePath();
		
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	public static String removeExtension(String filename) { 		
		int lastPeriod = filename.lastIndexOf(".");	
		if (lastPeriod > 0) { 
			return filename.substring(0, lastPeriod);
		}		
		return filename;
		
		
	}
	
	
	public static int calculateYearsBetween(Date fromDate, Date toDate) { 
		
		Calendar from = Calendar.getInstance();
		from.setTime(fromDate);
		
		Calendar to = Calendar.getInstance();
		to.setTime(toDate);
		
		int yearsBetween = from.get(Calendar.YEAR) - to.get(Calendar.YEAR);
		if (to.get(Calendar.DAY_OF_YEAR) > from.get(Calendar.DAY_OF_YEAR)) { 
			yearsBetween -= 1;
		}
		return yearsBetween;
	}
	
	
	
	
}
