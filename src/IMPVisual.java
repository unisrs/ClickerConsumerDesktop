import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


// TODO: Auto-generated Javadoc
/**
 * The Class IMPVisual.
 */
public class IMPVisual {
	
	/** The _instance. */
	private static IMPVisual _instance;
	
	/** The hub. */
	private IMPCommunicationHub hub;
	
	/** The Constant FRAME_WIDTH. */
	private static final int FRAME_WIDTH = 650;
	
	/** The Constant FRAME_HEIGHT. */
	private static final int FRAME_HEIGHT = 400;
	
	/** The frame. */
	private JFrame frame;
	
	/** The ip field. */
	JTextField ipField; 
	
	/**
	 * Instantiates a new iMP visual.
	 */
	private IMPVisual(){
		buildFrame();
	}
	

	
	/**
	 * Gets the single instance of IMPVisual.
	 *
	 * @return single instance of IMPVisual
	 */
	public static synchronized IMPVisual getInstance(){
        if (_instance == null) {
            _instance = new IMPVisual();
        }
        return _instance;
	}
	

	
	/**
	 * Builds the frame.
	 */
	private void buildFrame(){
		frame = new JFrame();
		frame.setTitle("Input Management Package");
		frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setLayout(new GridLayout(1,1));
		frame.setLayout(new FlowLayout());
		
		frame.add(buildPanel());
		JPanel panel = new JPanel();
		frame.add(panel);
		frame.setVisible(true);
	}

	/**
	 * Builds the panel.
	 *
	 * @return the j panel
	 */
	private JPanel buildPanel() {
		JPanel panel = new JPanel();
//		panel.setLayout(new GridLayout(1,2));
//		panel.setLayout(new GridLayout());
//		panel.setLayout(new FlowLayout());
		panel.add(buildConnectionInput());
		
		ConsumerSelectionBox consumerBox = new ConsumerSelectionBox(this);
		panel.add(consumerBox);
		
		return panel;
		
	}
	
	/**
	 * Builds the connection input.
	 *
	 * @return the j panel
	 */
	private JPanel buildConnectionInput(){
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridLayout(1,2));
		JPanel myPanel = new JPanel();
		JLabel ipLabel = new JLabel();
		ipLabel.setText("Server IP Address:");
//		myPanel.setLayout(new GridLayout(2,1));
		myPanel.setLayout(new FlowLayout());
//		myPanel.setSize((FRAME_WIDTH-10)/2, (FRAME_HEIGHT-10)/2);
		topPanel.add(ipLabel);
		
		ipField = new JTextField();
		ipField.setText("127.0.0.1");
		
		JButton connectButton = new JButton();

		connectButton.setText("Connect");
		connectButton.setToolTipText("Connect to Server");
		ConnectListener c = new ConnectListener();
		connectButton.addActionListener(c);
		
		ipField.addActionListener(c);
		
		topPanel.add(ipField);
		myPanel.add(topPanel);
		myPanel.add(connectButton);
		
		return myPanel;
	}
	
	
	/**
	 * Gets the consumers.
	 *
	 * @return the consumers
	 */
	public ArrayList<String> getConsumers(){
		System.out.println("Calling hub.getconsumers()");
		if (hub==null){
			this.hub = IMPCommunicationHub.getInstance();
		}
		return hub.getConsumers();
	}

	/**
	 * Sets the hub.
	 *
	 * @param hub the new hub
	 */
	public void setHub(IMPCommunicationHub hub){
		this.hub = hub;
	}
	
	/**
	 * The listener interface for receiving connect events.
	 * The class that is interested in processing a connect
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addConnectListener<code> method. When
	 * the connect event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see ConnectEvent
	 */
	private class ConnectListener implements ActionListener {

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			String s = ipField.getText();
			hub.setIP(s);
			System.out.println("CONNECT CONNECT CONNECT");
			
		}
		
	}
	
}
