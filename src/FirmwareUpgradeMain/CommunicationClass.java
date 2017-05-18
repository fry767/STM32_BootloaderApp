package FirmwareUpgradeMain;

import gnu.io.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
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
	private byte rcv_state = 0;
	private byte rcv_cmd = 0;
	private byte byte_rcv = 0;
	private byte device = 0;
	private byte name_length = 0;
	private byte msg_size = 0;
	private byte bufferChecksum = 0;
	/* Public value*/
	public int flash_size[] = {0,0,0,0,0,0,0,0,0,0} ;
	public StringBuilder[] deviceName = new StringBuilder[10];
	//the timeout value for connecting with the port
	final static int TIMEOUT = 2000;
	
	//a string for recording what goes on in the program
	//this string is written to the GUI
	String logText = "";
	String deviceText = "";
	
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
	            switch(rcv_state)
	            {
		            case 0:
		            	if(singleData == 0x04)
		            	{
		            		bufferChecksum += singleData;
		            		rcv_state++;
		            		logText = "0x04 receive";
		    		        window.textArea.append(logText + "\n");
		            	}
		            	break;
		            case 1:
		            	if(singleData == 0x7C)
		            	{
		            		bufferChecksum += singleData;
		            		rcv_state++;
		            		logText = "0x7C receive";
		    		        window.textArea.append(logText + "\n");
		            	}
		            	else
		            		rcv_state = 0;
		            	break;
		            case 2 :
		            	bufferChecksum += singleData;
		            	rcv_cmd = singleData;
		            	rcv_state ++;
		            	logText = "Command Receive";
	    		        window.textArea.append(logText + "\n");
		            	break;
		            case 3:
		            	switch (rcv_cmd)
		            	{
			            	case 0x01: /* Flash size CMD*/
			            		byte_rcv++;
			            		bufferChecksum += singleData;
			            		switch(byte_rcv)
			            		{
			            		case 1:
			            			msg_size = singleData;
			            			break;
			            		case 2:
			            			device = singleData;
			            			break;
			            		case 3:
			            			flash_size[device] = singleData << 8 & 0xFF00;
			            			break;
			            		case 4:
			            			flash_size[device] = singleData & 0x00FF;
			            			rcv_state ++;
			            			byte_rcv=0;
			            			rcv_cmd = (byte) 0xFF;
			            			device = (byte) 0xFF;
			            			break;
			            		}
			            		break;
			            	case 0x11 : /* Name Attribution CMD*/
			            		byte_rcv++;
			            		bufferChecksum += singleData;
			            		if(byte_rcv == 1)
			            			name_length = (byte) (singleData - 1);
			            		else if(byte_rcv == 2)
			            		{
			            			device = singleData;
			            			deviceName[device].append("");
			            		}
			            		else if(byte_rcv > 2 && byte_rcv <= name_length + 1)
			            		{ 
			            			deviceName[device].append(singleData);	
				    		       
			            		}
			            		else
			            		{
			    		        	logText = "Name receive";
				    		        window.textArea.append(logText + "\n");
				    		        rcv_state ++;
			            			byte_rcv=0;
			            			device = (byte) 0xFF;
			            			rcv_cmd = (byte) 0xFF;	
			            		}
			            		
			            		break;
			            	case 0x21: /* Erase CMD*/
			            		rcv_state = (byte) 0x99;
			            		break;
			            	case 0x31 :/* Write CMD*/
			            		rcv_state = (byte) 0x99;
			            		break;
			            	case 0x41: /* JUMP CMD*/
			            		rcv_state = (byte) 0x99;
			            		break;
			            	case 0x51 :/* CHANGE COM SPEED CMD*/
			            		rcv_state = (byte) 0x99;
			            		break;
		            		case 0x61: /* GET VALUE CMD*/
		            			rcv_state = (byte) 0x99;
			            		break;
		            		case 0x71: /* Write Firmware*/
		            			rcv_state = (byte) 0x99;
		            			break; 
			            	default: 
			            		rcv_state = 0;
			            		logText = "Command received not yet implemented (" + new String(new byte[] {singleData}) +")";
			    		        window.textArea.setForeground(Color.red);
			    		        window.textArea.append(logText + "\n");
			            		break;
		            	}
		            	
		            	break;
		            case 4 : /*Checksum value*/
		            	rcv_state = 0;
	            		int inverse = ~bufferChecksum;
	            		inverse += 1;
	            		if(inverse != singleData)
	            		{
	            			logText = "Checksum doesnt match";
		    		        window.textArea.setForeground(Color.red);
		    		        window.textArea.append(logText + "\n");
	            		}else
	            		{
	            			logText = "Checksum match !!!!!";
		    		        window.textArea.append(logText + "\n");
	            		}
	            		
	            		break;
		            default:
		            	break;
	            }
	  
	            logText = new String(new byte[] {singleData});
	            window.textArea.append(logText);

	            
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

	public void WriteDeviceName(String wantedDeviceName) throws IOException {
		// TODO Auto-generated method stub
		byte[] msg_header = {0x04,0x7C};
		byte msg_command = 0x10;
		byte msg_length = (byte) wantedDeviceName.length();
		byte[] bWantedDeviceName = wantedDeviceName.getBytes();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(msg_header);
		out.write(msg_command);
		out.write(msg_length);
		out.write(bWantedDeviceName);
		byte[] bufferForChecksum = out.toByteArray();
		
		if(bConnected != false)
		 {
			 try
			 {
				output.write(msg_header);
				output.flush();
	        	Thread.sleep(10);
	            output.write(msg_command);
	            output.write(msg_length);
	            output.flush();
	            Thread.sleep(10);
		        output.write(bWantedDeviceName);
		        output.flush();
		        Thread.sleep(10);
		        output.write(calculateChecksum(bufferForChecksum,(short)bufferForChecksum.length));
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
	private byte calculateChecksum (byte[] buffer,short size)
	{
		byte msg_checksum = 0;
		
		for(short i = 0; i < size; i++)
		{
			msg_checksum += buffer[i];
		}
		int inverse = ~msg_checksum;
		inverse +=1;
		return (byte)inverse;
		
	}
	private byte[] swapByteArray(byte[] buffer,int size)
	{
		byte[] tmp = Arrays.copyOf(buffer, size);
		for(int i = 0; i < size ; i+=4)
		{
			if(size - i >= 4)
			{
				buffer[i] = tmp[i+3];
				buffer[i+1] = tmp[i+2];
				buffer[i+2] = tmp[i+1];
				buffer[i+3] = tmp[i];
			}else
			{
				switch(size - i)
				{
				case 1:
					buffer[i] = tmp[i];
					break;
				case 2:
					buffer[i] = tmp[i+1];
					buffer[i+1] = tmp[i];
					break;
				case 3:
					buffer[i] = tmp[i+2];
					buffer[i+1] = tmp[i+1];
					buffer[i+2] = tmp[i];
					break;
				default:
					break;
				}
			}
		}
		return buffer;
	}
	
}
