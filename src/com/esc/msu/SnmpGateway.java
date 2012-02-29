package com.esc.msu;

import coldfusion.eventgateway.CFEvent;
import coldfusion.eventgateway.Gateway;
import coldfusion.eventgateway.GatewayHelper;
import coldfusion.eventgateway.GatewayServices;
import coldfusion.eventgateway.Logger;
import coldfusion.server.ServiceRuntimeException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


/**
 * ColdFusion gateway providing support for
 * listening for SNMP event notifications and
 * passing them along to the specified set of
 * CFC listeners.
 */
public class SnmpGateway implements Gateway {

	/**
	 * String identifying the gateway type
	 */
	private final static String gatewayType = "SnmpGateway";
	/**
	 * ID provided by EventService
	 */
	private String gatewayID = "";
	/**
	 * Path to my configuration file
	 */
	private String config = null;
	/**
	 * The handle to the CF gateway service
	 */
	private GatewayServices gatewayServices = null;
	/**
	 * our instance of the Logger for log messages
	 */
	private Logger logger = null;
	/**
	 * parameter to toggle logging
	 */
	private boolean isLogging = true;
    /**
     * Listener CFC paths for our events
     */
    private String[] cfcListeners = null;
    /**
     * ColdFusion Gateway status
     */
    private int status = Gateway.STOPPED;
    /** 
     * CFC function to call when SNMP Gateway event listener
     * receives an SNMP event notification
     */
    private String eventFunction;
    /**
     * the IP address upon which the SNMP Gateway event listener
     * listens for SNMP event notifications.
     */
    private String eventListenerAddress;
    /**
     * the port number upon which the SNMP Gateway event listener
     * listens for SNMP event notifications.
     */
    private String eventListenerPort;
    /**
     * the SNMP Gateway event listener class instance.
     */
    private SnmpGatewayEventListener eventListener;
    /**
     * the Executor instance within which the SNMP Gateway event listener
     * runs.
     */
    private ExecutorService eventExec;

    /**
     * Constructor for the SnmpGateway service
     * 
     * @param gatewayID the CF identifier for this gateway
     * @param config the path and configuration file name (if any)
     */
	public SnmpGateway(String gatewayID, String config) {
	    this.gatewayID = gatewayID;
	    this.config    = config;

	    if (isLogging) {
	        this.gatewayServices = GatewayServices.getGatewayServices();
	    	this.logger = this.gatewayServices.getLogger();
	    }
	    
        if (this.config != null) {
        	this.loadConfig();  // requires logger
        }
        
        this.status = Gateway.STARTING;
	    if (isLogging) {
	        this.logger.info("Starting Gateway: " + this.gatewayID);
	    }
	}
	
	/**
     * Constructor for the SnmpGateway service - used for testing from main()
     * 
     * @param gatewayID the CF identifier for this gateway
     * @param config the path and configuration file name (if any)
     */
	public SnmpGateway(String gatewayID, String config, boolean isLogging) {
	    this.gatewayID = gatewayID;
	    this.config    = config;
	    this.isLogging = isLogging;

	    if (isLogging) {
	        this.gatewayServices = GatewayServices.getGatewayServices();
	    	this.logger = this.gatewayServices.getLogger();
	    }
	    
        if (this.config != null) {
        	this.loadConfig();
        }
        this.status = Gateway.STARTING;
	    if (isLogging) {
	        this.logger.info("Starting Gateway: " + this.gatewayID);
	    }
	}
    /**
     * Load the properties file to get our settings
     */
    private void loadConfig() throws ServiceRuntimeException {
		
    	if (isLogging) {
            logger.info(SnmpGateway.gatewayType + " (" + this.gatewayID + 
            		    ") Initializing gateway with configuration file: " + this.config);
    	}
        Properties properties = new Properties();

        try {
            FileInputStream propsFile = new FileInputStream(this.config);
            properties.load(propsFile);
            propsFile.close();
        } catch (IOException e) {
            String error = SnmpGateway.gatewayType + " (" + this.gatewayID + ") Unable to load configuration file: " + this.config;
            throw new ServiceRuntimeException(error, e);
        }

        // The SNMP Gateway Event Listener IP address
        this.eventListenerAddress = properties.getProperty("listener_address", "0.0.0.0");
        // The SNMP Gateway Event Listener port number
    	this.eventListenerPort = properties.getProperty("listener_port", "162");
        // Event functions
        this.eventFunction = properties.getProperty("event_function", "onEvent");
    }

	/**
	 *  get the IP address of the SNMP Gateway event listener
	 *  
	 * @return the IP address of the SNMP Gateway event listener
	 */
    public String getEventListenerAddress() {
    	return this.eventListenerAddress;
    }
    
    /**
     * set the IP address of the SNMP Gateway event listener
     * 
     * @param l  the IP address of the SNMP Gateway event listener
     */
    public void setEventListenerAddress(String l) {
    	this.eventListenerAddress = new String(l);
    }
    
	/**
	 *  get the port number of the SNMP Gateway event listener socket
	 *  
	 * @return the port number of the SNMP Gateway event listener socket
	 */
    public String getEventListenerPort() {
    	return this.eventListenerPort;
    }
    
    
	/**
	 *  set the port number of the SNMP Gateway event listener socket
	 *  
	 * @param p the port number of the SNMP Gateway event listener socket
	 */
    public void setEventListenerPort(String p) {
    	this.eventListenerPort = p;
    }
    
    /**
     * start the SNMP Gateway Event Listener
     * 
     * @return success - true
     * <br>    failure - false
     */
    private boolean startEventListener() {
    	boolean rc = true;
    	
    	if (this.eventListener == null) {
    	
    		Vector<String> args = new Vector<String>();
		
    		// turn off log4j output
    		args.add("-d");		
    		args.add("OFF");
    		// identify this as a eventListener		
    		args.add("-Ol");
    		// identify the target SNMP Agent
    		args.add(this.getEventListenerAddress() + "/" + this.getEventListenerPort());
		
    		this.eventListener = new SnmpGatewayEventListener(this, args.toArray(new String[args.size()]));
    		if (this.eventListener == null) {
    			rc = false;
    		} else {
    			this.eventExec = Executors.newSingleThreadExecutor();
    			this.eventExec.execute(this.eventListener);
    		}
    	}
		return rc;
    }
    
	/**
     * Return the id that uniquely defines the gateway
     *
     * @return the id that uniquely defines the gateway
     */
	public String getGatewayID() {
		return this.gatewayID;
	}
	
	/**
	 * get the helper for this gateway
	 * 
	 * @return the helper for this gateway
	 */
	public GatewayHelper getHelper() {
		return new SnmpGatewayHelper(this);
	}
	
	/**
     * Return the status of the gateway
     *
     * @return one of STARTING, RUNNING, STOPPING, STOPPED, FAILED.
     */
	public int getStatus() {
		return this.status;
	}
	
	/**
     * Return the status in text format of the gateway
     *
     * @return one of STARTING, RUNNING, STOPPING, STOPPED, FAILED.
     */
	public String getStatusText() {
		String s = "UNKNOWN";
		
		switch(this.status) {
		case Gateway.FAILED:
			s = "FAILED";
			break;
		case Gateway.RUNNING:
			s = "RUNNING";
			break;
		case Gateway.STARTING:
			s = "STARTING";
			break;
		case Gateway.STOPPED:
			s = "STOPPED";
			break;
		case Gateway.STOPPING:
			s = "STOPPING";
			break;
		}
		return new String(s);
	}
	
	/**
	 * gateway interface routine - not used for SnmpGateway
	 * 
	 * @param cfmsg the outgoing message
	 * @return error message - not supported
	 */

	public String outgoingMessage(CFEvent cfmsg) {
		// TODO Auto-generated method stub
		return "ERROR: outgoingMessage not supported";
	}
	
	/**
	 * convenience routine to log warnings from SnmpGateway classes
	 * 
	 * @param s Warning message
	 */
	public synchronized void logWarn(String s) {
		if (isLogging) {
			this.logger.warn(s);
		}
	}
	
	/**
	 * package up and send along an SNMP event notification to CFC listeners
	 * 
	 * @param e  the SNMP event notification 
	 */
	public void inboundMessage(SnmpGatewayEvent e) {
		
		if (this.eventFunction != null && this.eventFunction.trim().length() > 0) {
			
			/*
		    if (!this.isLogging) {
		    	System.out.println("Received event at: " + e.getTimeReceived() + " from " + e.getSender());
		    	System.out.println("\t sender version: " + e.getEventVersion());
		    	System.out.println("\t sender timestamp: " + e.getEventTimeStamp());
		    	System.out.println("\t community/securityname: " + e.getCommunityString());
		    	System.out.println("\t request ID: " + e.getRequestId());
		    	System.out.println("\t event type: " + e.getEventType());
		    	System.out.println("\t event varbinds: " + e.getEventVarbinds().toString());
		    }
		    */

		    // populate the CFEvent class
		    CFEvent event = new CFEvent(this.gatewayID);  // use our gatewayID
		    event.setCfcMethod(this.eventFunction);		  // set the function to call in the CFC

		    Hashtable<String, Object> eventData = new Hashtable<String, Object>(); // create the data map
		    eventData.put("CommunityString", e.getCommunityString());  	
		    eventData.put("EventTimeStamp",  e.getEventTimeStamp());
		    eventData.put("EventType",       e.getEventType()); 
		    eventData.put("EventVarbinds",   e.getEventVarbinds()); 
		    eventData.put("EventVersion",    e.getEventVersion());
		    eventData.put("RequestId",       e.getRequestId());
		    eventData.put("Sender",          e.getSender());
		    eventData.put("TimeReceived",    e.getTimeReceived());
		    
		    event.setData(eventData);    								// place the data map into the CFEvent class
		    event.setGatewayType(SnmpGateway.gatewayType);  			// set our gateway type
		    event.setOriginatorID("");									// set the event originator

		    for (String s: this.cfcListeners) {
		    	//TODO: should the event class be cloned for each recipient?
		    	event.setCfcPath(s);

		    	this.gatewayServices.addEvent(event);
		    }
		}

	}
	
    /**
     * Restart this Gateway
     * <P>
     * Generally this can be implemented as a call to stop() and then start(),
     * but you may be able to optimize this based on what kind
     * of service your gateway talks to.
     */
	public void restart() {
        this.stop();
        this.start();
	}
	
	/**
     * Set the CFClisteners list.
     * <P>
     * Takes a list of fully qualified CF component names (e.g. "my.components.HandleEvent")
     * which should each receive events when the gateway sees one.
     * This will reset the list each time it is called.
     * <P>
     * This is called by the Event Service manager on startup, and may be called
     * if the configuration of the Gateway is changed during operation.
     *
     * @param listeners a list of component names
     */
	public void setCFCListeners(String[] listeners) {
		this.cfcListeners = listeners;	
	}
	
    /**
     * Set the id that uniquely defines the gateway
     * 
     * @param id this gateways id string
     */
	public void setGatewayID(String id) {
		this.gatewayID = id;
	}
	
	/**
	 * start the SNMP Event Listener and transition
	 * the SNMP Gateway status to the RUNNING state
	 */
	public void start() {
        if (this.config != null) {
        	this.loadConfig();
        }
	    if (this.startEventListener()) {
	  		this.status = Gateway.RUNNING;
	        if (isLogging) {
	        	this.logger.info("SNMP event notification eventListener started on " +
	        			  	this.getEventListenerAddress() + "/" + this.getEventListenerPort());
	        }
	    } else {
	    	if (isLogging) {
	    		this.logger.info("SNMP event notification eventListener FAILED to start on " +
	        			  	this.getEventListenerAddress() + "/" + this.getEventListenerPort());
	    	}
	    }
	}
	
	/**
	 * stop the SNMP Event Listener and transition 
	 * the SNMP Gateway status to the STOPPED state
	 */
	public void stop() {
		this.eventListener.stop();
		this.eventExec.shutdownNow();
	
		this.status = Gateway.STOPPED;
		if (isLogging) {
		    this.logger.info("Stopping Gateway: " + this.gatewayID);
		}
	}

/*

	public static void main(String[] args) {
		
		SnmpGateway sg = new SnmpGateway("ColdFusion SnmpGateway", "./config/SnmpGateway.cfg", false);
		SnmpGatewayHelper sgh = (SnmpGatewayHelper )sg.getHelper();
		System.out.println("SnmpGateway status is " + sg.getStatusText());
		sg.start();
		
		SnmpGatewayCredentials cred = sgh.createCredentials("192.168.1.220",
				                                                 "public");
		cred.setTargetPort(1161);
		Vector<String> vbl = new Vector<String>();
		vbl.add("1.3.6.1.2.1.1.3.0");
		vbl.add("1.3.6.1.2.1.1");
		
		System.out.println("SnmpGateway status is " + sg.getStatusText());
		try {
			SnmpGatewayResponse sgr = sgh.get(cred, vbl);
			System.out.println(sgr.getSynopsis());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		vbl.clear();
		vbl.add("1.3.6.1.2.1.1.3");
		vbl.add("1.3.6.1.2.1.1.5");
		vbl.add("1.3.6.1.2.1.8");
		
		try {
			SnmpGatewayResponse sgr = sgh.getNext(cred, vbl);
			System.out.println(sgr.getSynopsis());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sg.stop();
		System.out.println("SnmpGateway status is " + sg.getStatusText());
	}
*/
	
}
