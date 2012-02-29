<!--- 
	Get a handle on the snmp event gateway helper.
	Note that the name of the gateway configured must be "snmp"
--->
<cfset snmpHelper = GetGatewayHelper("snmp") />

<!--- dump the snmp object --->
<cfdump var="#snmpHelper#" />

<!--- create the credentials --->
<cfset credentials = snmpHelper.createCredentials("192.168.1.1", "public") />

<!--- dump the credentials --->
<cfdump var="#credentials#" />

<!--- create the varbinds --->
<cfset varbinds = ArrayNew(1) />

<cfset ArrayAppend(varbinds, "1.3.6.1.2.1.1.3.0") />
<cfset ArrayAppend(varbinds, "1.3.6.1.2.1.1") />
<cfset ArrayAppend(varbinds, "1.3.6.1.2.1.1.5.0") />

<!--- dump the varbinds --->
<cfdump var="#varbinds#" />

<!--- do a get request --->
<cfset response = snmpHelper.get(credentials, varbinds) />

<!--- dump the response --->
<cfdump var="#response#" />

<!--- output details --->
<cfoutput>
	<p>
		duration: #response.getDuration()#<br />
		errorIndex: #response.getErrorIndex()#<br />
		errorStatus: #response.getErrorStatus()#<br />
		errorStatusText: #response.getErrorStatusText()#<br />
		requestType: #response.getRequestType()#<br />
		<cfdump var="#response.getResponseVarbinds()#" />
		start: #response.getStart()#<br />
		synopsis: #response.getSynopsis()#<br />
		target: #response.getTarget()#
	</p>
</cfoutput>