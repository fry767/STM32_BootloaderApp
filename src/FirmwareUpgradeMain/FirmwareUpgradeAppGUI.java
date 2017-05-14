package FirmwareUpgradeMain;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JComboBox;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.List;
import java.awt.Window;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextArea;



public class FirmwareUpgradeAppGUI extends javax.swing.JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FirmwareUpgradeAppGUI window = new FirmwareUpgradeAppGUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	private String logger = "";
	CommunicationClass Com = null;
	File CurrentChosenFile = null;
	BufferedReader br = null;
	byte checksum;
	int fileLength;
	byte[] bytesList = null;
	public FirmwareUpgradeAppGUI() {
		initialize();
		Com = new CommunicationClass(this);
		Com.searchForPorts();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() 
	{
		checksum = 0;
		fileLength = 0;
		frame = new JFrame();
		frame.setBounds(100, 100, 369, 325);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		comboBox = new javax.swing.JComboBox();
		
		JLabel lblCom = new JLabel("Com");
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Com.connect();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Com.initListener();
			}
		});
		
		JButton btnDisconnect = new JButton("Disconnect");
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Com.disconnect();
			}
		});
		
		JButton btnNewFirmware = new JButton("New Firmware...");
		btnNewFirmware.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(null);
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) 
				{
					 logger = "You chose to open this file: " +
								chooser.getSelectedFile().getName();
					 textArea.setForeground(Color.black);
					 textArea.append(logger + "\n");
					 CurrentChosenFile = chooser.getSelectedFile();
					Path p = Paths.get(CurrentChosenFile.getPath());
					try {
						bytesList = Files.readAllBytes(p);
						checksum = 0;
						for(int i = 0; i < bytesList.length;i++)
						{
							checksum +=bytesList[i];
						}
						int inverse = ~checksum;
						inverse +=1;
						checksum = (byte) inverse;
						fileLength = bytesList.length;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						
				
					

					
					 
				}
				
				
			}
		});
		
		textArea = new javax.swing.JTextArea();
		
		JButton btnSendFirmware = new JButton("Send Firmware");
		btnSendFirmware.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(fileLength != 0)
				{
					Com.writeFirmware(fileLength, bytesList, checksum);
				}
			}
		});
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblCom)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(28)
							.addComponent(btnConnect)
							.addGap(18)
							.addComponent(btnDisconnect, GroupLayout.PREFERRED_SIZE, 132, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(9)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(btnNewFirmware)
								.addGroup(groupLayout.createSequentialGroup()
									.addGap(10)
									.addComponent(btnSendFirmware)))
							.addGap(18)
							.addComponent(textArea, GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblCom)
						.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnConnect)
						.addComponent(btnDisconnect, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(textArea, GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnNewFirmware)
							.addGap(12)
							.addComponent(btnSendFirmware)))
					.addContainerGap())
		);
		frame.getContentPane().setLayout(groupLayout);
	
	}
	@SuppressWarnings("rawtypes")
	public javax.swing.JComboBox comboBox;
	public javax.swing.JTextArea textArea;
}
