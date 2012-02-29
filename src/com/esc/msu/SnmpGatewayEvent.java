package com.esc.msu;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

/**
 * comprises an SNMP event notification received by the SNMP Gateway event listener
 *
 */
public class SnmpGatewayEvent {

	private final CommandResponderEvent event;
	private final Date timeReceived = new Date();
	private final PDU pdu;
	private final int pduType;
	public  final static OID sysUpTime   = new OID("1.3.6.1.2.1.1.3.0");
	public  final static OID snmpTrapOID = new OID("1.3.6.1.6.3.1.1.4.1.0");
	public  final static OID snmpGenericPrefix = new OID("1.3.6.1.6.3.1.1.5");
	public  final static int EGPNEIGHBORLOSS = 5;
	
	SnmpGatewayEvent(CommandResponderEvent e) {
		this.event = e;
		this.pdu = e.getPDU();
		if (this.pdu != null){
			this.pduType = this.pdu.getType();
		} else {
			this.pduType = 0;
		}
	}
	
	/**
	 * get the date and time this SNMP Event Notification was received
	 * @return the date and time this SNMP Event Notification was received
	 */
	public String getTimeReceived() {
		return this.timeReceived.toString();
	}
	
	/**
	 * get the IP address of the sender of this SNMP Event Notification
	 * @return the IP address of the sender of this SNMP Event Notification
	 */
	public String getSender() {
		return this.event.getPeerAddress().toString();
	}
	
	/**
	 * get the event notification varbindlist as a HashMap<String(OID), String<Value)>
	 * <p>
	 *       NOTE- the varbind ordering in the HashMap may NOT be consistent
	 *             with the ordering in the SNMP trap PDU
	 * <p>
	 * 		 NOTE- an SNMP v1 trap may not contain additional varbinds
	 * <p>
	 * @return a HashMap containing a representation of the event notification varbindlist
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, String> getEventVarbinds() {
		HashMap<String, String> vbl = new HashMap<String, String>();
		
		if (this.pdu != null) {	
			Vector<VariableBinding> vbs = this.pdu.getVariableBindings();
			for(VariableBinding vb : vbs) {
				vbl.put(vb.getOid().toString(), vb.getVariable().toString());
			}
		}
		return vbl;
	}
	
	/**
	 * get the version for this SNMP Event Notification
	 * @return  the version for this SNMP Event Notification
	 */
	public String getEventVersion() {
		String v = "";
		
		switch(this.event.getSecurityModel()) {
		case SecurityModel.SECURITY_MODEL_ANY:
			v = new String("Security model undefined");
			break;
		case SecurityModel.SECURITY_MODEL_SNMPv1:
			v = new String("SNMP v1 " + PDU.getTypeString(this.pduType));
			break;
		case SecurityModel.SECURITY_MODEL_SNMPv2c:
			v = new String("SNMP v2c " + PDU.getTypeString(this.pduType));
			break;
		case SecurityModel.SECURITY_MODEL_USM:
			v = new String("SNMP v3 " + PDU.getTypeString(this.pduType));
			break;
		}

		return v;
	}
	
	/**
	 * get the sysUptime for this SNMP Event Notification
	 * @return  the sysUptime for this SNMP Event Notification
	 */
	public String getEventTimeStamp() {
		String v = "PDU is missing";
		
		switch(this.pduType) {
		case PDU.NOTIFICATION:
		case PDU.INFORM:
			v = this.getNotificationTimeStamp();
			break;
		case PDU.V1TRAP:
			v = this.getTrapTimeStamp();
			break;
		}
		return v;

	}
	
	/**
	 * get the type for this SNMP Event Notification
	 * @return  the type for this SNMP Event Notification
	 */
	public String getEventType() {
		String v = "PDU is missing";
		
		switch(this.pduType) {
		case PDU.NOTIFICATION:
		case PDU.INFORM:
			v = this.getNotificationType();
			break;
		case PDU.V1TRAP:
			v = this.getTrapType();
			break;
		}
		
		return v;
	}

	/**
	 * get the TimeStamp for this v2c/v3 Trap
	 * @return the TimeStamp for this v2c/v3 Trap
	 */	
	@SuppressWarnings("unchecked")
	protected String getNotificationTimeStamp() {
		Vector<VariableBinding> vbs = (Vector<VariableBinding> )this.pdu.getVariableBindings();
		String ts = null;
		
		// the timestamp is *supposed* to be in the first varbind...
		for (VariableBinding vb: vbs) {
			if (vb.getOid().leftMostCompare(SnmpGatewayEvent.sysUpTime.size(), SnmpGatewayEvent.sysUpTime) == 0) {
				ts = new String(vb.getVariable().toString());
				break;
			}
		}
		
		if (ts == null) {
			ts = new String("TimeStamp is not present");
		}
		return ts;
	}

	/**
	 * get the event type for this v2c/v3 Trap
	 * @return the event type for this v2c/v3 Trap
	 */
	@SuppressWarnings("unchecked")
	protected String getNotificationType() {
		Vector<VariableBinding> vbs = (Vector<VariableBinding> )this.pdu.getVariableBindings();
		String t = null;
		
		// the trapOID is *supposed* to be in the second varbind...
		for (VariableBinding vb: vbs) {
			OID o = vb.getOid();
			if (o.startsWith(SnmpGatewayEvent.snmpTrapOID)  && (o.size() == SnmpGatewayEvent.snmpTrapOID.size())) {
				Variable v = vb.getVariable();
				if (v.getSyntax() == SMIConstants.SYNTAX_OBJECT_IDENTIFIER) {
					OID type = new OID(v.toString());
					if (type.startsWith(SnmpGatewayEvent.snmpGenericPrefix) && (type.size() == SnmpGatewayEvent.snmpGenericPrefix.size() + 1)) {
						int g = type.last();
				    	t = this.getGenericEventString(g);
					} else {
						if (type.get(type.size() - 2) == 0) {
					    	// enterprise specific
					    	int s = type.last();
					    	type.trim(2);  
					    	t = new String("Enterprise(" + type.toString() + ")  specific(" + s + ")");
						}
					}
					break;
				}
			}
		}
		
		if (t == null) {
			t = new String("Event type is not present");
		}
		return t;
	}
	
	/**
	 * get the text string associated with a generic event parameter
	 * @param g the generic event 
	 * @return the text string associated with a generic event parameter 
	 */
	protected String getGenericEventString(int g) {
		String t;
		
		switch(g) {
		case PDUv1.COLDSTART:
			t = new String("Cold Start");
			break;
		case PDUv1.WARMSTART:
			t = new String("Warm Start"); //probably never see this one
			break;
		case PDUv1.LINKDOWN:
			t = new String("Link Down");
			break;
		case PDUv1.LINKUP:
			t = new String("Link Up");
			break;
		case PDUv1.AUTHENTICATIONFAILURE:
			t = new String("Authentication Failure");
			break;
		case SnmpGatewayEvent.EGPNEIGHBORLOSS:
			t = new String("EGP Neighbor Loss"); //probably never see this one
			break;
		default:
			t = new String("Unknown"); 
			break;
		}
		return t;
	}
	
	/**
	 * get the TimeStamp for this V1 Trap
	 * @return the TimeStamp for this V1 Trap
	 */
	protected String getTrapTimeStamp() {
		PDUv1 pduV1 = (PDUv1 )this.pdu;
		
		return Long.toString(pduV1.getTimestamp());
	}
	
	/**
	 * get the event type for this V1 Trap
	 * @return the event type for this V1 Trap
	 */
	protected String getTrapType() {
		PDUv1 pduV1 = (PDUv1 )this.pdu;
		String t;
		
		OID e = pduV1.getEnterprise();
		int s = pduV1.getSpecificTrap();
		int g = pduV1.getGenericTrap();
		
		if (g == PDUv1.ENTERPRISE_SPECIFIC) {
			t = new String("Enterprise(" + e.toString() + ")  specific(" + s + ")");
		} else {
			t = this.getGenericEventString(g);
			
		}
		return t;
	}
	
	public String getCommunityString() {
		return this.getSecurityName();
	}
	
	public String getSecurityName() {
		String s = new String(this.event.getSecurityName());
		return s;
	}
	
	public String getRequestId() {
		return this.pdu.getRequestID().toString();
	}
}
