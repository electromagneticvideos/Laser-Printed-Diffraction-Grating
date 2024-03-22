/* 
 
Description.
 
This Program generates a HP PCL that can be used to print a diffraction grating and either saves it
to a file or sends it to the network port of a printer. 
 
License

COPYRIGHT: This software and other files are copyright © 2024 by Peter Jones.

TERMS OF USE: You are free to download, use and modify this software and associated 
files subject to these conditions:

    If you use it or a diffraction (or similar) grating made with it in any public performance, 
    document, or writing, you must give credit by reasonably indicating to your audience it 
    came from YouTube channel @ElectromagneticVideos and if possible provide the link to the 
    applicable video https://youtu.be/WwQXQrm33JM in someplace like a text description where 
    they can click on it.

    If you make changes to fix or improve the software, and wish to distribute those changes, 
    it must include this license. You must also upload the improved software to this 
    repository (https://github.com/electromagneticvideos/Laser-Printed-Diffraction-Grating) so 
    others can benefit.

    Warranty: There is no warranty. This software and associated files are provided without support of any kind.

    Exclusion of Liability. The author accepts no liability whatsoever for any use this 
    software or associated files.

If you do not accept any or all of these terms, or if they are not allowed in or made invalid 
by law in any regions that may apply to you or your use of it, you may not download or use 
this software or associated files.

If there is an issue with this license for you intended use, please contact electromagneticvideos [at] gmail.com
 
 
 
 */



import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
 
 
public class Generate_Diffraction_Grating 
{

    static String version = "1.00" ;


	
	
	/**
	 * Untility class to make a byte array out of two smaller ones.
	 *  
	 * @param b1 First byte  array
	 * @param b2 Second byte  array
	 * @return b1 "+" b2
	 */
	static byte[] concat(byte b1[], byte b2[])
	{
	
	   byte b[] = new byte[b1.length+b2.length];
	   
	   
	   for (int counter = 0; counter<b1.length; counter++)
		{
			b[counter] = b1[counter];
			 
		}
	   for (int counter = 0; counter<b2.length; counter++)
		{
			b[counter+b1.length] = b2[counter];
			 
		}
	   
	   return b;
	}
	
	/**
	 * Takes an array of bytes, and creates a bigger array of bytes by 
	 * duplicating the content of the input array as many times as requested
	 *  
	 * @param N
	 * @param values
	 * @return
	 */
	static byte[] make_and_fill_an_array_of_characters(int N, byte[] values)
	{
		byte[] tmp = new byte[N*values.length];
		
		for (int counter = 0; counter<N; counter++)
		{
			int start_position = counter*values.length;
			for (int i = 0; i<values.length; i++)
			{
				
				int x = start_position+i;
			    tmp[x] = values[i];
			}
		}
		 
		return tmp;
	}
	
	
	
	
	// HP PCL fixed commands
	
	
	static char  ESC  = 27;
	static String RESET = ESC+ "E";
	static String SET_LETTER_SIZE = ESC+ "&l2A";
	static String SET_PORTRAIT_ORIENTATION = ESC+ "&l0O";
	static String ZERO_TOP_MARGIN = ESC+ "&l0E";
	static String VERTICAL_MOTION_INDEX_8_48TH= ESC+ "&l8C";
	
	static String PERFORATION_SKIP1 = ESC+ "&l0L";
	static String RESET_CURSOR = ESC+ "&a0R";
	static String SYMBOL_SET_LATION_1 = ESC+ "(0N";
	static String FIXED_CHARACTER_SPACING = ESC+ "(s0P";
	static String PITCH_10_CHARS_INCH = ESC+ "(s10H";
	static String TYPEFACE_COURIER = ESC+ "(s4099T";
	 
	static String Graphics_Presentation_ALONG_WIDTH= ESC+ "*r3F";
	  
	static String Start_Raster_Graphics_X0= ESC+ "*r0A";
	static String Raster_Graphics_NO_COMPRESSION= ESC+ "*b0M";
	static String End_Raster_Graphics= ESC+ "*rC";
	
	
	// HP PCL variable commands
	  
	static String Graphics_Resolution(int dpi)
	{
		return ESC+ "*t"+dpi+"R";
	}
	
	static String Source_Raster_Height(int height_in_pixels)
	{
		return ESC+ "*r"+height_in_pixels+"T";
	}
	
	static String Source_Raster_Width(int width_in_pixels)
	{
		return ESC+ "*r"+width_in_pixels+"S";
	}
	
	static String Raster_Y_Offset(int  pixels)
	{
		return ESC+ "*b"+pixels+"Y";
	}
	 
	static byte[] Transfer_a_Raster_Data_Row(byte[] data)
	{
	 
	
		
	  String s  = ESC+ "*b"+data.length+"W" ;
	  
	  byte[] b  =s.getBytes();
	  
	  byte[] result = concat(b  , data);
	  
	  return result;
	  
	}
	
	/**
	 * Repeats "Transfer_a_Raster_Data_Row(byte[] data)" N times
	 * 
	 * @param N  Number of times to repeat "Transfer_a_Raster_Data_Row(byte[] data)"
	 * @param data The byte array of pixels for "Transfer_a_Raster_Data_Row(byte[] data)"
	 * @return A byte array with the data from "Transfer_a_Raster_Data_Row(byte[] data)" repeated N times
	 */
	static byte[] draw_N_indentical_rows_of_these_pixels(int N, byte[] data)
	{
		byte datab[]  = Transfer_a_Raster_Data_Row(data);
		
		byte tmp[] = datab;
		for (int counter = 1; counter<N; counter++)
		{
			tmp =  concat( tmp,  datab);
		}
		return tmp;
	}
	
	
	/* **********************  Here is where we define the dpi and pixel pattern of the  grating ********* */
	
	  
	static int PRINT_RESOLUTION_IN_DPI = 600;    //600 is what I uses for the 1001001001 grating 
	                                             //change to 300 if you find the pixels smear together
	                                             //I have used 75 dpi to easily see the pattern
	 
	
	static byte[] raster_bit_pattern_1010101 = {(byte)0b01010101, (byte)0b01010101 , (byte)0b01010101 } ; //works at 300dpi
	static byte[] raster_bit_pattern_1001001 = {(byte)0b10010010, (byte)0b01001001 , (byte)0b00100100 } ; //works at 600dpi
	  

	static byte[] raster_bit_pattern = raster_bit_pattern_1001001 ; //Select which of the above bit patterns you want to use
	
	
	/* **********************  Here is where the HP PCL data is created based on the above  ********* */
	
	
	
	static String begin_page = RESET  + SET_LETTER_SIZE + SET_PORTRAIT_ORIENTATION + ZERO_TOP_MARGIN + VERTICAL_MOTION_INDEX_8_48TH;
	 
	static String begin_raster_graphics =  Graphics_Presentation_ALONG_WIDTH 
                                         + Graphics_Resolution(PRINT_RESOLUTION_IN_DPI)
                                         + Source_Raster_Height(300)
                                         + Source_Raster_Width(300)
                                         + Start_Raster_Graphics_X0
                                         + Raster_Graphics_NO_COMPRESSION;
	 
	 
	static byte[]  begin_bytes = (begin_page +begin_raster_graphics).getBytes();	
	
	
	static int NUMBER_OF_ROWS_IN_RASTER_IMAGE = 300;
	static int NUMBER_OF_TIMES_TO_REPEAST_BIT_PATTERN_FOR_EACH_ROW = 13;
	static byte[]  bytes_raster  =draw_N_indentical_rows_of_these_pixels(NUMBER_OF_ROWS_IN_RASTER_IMAGE , make_and_fill_an_array_of_characters(NUMBER_OF_TIMES_TO_REPEAST_BIT_PATTERN_FOR_EACH_ROW, raster_bit_pattern   ));
	 	 	
			 
	static byte[]   bytes_begin_and_raster = concat(begin_bytes,bytes_raster );
	
	static byte[]   hppcl_data_to_print = concat(bytes_begin_and_raster ,End_Raster_Graphics.getBytes());
	
  
	/***************** Program that sends or saves the HP PCL grating **************************/
	
	public static void main(String[] args) 
	{
  
			 
					
					int number_of_args = args.length;
					String filename = null;
					String printer_ip = null;
					
					
					if (number_of_args==2)
					{
						if ("-print".contentEquals(args[0]))
						{
						    printer_ip = args[1];
						}
						else if ("-save".contentEquals(args[0]))
						{
							filename = args[1];
						}
						
					}
					
					
					
					if ((printer_ip==null)&&(filename==null))
					{
						System.out.println("Error: Use this program as follows:");
						System.out.println();
						System.out.println("To send a grating directly to the printer, ");
						System.out.println();
						System.out.println("             java -jar Generate_Diffraction_Grating.jar -print printers_IP_number");
						System.out.println(" example     java -jar Generate_Diffraction_Grating.jar -print 192.168.1.245");
						System.out.println();
						System.out.println("To save the grating directly as a file, ");
						System.out.println();
						System.out.println("             java -jar Generate_Diffraction_Grating.jar -save  file_path_name");
						System.out.println("             java -jar Generate_Diffraction_Grating.jar -save  my_grating.prn");
						System.out.println();
						                             
						System.exit(-1);
					}
					
					 
					 
					if (filename!=null) System.out.println("File \""+filename+"\"");
					if (printer_ip!=null) System.out.println("Printer IP Number \""+printer_ip+"\"");
					
					
				   
					//Send to printer */
					
					
					if (printer_ip!=null) 
					{	
					 int  raw_jetdirect_appsocket_port = 9100;
					 try 
					 {
						    System.out.println("Starting sending to printer");
					        Socket socket = new Socket(printer_ip, raw_jetdirect_appsocket_port);
					        BufferedOutputStream out   =  new BufferedOutputStream(socket.getOutputStream(), 32768) ;   
							out.write(hppcl_data_to_print);
					        out.flush();
					        out.close();
					        socket.close();
					  }
					  catch (IOException e)
					  {
						  System.out.println("Error sending to printer");
						  e.printStackTrace();
						  System.exit(-1);
					  }
					 System.out.println("Finished sending to printer");
					}
					
					
					if (filename!=null)
					{
						 
						 try 
						 {
							 System.out.println("Starting writing to file");
						      
							 FileOutputStream stream = new FileOutputStream(filename);
							 stream.write(hppcl_data_to_print);
							 stream.close();
							 System.out.println("Done writing to file");
						  } catch (IOException e) 
						  {
						    System.out.println("Error writing to file"); 
							e.printStackTrace();
						  }
						 
						 
					}
				 
						
	}

}
