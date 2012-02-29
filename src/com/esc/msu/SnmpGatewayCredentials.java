package com.esc.msu;

/**
 * Provides the SNMP credentials for an SNMPGateway request.
 */
public class SnmpGatewayCredentials {

	/**
	 * constant values for SNMP versions
	 */
	public final static int SNMP_VERSION_1  = 0;
	public final static int SNMP_VERSION_2c = 1;
	
	/**
	 * The SNMP Version for the Gateway operation. 
	 * Default SNMP version is SNMPv2c.
	 */
	private int snmpVersion = SNMP_VERSION_2c;
	
	/**
	 * The port number of the target SNMP Agent.
	 * Default port number is 161. 
	 */
	private int    targetPort = 161;
	
	/**
	 * The IP address of the target SNMP Agent.
	 */
	private String targetAddress;
	
	/**
	 * the community string to use with an SNMP Gateway request
	 */
	private String targetCommunity;
	
	/**
	 * Constructor for a set of v1/v2c SNMP Credentials
	 * 
	 * @param target the IP Address of the target SNMP Agent
	 * @param community the community string to use in requests made on the
	 *        target SNMP Agent
	 */
	public SnmpGatewayCredentials(String target, String community) {
		this.targetAddress = new String(target);
		this.targetCommunity = new String(community);
	}
	
	/**
	 * Set the SNMP version
	 * @param version the SNMP version to set- either SNMP_VERSION_1
	 * or SNMP_VERSION_2c
	 */
	public void setSnmpVersion(int version) {
		if ((version == SnmpGatewayCredentials.SNMP_VERSION_1) ||
			(version == SnmpGatewayCredentials.SNMP_VERSION_2c)) {
			this.snmpVersion = version;
		}
	}
	
	/**
	 * Get the numeric identifier for the SNMP Version
	 * @return the numeric identifier for the SNMP Version
	 */
	public int getSnmpVersion() {
		return(this.snmpVersion);
	}
	
	/**
	 * Get the String identifier for the SNMP Version
	 * @return the string identifier for the SNMP Version
	 */
	public String getSnmpVersionArg() {
		String s;
		
		switch(this.snmpVersion) {
		case SnmpGatewayCredentials.SNMP_VERSION_1:
			s = new String("1");
			break;
		case SnmpGatewayCredentials.SNMP_VERSION_2c:
			s = new String("2c");
			break;
		default:
			s = null;
		}
		return s;
	}
	
	/**
	 * Set the port number of the target SNMP Agent. 
	 * @param port the port number of the target SNMP Agent.
	 */
	public void setTargetPort(int port) {
		this.targetPort = port;
	}
	
	/**
	 * Get the port number of the target SNMP Agent. 
	 * @return the port number of the target SNMP Agent.
	 */
	public int getTargetPort() {
		return(this.targetPort);
	}
	/**
	 * Get the port number of the target SNMP Agent. 
	 * @return the port number of the target SNMP Agent.
	 */
	public String getTargetPortArg() {
		return(new String("/" +Integer.toString(this.targetPort)));
	}
	/**
	 * Set the IP address of the target SNMP Agent.
	 * @param address the IP address of the target SNMP Agent.
	 */
	public void setTargetAddress(String address) {
		this.targetAddress = new String(address);
	}
	
	/**
	 * Get the IP address of the target SNMP Agent.
	 * 
	 * @return the IP address of the target SNMP Agent.
	 */
	public String getTargetAddress() {
		return (new String(this.targetAddress));
	}
	
	/**
	 * Set the community string of the target SNMP Agent.
	 * @param community the community string of the target SNMP Agent.
	 */
	public void setTargetCommunity(String community) {
		this.targetCommunity = new String(community);
	}
	
	
	/**
	 * Get the community string  for the target SNMP Agent.
	 * @return the community string  for the target SNMP Agent.
	 */
	public String getTargetCommunity() {
		return(new String(this.targetCommunity));
	}
}
