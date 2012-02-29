<cfcomponent hint="I this is a sample CFC that handles SNMP Events">
	
	<cffunction name="onEvent" access="public" hint="I handle incomming events from an event gateway." output="false" returntype="void">
		<cfargument name="CFEvent" hint="I am the cfevent structure" required="true" type="struct" />
		<cfset var dump = 0 />
		
		<cfsavecontent variable="dump">
			<cfdump var="#arguments.CFEvent#" />
		</cfsavecontent>
		
		<cffile action="write" file="#expandPath("dump/dump#CreateUuid()#.html")#" output="#dump#" />
		
		<cflog text="Incomming Message Received" />
	</cffunction>

</cfcomponent>