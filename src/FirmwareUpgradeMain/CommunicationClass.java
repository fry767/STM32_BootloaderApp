package FirmwareUpgradeMain;

import gnu.io.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;

public class CommunicationClass implements SerialPortEventListener 
{
	FirmwareUpgradeAppGUI window = null;
	//for containing the ports that will be found
	private Enumeration ports = null;
	//map the port names to CommPortIdentifiers
	private HashMap portMap = new HashMap();
	
	//this is the object that contains the opened port
	private CommPortIdentifier selectedPortIdentifier = null;
	private SerialPort serialPort = null;
	
	//input and output streams for sending and receiving data
	private InputStream input = null;
	private OutputStream output = null;
	
	//just a boolean flag that i use for enabling
	//and disabling buttons depending on whether the program
	//is connected to a serial port or not
	private boolean bConnected = false;
	private boolean isDataReceive = false;
	private byte	key = 0;
	//the timeout value for connecting with the port
	final static int TIMEOUT = 2000;
	
	//a string for recording what goes on in the program
	//this string is written to the GUI
	String logText = "";
	
	@SuppressWarnings("restriction")
	private void setSerialPortParameters() throws IOException {
	    int baudRate = 115200; // 57600bps
	 
	        try {
	            // Set serial port to 57600bps-8N1..my favourite
	            serialPort.setSerialPortParams(
	                    baudRate,
	                    SerialPort.DATABITS_8,
	                    SerialPort.STOPBITS_1,
	                    SerialPort.PARITY_NONE);
	 
	            serialPort.setFlowControlMode(
	                    SerialPort.FLOWCONTROL_NONE);
	        } catch (UnsupportedCommOperationException ex) {
	        	 logText = "Unsupported serial port parameter ";
	         window.textArea.append(logText + "\n");
	         window.textArea.setForeground(Color.RED);
	        throw new IOException("Unsupported serial port parameter");
	    }
	}
	
	public CommunicationClass(FirmwareUpgradeAppGUI window)
	{
	    this.window = window;
	}
	
	//search for all the serial ports
	//pre: none
	//post: adds all the found ports to a combo box on the GUI
	@SuppressWarnings("restriction")
	public void searchForPorts()
	{
	    ports = CommPortIdentifier.getPortIdentifiers();
	
	    while (ports.hasMoreElements())
	    {
	        CommPortIdentifier curPort = (CommPortIdentifier)ports.nextElement();
	
	        //get only serial ports
	        if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL)
	        {
	        	window.comboBox.addItem(curPort.getName());
	            portMap.put(curPort.getName(), curPort);
	        }
	    }
	}
	
	public void connect() throws Exception
	{
	    String selectedPort = (String)window.comboBox.getSelectedItem();
	    selectedPortIdentifier = (CommPortIdentifier)portMap.get(selectedPort);
	
	    CommPort commPort = null;
	
	    try
	    {
	    	
	        //the method below returns an object of type CommPort
	        commPort = selectedPortIdentifier.open("FirmwareUpgradeApp", TIMEOUT);
	        serialPort = (SerialPort)commPort;
	        setSerialPortParameters();
	        //the CommPort object can be casted to a SerialPort object
	       
	        setConnected(true);
	        
	        output = serialPort.getOutputStream();
	        input = serialPort.getInputStream();
	        
	        //logging
	        logText = selectedPort + " opened successfully.";
	        window.textArea.setForeground(Color.black);
	        window.textArea.append(logText + "\n");
	    }
	    catch (PortInUseException e)
	    {
	        logText = selectedPort + " is in use. (" + e.toString() + ")";
	        window.textArea.setForeground(Color.RED);
	        window.textArea.append(logText + "\n");
	    }
	    catch (Exception e)
	    {
	    	serialPort.close();
	        
	        logText = "Failed to open " + selectedPort + "(" + e.toString() + ")";
	        window.textArea.append(logText + "\n");
	        window.textArea.setForeground(Color.RED);
	        throw e;
	    }
	}
	 public void disconnect()
	    {
	        //close the serial port
	        try
	        {
	            serialPort.removeEventListener();
	            serialPort.close();
	            input.close();
	            output.close();
	            setConnected(false);
	
	
	            logText = "Disconnected.";
	            window.textArea.setForeground(Color.red);
	            window.textArea.append(logText + "\n");
	        }
	        catch (Exception e)
	        {
	            logText = "Failed to close " + serialPort.getName() + "(" + e.toString() + ")";
	            window.textArea.setForeground(Color.red);
	            window.textArea.append(logText + "\n");
	        }
	    }
	final public boolean getConnected()
	{
	    return bConnected;
	}
	
	public void setConnected(boolean bConnected)
	{
	    this.bConnected = bConnected;
	}
	public InputStream getSerialInputStream() {
	    return input;
	}
	public OutputStream getSerialOutputStream() {
	    return output;
	}
	@SuppressWarnings("restriction")
	public void initListener()
	{
	    try
	    {
	        serialPort.addEventListener(this);
	        serialPort.notifyOnDataAvailable(true);
	    }
	    catch (TooManyListenersException e)
	    {
	        logText = "Too many listeners. (" + e.toString() + ")";
	        window.textArea.setForeground(Color.red);
	        window.textArea.append(logText + "\n");
	    }
	}
	@Override
	public void serialEvent(SerialPortEvent evt) {
	    if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE)
	    {
	        try
	        {
	        	
	            byte singleData = (byte)input.read();
	            logText = new String(new byte[] {singleData});
	            window.textArea.append(logText);
	            if(singleData == 119)
	            	isDataReceive = true;
	            
	        }
	        catch (Exception e)
	        {
	            /* Failed to Read Data*/
	        }
	    }
		
	}
	 public void writeFirmware(int fileLength,byte[] firmware,byte checksum)
	 {
		 if(bConnected != false)
		 {
	        try
	        {
	        	byte[] key = {77,68,75,69,89};/*MDKEY*/
	        	output.write(key);
	        	output.flush();
	        	Thread.sleep(10);
	            output.write(ByteBuffer.allocate(4).putInt(fileLength).array());
	            output.flush();
	            Thread.sleep(100);
	            output.write(firmware);
	            output.flush();
	            Thread.sleep(10);
	            //will be read as a byte so it is a space key
		        output.write(checksum);
		        output.flush();
		    }
		    catch (Exception e)
		    {
		        logText = "Failed to write data. (" + e.toString() + ")";
		        window.textArea.setForeground(Color.red);
		        window.textArea.append(logText + "\n");
	        }
	     }
	  }
}
