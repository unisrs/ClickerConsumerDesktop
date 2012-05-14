import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * The Class IMPCommunicationHub.
 * 
 * Property of University of Northern Iowa
 * 
 * Handles all communication between the Input Management Package and the Server
 * 
 * This is a Singleton class.  Only one instance of me can exist at
 * a time.
 * 
 * At current state, also manages passing of information to Interaction
 * Response Consumers.
 * 
 * 
 */
public class IMPCommunicationHub extends CommunicationHub{

	//denote what a consumer is expecting answers to be formatted as
	private static final String AVERAGE_FORMATTING = "Avg";
	
	private static final String COUNT_FORMATTING = "Count";
	
	private static final String ALL_FORMATTING = "All";

	//delimiters
	private static String SEMI_COLON_SEPARATOR = "`/;";
	
	private static String COMMA_SEPARATOR      = "`/,";
	
	private static String AMPERSAND_SEPARATOR  = "`/&";
	
	private static String COLON_SEPARATOR      = "`/:";
	
	private static String TILDE_SEPARATOR      = "`/~";

	
	/** For connection to the server */
	private Socket socket;
	
	/** Manages connection to the server */
	private Reconnecter reconnecter; 
	
	/** Used to monitor state of connection to the server */
	private boolean waitingForHeartbeat = false;
	
	/** Used to check status of the connection */
	private Timer heartbeatTimer;
	
	/** interval for heartbeatTimer*/
	private int heartbeatSeconds = 15;
	
	/** The reader. */
	private BufferedReader reader;
	
	/** The writer. */
	private PrintWriter writer;
	
	/** The server ip address. */
	private String serverIP;
	
	/** The port in which the Input Management Package will connect to */
	private String loginPort = "7171";
	
	/** How the server will identify me*/
	private String myID;
	
	/** The Admin which I will connect to. */
	private String myAdmin;

	/** My GUI */
	private IMPVisual visual;


	/** The accepted consumers. */
	private String[] acceptedConsumers;
	
	/** The consumer instance. */
	private static ClickerConsumerInterface consumerInstance;
	
	/** The consumer class. */
	private static Class<? extends ClickerConsumerInterface> consumerClass;
	
	/** The available consumers. */
	private Map<String, Class<? extends ClickerConsumerInterface>> availableConsumers;//widget identifier, widget class object
	
	/** The disabled consumers. */
	private Map<String, Class<? extends ClickerConsumerInterface>> disabledConsumers;//Consumers which have been disabled from the visual
	
	/** The active consumer array. */
	private Map<String, ArrayList<ClickerConsumerInterface>> activeConsumerArray;//group identifier, specific widget instance
	
	/** The all answer array. */
	private Map<String, Map<String, Map<String, String>>> allAnswerArray;		//group identifier, <individual identifier, <label, value> > >
	
	/** The count answer array. */
	private Map<String, Map<String, Map<String, String>>> countAnswerArray;     //group identifier, <individual identifier, <index, value> > >
	
	/** The average answer array. */
	private Map<String, Map<String, Map<String, String>>> averageAnswerArray;   //group identifier, <widget index, <"Average", value> > >

	/** The consumption string. */
	private String consumptionString;
	
	/** The temp string. */
	private static String tempString;
	
	/** The current question. */
	private Map<String, String> currentQuestion;

	/** My Instance of myself */
	private static IMPCommunicationHub _instance;

	/**
	 * Instantiates a new instance of myself
	 */
	private IMPCommunicationHub() {
		loadConsumersFromSubdirectory();
		currentQuestion = Collections.synchronizedMap(new HashMap<String, String>());
	}

	/**
	 * Sets the visual.
	 *
	 * @param visual the new visual
	 */
	public void setVisual(IMPVisual visual){
		this.visual = visual;
	}

	/**
	 * Gets the single instance of IMPCommunicationHub.
	 *
	 * @return single instance of IMPCommunicationHub
	 */
	public static synchronized IMPCommunicationHub getInstance() {
		if (_instance == null) {
			_instance = new IMPCommunicationHub();
		}
		return _instance;
	}



	/**
	 * Set my credentials for connecting to the server
	 * Takes in a String containing the Admin I wish to link to
	 * and the ID I will be known by, separated by the COMMA_SEPARATOR
	 * 
	 * @param String credentials 
	 * 
	 * @see COMMA_SEPARATOR
	 */
	@Override
	public void setLoginInfo(String credentials) {
		String[] creds =credentials.split(COMMA_SEPARATOR);

		myAdmin = creds[0];
		myID = creds[1];
	}

	/**
	 * Sets the target IP address of the server and begins the connections sequence
	 * 
	 * @param IP Address
	 */
	@Override
	public void setIP(String ipAddress) {
		myAdmin = "frederis";
		myID = "IMP1";
		serverIP = ipAddress;
		reconnecter = new Reconnecter(serverIP);
		new Thread(reconnecter).start();
	}

	/**
	 * Send a message to the server
	 * 
	 * @param message
	 */
	@Override
	public void sendMessage(String message) {
		writer.println(message);
		writer.flush(); 

	}

	/**
	 * 
	 * Reads a message from the server
	 * 
	 * @return Message from the server
	 * @throws IOException
	 * 
	 */
	@Override
	public String readMessage() throws IOException{
		return reader.readLine();
	}

	/**
	 * 
	 * called when disconnected from the server and
	 * initiates reconnection
	 * 
	 */
	@Override
	public void gotDisconnected() {
		closeConnections();  
		reconnecter = new Reconnecter(serverIP);
		new Thread(reconnecter).start();
	}

	/**
	 * 
	 * Cancels the heartbeat timer
	 * 
	 */
	@Override
	public void closeConnections() {
		waitingForHeartbeat = false;
		heartbeatTimer.cancel();
	}




	/**
	 * 
	 * Alert the user of something on the visual
	 * 
	 * 
	 */
	@Override
	public void alertUser(String message) {
		//TODO: implement this
		//visual.alertUser(message);

	}

	/**
	 * The Class Reconnecter.
	 * 
	 * Handles connection/reconnection to the server
	 * 
	 */
	private class Reconnecter implements Runnable {
		
		String ip;
		int adminPort = 7700;
		int timeout = 5000;
		boolean isConnected;

		/**
		 * Instantiates a new reconnecter.
		 *
		 * @param ip the ip
		 */
		public Reconnecter(String ip) {
			this.ip = ip;
			isConnected = false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			int retryCount = 0;
			while (!isConnected && retryCount < 2) {
				try {               
					SocketAddress sockaddr = new InetSocketAddress(ip, Integer.parseInt(loginPort));
					Socket newSocket = new Socket();
					newSocket.connect(sockaddr, timeout);
					newSocket.setKeepAlive(true);
					socket = newSocket;
					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					writer = new PrintWriter(socket.getOutputStream(), false);
					sendMessage(myAdmin+COMMA_SEPARATOR+myID);
					System.out.println("sent myAdmin and myID to server ");
					isConnected = true;
					sendMessage(getConsumptionString());
					startListening();
				} catch (Exception e) {
					try {
						System.out.println("failed to connect, waiting and trying again");
						retryCount++;
						Thread.sleep(100);
					} catch (InterruptedException ie) {}
				}
			}
			//     Log.d("RECONNECT", "Out of reconnection loop");
			//     reconnectingDialog.dismiss();
			if (isConnected) {
				System.out.println("connected");
				//startListening();
				//subHandler.sendEmptyMessage(RECONNECT_SUCCESS);     
			} else {
				System.out.println("not connected");
				//subHandler.sendEmptyMessage(RECONNECT_FAILED);
			}
		}

	}


	/**
	 * 
	 * Called when a Heartbeat is received from the server
	 * 
	 */
	public void receivedHeartBeat() {
		waitingForHeartbeat = false;
		System.out.println("received heartbeat");
	}


	/**
	 * 
	 * Called to initialize listening for communication on the server
	 * 
	 */
	public void startListening() {
		heartbeatTimer = new Timer();
		heartbeatTimer.scheduleAtFixedRate(new HeartbeatTask(), 15000, heartbeatSeconds * 1000);
		//inputManager = new InputManagementThread();
		//		HandlingRunnable runner = new HandlingRunnable();
		//		new Thread(runner).run();
		Thread thread = new Thread(new HandlingRunnable());
		thread.start();
	}


	/**
	 * The Class HeartbeatTask.
	 * 
	 * Manages monitoring health of the connection with the server
	 * 
	 */
	private class HeartbeatTask extends TimerTask {
		
		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		public void run() {
			if (waitingForHeartbeat) {
				try {
					socket.close();
				} catch (IOException e) {}
				System.out.println("Got Disconnected, at Line 210");
				gotDisconnected();
			} else {
				waitingForHeartbeat = true;
				System.out.println("sending heartbeat request");
				sendMessage(STILL_CONNECTED_REQUEST);
			}
		}
	}
	
	/**
	 * The Class HandlingRunnable.
	 * 
	 * Manages receiving new input from the Server.
	 * 
	 */
	private class HandlingRunnable implements Runnable{
		
		/** The Constant HEARTBEAT_RESPONSE. */
		private static final String HEARTBEAT_RESPONSE = "YesImHere";
		
		private String str;
		
		private String[] strParts;
		
		private boolean run;
		
		private String[] emptyStringArray = {""};

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run(){
			run = true;
			while(run){
				try {

					str = readMessage();

					if (str==null){
						gotDisconnected();
						break;
					}

					else if (str==HEARTBEAT_RESPONSE){
						receivedHeartBeat();
					}
					else{
						strParts = str.split(SEMI_COLON_SEPARATOR);

						if(strParts[0].equalsIgnoreCase("Open") || strParts[0].equalsIgnoreCase("OpenClickPad")){
							//expecting: Open`/;ID`;/Widgets`/&pluginName`/:typeField1`/,index`/,index`/:typeField2`/,index`/;pluginName2`/:typeField1`/,index`/&groupName`/,groupName2`/:#
							System.out.println("Question is: "+str);
							String[] temp = str.split(AMPERSAND_SEPARATOR); //question&plugin&group
							//currentQuestion = str;
							try{
								acceptedConsumers = temp[1].split(COMMA_SEPARATOR);
							} catch (ArrayIndexOutOfBoundsException e){
								acceptedConsumers = emptyStringArray;
							}
							for(String groupName : temp[2].split(COMMA_SEPARATOR)){
								String[] groupNameParts = groupName.split(COLON_SEPARATOR);
								currentQuestion.put(groupNameParts[0], str);
								getActivePlugins(acceptedConsumers, groupNameParts[0], currentQuestion.get(groupNameParts[0]));
								allAnswerArray.put(groupNameParts[0], Collections.synchronizedMap(new HashMap<String, Map<String, String>>() ));
							}
						} else if(strParts[0].equals("Close")){
							for(String groupName : strParts[1].split(COMMA_SEPARATOR)){
								removePlugins(groupName);
								removeAnswers(groupName);
							}
						} else if(str.length()>0){
							System.out.println("Received: "+str);//TODO: remove debug printouts
							if(activeConsumerArray.containsKey(strParts[1])){
								processNewInput(strParts[0], strParts[1], strParts[3]);
							}
						}}
				} catch(SocketException e){
					run = false;
				} catch(Exception e){
					run = false;
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Process new input.
	 *
	 * @param clientName the client name
	 * @param clientGroup the client group
	 * @param values the values
	 */
	private void processNewInput(String clientName, String clientGroup, String values){
		//answerArray;	//group identifier, <individual identifier, <label, value> > >
		Map<String, Map<String, String>> groupAnswerMap = null;//map for a specific group
		if(!allAnswerArray.containsKey(clientGroup)){
			groupAnswerMap = Collections.synchronizedMap(new HashMap<String, Map<String, String>>());
		} else {
			groupAnswerMap = allAnswerArray.get(clientGroup);
		}
		Map<String, String> individualCurrentAnswers = null;//map of answers for a specific individual
		if(!groupAnswerMap.containsKey(clientName)){
			individualCurrentAnswers = Collections.synchronizedMap(new HashMap<String, String>());
		} else {
			individualCurrentAnswers = groupAnswerMap.get(clientName);
		}
		individualCurrentAnswers = calculateAll(clientGroup, values);
		groupAnswerMap.put(clientName, individualCurrentAnswers);
		//System.out.println("individualCurrentAnswers: "+individualCurrentAnswers);
		for (ClickerConsumerInterface cci : activeConsumerArray.get(clientGroup)){
			String[] dec = cci.declareConsumptions().split(COLON_SEPARATOR);
			if(dec[1].equalsIgnoreCase("All")){
				Map<String, Map<String, String>> distributableAnswerAll = new HashMap<String, Map<String, String>>();
				distributableAnswerAll.put(clientName, individualCurrentAnswers);
				distributeValues(clientGroup, distributableAnswerAll);

				//TODO: iterate through, add in a No answer category if someone hasn't submitted an answer yet
			} else if (dec[1].equalsIgnoreCase("Count")){
				Iterator iteratePerson = groupAnswerMap.keySet().iterator();//  Person name, index#, value
				String iteratePersonNext = "";
				Map<String, String> totals = new HashMap<String, String>();
				//int totalCount = 0;
				String[] questionParts = currentQuestion.get(clientGroup).split(SEMI_COLON_SEPARATOR);
				String[] widgets = questionParts[3].split(COMMA_SEPARATOR);
				for (String widget : widgets){
					String[] widgetParts = widget.split(COLON_SEPARATOR);
					if(widgetParts[0].equals("B") || widgetParts[0].equals("TOG")){
						totals.put(widgetParts[1], "0");
					} else if (widgetParts[0].equals("COMBO")){
						//widgetParts[2] = options
						String[] comboOptions = widgetParts[2].split(TILDE_SEPARATOR);
						for (String s : comboOptions){
							totals.put(s, "0");
						}
					}
				}
				while(iteratePerson.hasNext()){
					iteratePersonNext = (String) iteratePerson.next();
					Iterator iterateIndex = groupAnswerMap.get(iteratePersonNext).keySet().iterator();
					String iterateIndexNext = "";
					while (iterateIndex.hasNext()){
						iterateIndexNext = (String) iterateIndex.next();
						String nextValue = groupAnswerMap.get(iteratePersonNext).get(iterateIndexNext);
						if(!nextValue.equals(" ")){
							if(totals.containsKey(nextValue)){
								totals.put(nextValue, ""+(Integer.parseInt(totals.get(nextValue)) + 1));
							} else {
								totals.put(nextValue, "1");
							}
						}
					}
				}
				Map<String, Map<String, String>> answersToInsert = new HashMap<String, Map<String, String>>();
				answersToInsert.put("Count", totals);
				countAnswerArray.put(clientGroup, answersToInsert);
				distributeValues(clientGroup, answersToInsert);
			} else if (dec[1].equalsIgnoreCase("Avg")){
				int widgetCount = Integer.parseInt(dec[2]);
				//individualCurrentAnswers      single persons      index : value
				//groupAnswerMap			person     :     index   : value
				Map<String, Integer> averagePersonCounts = new HashMap<String, Integer>();
				Iterator iteratePerson = groupAnswerMap.keySet().iterator();
				String personNext = "";
				Map<String, Integer> totalsToAverage = new HashMap<String, Integer>();
				Map<String, String> person = null;
				while (iteratePerson.hasNext()){
					personNext = (String) iteratePerson.next();
					person = groupAnswerMap.get(personNext);
					Iterator indexIterator = person.keySet().iterator();
					String indexNext = "";
					while(indexIterator.hasNext()){
						indexNext = (String) indexIterator.next();
						Integer value = 0;
						try{
							value = Integer.parseInt(person.get(indexNext));
							if(averagePersonCounts.containsKey(indexNext)){
								averagePersonCounts.put(indexNext, averagePersonCounts.get(indexNext) + 1);
							} else {
								averagePersonCounts.put(indexNext, 1);
							}
						} catch (Exception e) {
							System.out.println("integer.parseint exception");
							e.printStackTrace();
						}
						if(totalsToAverage.containsKey(indexNext)){
							totalsToAverage.put(indexNext, totalsToAverage.get(indexNext) + value);
						} else {
							totalsToAverage.put(indexNext, value);
						}
					}
				}
				Map<String, Map<String, String>> answersToInsert = new HashMap<String, Map<String, String>>();
				Iterator totalsIterator = totalsToAverage.keySet().iterator();
				String index = "";
				while(totalsIterator.hasNext()){
					index = (String) totalsIterator.next();
					float averageValue = (float)totalsToAverage.get(index) / (float)averagePersonCounts.get(index) ; 
					Map<String, String> answerMapToInsert = new HashMap<String, String>();
					answerMapToInsert.put("Average", ""+averageValue);
					answersToInsert.put(index, answerMapToInsert );
				}
				averageAnswerArray.put(clientGroup, answersToInsert);
				distributeValues(clientGroup, answersToInsert);
				//split dec, see if the beginning is average, if it is, then get the count
				//should be Avg,#
			} else {
				System.out.println("Error, unspecified means of answer distribution to "+cci.declareConsumptions());
			}
		}
	}

	/**
	 * Distribute values.
	 *
	 * @param group the group
	 * @param answerMapToDistribute the answer map to distribute
	 */
	private void distributeValues(String group, Map<String, Map<String, String>> answerMapToDistribute){
		//  Person name, index#, value
		//Map<String, ArrayList<ClickerConsumerInterface>> activeConsumerArray;//group identifier, specific widget instance
		System.out.println("output: "+answerMapToDistribute);
		if(activeConsumerArray.containsKey(group)){
			ArrayList<ClickerConsumerInterface> activeConsumers =activeConsumerArray.get(group);
			for(ClickerConsumerInterface cc : activeConsumers){
				Thread thread = new Thread(new MessagePassingRunnable(cc, answerMapToDistribute));
				thread.start();
			}
		}
	}

	/**
	 * The Class MessagePassingRunnable.
	 */
	private class MessagePassingRunnable implements Runnable {
		
		/** The consumer. */
		private ClickerConsumerInterface consumer;
		
		/** The input map. */
		private Map<String, Map<String, String>> inputMap;
		
		/**
		 * Instantiates a new message passing runnable.
		 *
		 * @param cci the cci
		 * @param input the input
		 */
		public MessagePassingRunnable(ClickerConsumerInterface cci, Map<String, Map<String, String>> input){
			consumer = cci;
			inputMap = input;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			consumer.inputData(inputMap);
		}
	}

	/**
	 * Removes the plugins.
	 *
	 * @param string the string
	 */
	private void removePlugins(String string) {
		System.out.println("Close called on: "+string);
		ArrayList<ClickerConsumerInterface> cciArray = activeConsumerArray.get(string);
		for(ClickerConsumerInterface cci : cciArray){
			cci.setActiveStatus(false);
		}
		activeConsumerArray.put(string, null);
		activeConsumerArray.remove(string);	
	}

	/**
	 * Removes the answers.
	 *
	 * @param groupName the group name
	 */
	private void removeAnswers(String groupName){
		allAnswerArray.put(groupName, null);
		allAnswerArray.remove(groupName);

	}

	/**
	 * Load consumers from subdirectory.
	 */
	private void loadConsumersFromSubdirectory(){
		availableConsumers = Collections.synchronizedMap(new HashMap<String, Class<? extends ClickerConsumerInterface>>());
		activeConsumerArray = Collections.synchronizedMap(new HashMap<String, ArrayList<ClickerConsumerInterface>>());
		allAnswerArray = Collections.synchronizedMap(new HashMap<String, Map<String, Map<String, String>>>() );
		countAnswerArray = Collections.synchronizedMap(new HashMap<String, Map<String, Map<String, String>>>() );
		averageAnswerArray = Collections.synchronizedMap(new HashMap<String, Map<String, Map<String, String>>>() );
		File consumerDirectory = new File("./consumers/");
		String[] files = consumerDirectory.list();
		URLClassLoader urlcl = null;
		try {
			urlcl = new URLClassLoader(new URL[]{(consumerDirectory.toURI().toURL())});
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		//ClickerConsumerInterface cca = new ClickerConsumerAdapter();
		System.out.println(files);
		for(String s: files){
			try {
				if(s.length() < 6){
					System.out.println("Filename: "+ s + "is too short to be an appropriate java class file. Skipping.");
					continue;
				}
				consumerClass = (Class<? extends ClickerConsumerInterface>) urlcl.loadClass(s.substring(0,s.length()-6));
				boolean works = ClickerConsumerInterface.class.isAssignableFrom(consumerClass);
				if(works){
					consumerInstance = (ClickerConsumerInterface)consumerClass.newInstance();
					String temporaryConsumption = consumerInstance.declareConsumptions();
					String[] temporaryConsumptionArray = temporaryConsumption.split(",");
					availableConsumers.put(temporaryConsumptionArray[0], consumerClass);
				} else {
					System.out.println(s + " does not properly fit the necessary interface. Skipping.");
				}
			} catch (ClassNotFoundException e) {
				System.out.println(s + " was not an appropriately formed java class file. Skipping.");
				continue;
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoClassDefFoundError e) {
				System.out.println("Invalid class file " + s + " found. Skipping.");
				continue;
			}
		}
	}

	/**
	 * Gets the consumption string.
	 *
	 * @return the consumption string
	 */
	private String getConsumptionString(){
		String tempConsumptionString = "IConsume"+SEMI_COLON_SEPARATOR;
		tempConsumptionString += buildConsumptionNotificationString();
		return tempConsumptionString;
	}

	/**
	 * Builds the consumption notification string.
	 *
	 * @return the string
	 */
	private String buildConsumptionNotificationString(){
		//Each consumption will be a Title,expected value:type;expected value:type
		//no ev:t will imply they take everything
		String tempConsumptionString = "";
		Iterator<String> i = availableConsumers.keySet().iterator();
		while(i.hasNext()){
			tempConsumptionString += i.next()+COMMA_SEPARATOR;
		}
		return tempConsumptionString.substring(0, tempConsumptionString.length()-3);
	}

	/**
	 * Gets the active plugins.
	 *
	 * @param consumerArray the consumer array
	 * @param groupName the group name
	 * @param question the question
	 * @return the active plugins
	 */
	private void getActivePlugins(String[] consumerArray, String groupName, String question){
		for (String s : consumerArray){
			String[] pluginInformation = s.split(COMMA_SEPARATOR);
			if(availableConsumers.containsKey(s)){
				ClickerConsumerInterface cci = null;
				try {
					cci = (ClickerConsumerInterface) availableConsumers.get(pluginInformation[0]).newInstance();
					cci.setID(groupName);
					cci.setQuestion(question);
					cci.setActiveStatus(true);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				if(activeConsumerArray.containsKey(groupName)){
					activeConsumerArray.get(groupName).add(cci);
				} else {
					ArrayList<ClickerConsumerInterface> cciArray = new ArrayList<ClickerConsumerInterface>();
					cciArray.add(cci);
					activeConsumerArray.put(groupName, cciArray);
				}
			}
		}
	}

	/**
	 * Calculate all.
	 *
	 * @param group the group
	 * @param input the input
	 * @return the map
	 */
	private Map<String, String> calculateAll(String group, String input){
		String[] splitAnswers = input.split(COMMA_SEPARATOR);
		Map<String, String> answersToReturn = Collections.synchronizedMap(new HashMap<String, String>());
		for (int i = 0; i<splitAnswers.length; i++){
			answersToReturn.put(""+i, splitAnswers[i]);
		}
		return answersToReturn;
	}

	/**
	 * Update available consumers.
	 *
	 * @param consumers the consumers
	 * @throws ConsumerNotFoundException the consumer not found exception
	 */
	public void updateAvailableConsumers(Map<String, Boolean> consumers) throws ConsumerNotFoundException{
		Iterator<String> i = consumers.keySet().iterator();
		Boolean next;
		String key;
		Class<? extends ClickerConsumerInterface> consumer;
		while (i.hasNext()){
			key = i.next();
			next = consumers.get(key);
			if (next){ //Consumer is Enabled in the Visual
				if (!availableConsumers.containsKey(key)){//Consumer is not currently enabled
					consumer = disabledConsumers.get(key);
					if (consumer!=null){//Found it in the disabled consumers
						availableConsumers.put(key, consumer);
						disabledConsumers.remove(key);
					} else {
						throw new ConsumerNotFoundException("Unable to locate consumer: " + key);
					}
				}
			} else { //Consumer is disabled in the Visual
				if (!disabledConsumers.containsKey(key)){//Consumer is currently not in the disabled list
					consumer = availableConsumers.get(key);
					if (consumer!=null){//Found it in the available consumers
						disabledConsumers.put(key, consumer);
						availableConsumers.remove(key);
					} else {
						throw new ConsumerNotFoundException("Unable to locate consumer: " + key);
					}
				}
			}
		}
	}

	/**
	 * Gets the consumers.
	 *
	 * @return the consumers
	 */
	public ArrayList<String> getConsumers(){
		System.out.println("Inside hub.getConsumers()");
		Iterator<String> aConsumers = availableConsumers.keySet().iterator();
		//Iterator<String> dConsumers = disabledConsumers.keySet().iterator();

		ArrayList<String> consumers = new ArrayList<String>();
		while (aConsumers.hasNext()){
			consumers.add(aConsumers.next());
		}

		//		while (dConsumers.hasNext()){
		//			consumers.add(dConsumers.next());
		//		}
		//		
		System.out.println(consumers==null);
		return consumers;
	}
}

