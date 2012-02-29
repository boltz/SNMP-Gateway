package com.esc.msu;

import java.io.IOException;
import java.util.Vector;

import org.snmp4j.PDU;
import org.snmp4j.tools.console.SnmpRequest;

//import coldfusion.eventgateway.Gateway;
import coldfusion.eventgateway.GatewayHelper;
import coldfusion.eventgateway.GatewayServices;
import coldfusion.eventgateway.Logger;

/**
 * ColdFusion gateway helper supporting SNMP get and getNext
 * operations
 */
public class SnmpGatewayHelper implements GatewayHelper {

	/**
	 * The gateway this class is helping
	 */
	private SnmpGateway gateway;
	
	/**
	 * The handle to the CF gateway service
	 */
	private GatewayServices gatewayServices;
	/**
	 * our instance of the Logger for log messages
	 */
	private Logger logger = null;
	private boolean isLogging = true;
	
	public SnmpGatewayHelper(SnmpGateway gateway) {
		
		this.gateway = gateway;
		
		this.gatewayServices = GatewayServices.getGatewayServices();
	    if (isLogging) {
		    this.logger = this.gatewayServices.getLogger(this.gateway.getGatewayID() + "-helper");
	        this.logger.info("Instantiating " + this.gateway.getGatewayID() + "-helper");
	    }
	}
	
	/**
	 * Perform the requested SNMP Request on behalf of the gateway
	 * 
	 * @param type the request type, either "GET" or "GETNEXT"
	 * @param cred the set of SNMP credentials for the target SNMP Agent
	 * @param vbs the set of MIB variable bindings (varbinds) to retrieve
	 * @return the SNMP Response corresponding to the provided Request
	 * 
	 * @throws IOException
	 */
	private SnmpGatewayResponse snmpRequest(String type,
										    SnmpGatewayCredentials cred, 
										    Vector<String> vbs) throws IOException {

		Vector<String> args = extractArgs(type, cred, vbs);
		
		SnmpRequest sr = new SnmpRequest(args.toArray(new String[args.size()]));
		
		long start = System.currentTimeMillis();
		PDU response = sr.send();
		long duration = System.currentTimeMillis() - start;
		
		SnmpGatewayResponse sgr = new SnmpGatewayResponse(type, start, duration,
			                   cred.getTargetAddress(),
			                   vbs, response);
		if (isLogging) {
		    this.logger.info("response is: \n" + sgr.getSynopsis());
		}
		return sgr;
	}
	
	/**
	 * invoke an SNMP Get-Request
	 * 
	 * @param cred the set of SNMP credentials for the target SNMP Agent
	 * @param vbs the set of MIB variable bindings (varbinds) to retrieve 
	 * @return the SNMP Response corresponding to the provided Request
	 * 
	 * @throws IOException
	 */
	public SnmpGatewayResponse get(SnmpGatewayCredentials cred,
											  Vector<String> vbs) throws IOException {

		return this.snmpRequest("GET", cred, vbs);
	}
	
	/**
	 * invoke an SNMP GetNext-Request
	 * 
	 * @param cred the set of SNMP credentials for the target SNMP Agent
	 * @param vbs the set of MIB variable bindings (varbinds) to retrieve 
	 * @return the SNMP Response corresponding to the provided Request
	 * 
	 * @throws IOException
	 */
	public SnmpGatewayResponse getNext(SnmpGatewayCredentials cred,
												Vector<String> vbs) throws IOException {
		
		return this.snmpRequest("GETNEXT", cred, vbs);
	}
	

	/**
	 * Extract the set of calling arguments and organize for use by the
	 * org.snmp4j.tools.console.SnmpRequest class
	 *  
	 * @param pdu the type of SNMP PDU (GET or GETNEXT)
	 * @param cred the set of SNMP credentials for the target SNMP Agent
	 * @param vbs the set of MIB variable bindings (varbinds) to retrieve
	 * @return an ArrayList<String> containing the set of calling arguments
	 *         to pass to SnmpRequest
	 */
	private Vector<String> extractArgs(String pdu, SnmpGatewayCredentials cred, 	
										   Vector<String> vbs) {
		Vector<String> args = new Vector<String>();
		
		// turn off log4j output
		args.add("-d");		
		args.add("OFF");
		// identify the Request PDU type (GET or GETNEXT)		
		args.add("-p");
		args.add(pdu);
		// identify the SNMP version (v1 or v2c)
		args.add("-v");
		args.add(cred.getSnmpVersionArg());
		// identify the community string
		args.add("-c");
		args.add(cred.getTargetCommunity());
		// identify the target SNMP Agent
		args.add(cred.getTargetAddress() + cred.getTargetPortArg());
		// specify the set of requested varbinds
		args.addAll(vbs);
		
		return(args);
	}

	/**
	 * convenience method to make a set of SNMP Credentials
	 * 
	 * @param target the IP Address of the target SNMP Agent
	 * @param community the community string to use in requests made on the
	 *        target SNMP Agent
	 * @return an instance of SnmpGatewayCredentials for use with the supplied
	 *        target SNMP Agent.
	 */
	public SnmpGatewayCredentials createCredentials(String target, String community) {
		return new SnmpGatewayCredentials(target, community);
	}
	
    public String getEventListenerAddress() {
    	return this.gateway.getEventListenerAddress();
    }
    public void setEventListenerAddress(String l) {
    	this.gateway.setEventListenerAddress(l);
    }
    public String getEventListenerPort() {
    	return this.gateway.getEventListenerPort();
    }
    public void setEventListenerPort(String p) {
    	this.gateway.setEventListenerPort(p);
    } 
	
}
