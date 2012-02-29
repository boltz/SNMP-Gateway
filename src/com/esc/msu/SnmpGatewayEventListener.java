/**
 * 
 */
package com.esc.msu;

import java.io.IOException;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.tools.console.SnmpRequest;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

/**
 * The SNMP Gateway listener for SNMP event notifications
 *
 */
public class SnmpGatewayEventListener extends SnmpRequest implements Runnable {

	private SnmpGateway sg;
    private AbstractTransportMapping transport;
    private ThreadPool threadPool;
    private Snmp snmp;
    private OctetString localEngineID = new OctetString(MPv3.createLocalEngineID());


    /**
     * constructor for the SNMP Gateway event listener
     * @param sg reference to the SNMP Gateway instance
     * @param arg0 the command line options for the underlying org.snmp4j.tools.console.SnmpReqeust class
     */
	public SnmpGatewayEventListener(SnmpGateway sg, String[] arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
		this.sg = sg;
	}

	/**
	 * override class for adding a USM user
	 * @param snmp
	 */
	private void addUsmUser(Snmp snmp) {
		  snmp.getUSM().addUser(getSecurityName(), new UsmUser(getSecurityName(),
		                                                       getAuthProtocol(),
		                                                       getAuthPassphrase(),
		                                                       getPrivProtocol(),
		                                                       getPrivPassphrase()));
	}
	
	/**
	 * override class for establishing the event listener
	 */
	public synchronized void listen() throws IOException {

		if (this.getAddress() instanceof TcpAddress) {
		    this.transport = new DefaultTcpTransportMapping((TcpAddress) this.getAddress());
		} else {
		    this.transport = new DefaultUdpTransportMapping((UdpAddress) this.getAddress());
		}
		this.threadPool = ThreadPool.create("DispatcherPool", this.getNumDispatcherThreads());
		MessageDispatcher mtDispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());

		// add message processing models
		mtDispatcher.addMessageProcessingModel(new MPv1());
		mtDispatcher.addMessageProcessingModel(new MPv2c());
		mtDispatcher.addMessageProcessingModel(new MPv3(this.localEngineID.getValue()));

		// add all security protocols
		SecurityProtocols.getInstance().addDefaultProtocols();
		SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

		this.snmp = new Snmp(mtDispatcher, transport);
		if (this.getVersion() == SnmpConstants.version3) {
		    USM usm = new USM(SecurityProtocols.getInstance(), localEngineID, 0);
		    SecurityModels.getInstance().addSecurityModel(usm);
		    if (this.getAuthoritativeEngineID() != null) {
		        snmp.setLocalEngine(this.getAuthoritativeEngineID().getValue(), 0, 0);
		    }
		    // Add the configured user to the USM
		    addUsmUser(snmp);
		} else {
		    CommunityTarget target = new CommunityTarget();
		    target.setCommunity(this.getCommunity());
		    this.setTarget(target);
		}

		snmp.addCommandResponder(this);

		transport.listen();
		//System.out.println("Listening on "+ this.getAddress());

		try {
		    this.wait();
		} catch (InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	}


	/**
	 * override class for procesing an incoming SNMP message
	 */
	public synchronized void processPdu(CommandResponderEvent e) {
		PDU command = e.getPDU();
		if (command != null) {
		    //e.setProcessed(true);
			if ((command.getType() == PDU.TRAP)   ||
				(command.getType() == PDU.V1TRAP) ||
				(command.getType() == PDU.INFORM)) {
		        //this.sg.inboundMessage(new SnmpGatewayEvent(e));
		        if (command.getType() == PDU.INFORM) {
		            // try to send INFORM response
		            try {
		                sendInformResponse(e);
		            } catch (MessageException mex) {
		                this.sg.logWarn("Failed to send response on INFORM PDU event (" +
		                    	        e + "): " + mex.getMessage());
		            }
		        }
		        this.sg.inboundMessage(new SnmpGatewayEvent(e));
			    e.setProcessed(true);
		    }
		}
	}

    /**
     * send a RESPONSE PDU to the source address of an INFORM notification.
     * 
     * This method was adapted from org.snmp4j.Snmp.sendInformResponse()
     * 
     * @param event
     *    the <code>CommandResponderEvent</code> with the INFORM request.
     * @throws
     *    MessageException if the response could not be created and sent.
     */
    private void sendInformResponse(CommandResponderEvent e) throws MessageException {
        PDU r = (PDU) e.getPDU().clone();
        r.setType(PDU.RESPONSE);
        r.setErrorStatus(PDU.noError);
        r.setErrorIndex(0);
        e.getMessageDispatcher().returnResponsePdu(e.getMessageProcessingModel(),
                                          		   e.getSecurityModel(),
                                        		   e.getSecurityName(),
                                        		   e.getSecurityLevel(),
                                        		   r,
                                        		   e.getMaxSizeResponsePDU(),
                                        		   e.getStateReference(),
                                        		   new StatusInformation());
    }

	/**
	 * start listening for SNMP event notifications
	 */
	public void run() {
		try {
			this.listen();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * stop listening for SNMP event notifications and reliquish system resources
	 */
	public void stop() {
		this.threadPool.interrupt();
		try {
			this.transport.close();
			this.snmp.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
