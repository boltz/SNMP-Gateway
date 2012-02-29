
classdir=/home/ellison/workspace/SNMP4J/dist/lib
classpath="-classpath ${classdir}/SNMP4J.jar"
snmpRequest=org.snmp4j.tools.console.SnmpRequest
uptime="-Cu 42420"
community="-c public"
trapoid="-Ct 1.3.6.1.4069.5.6.7.8.0.9"
target="127.0.0.1/1162"

version_v2c="-v 2c"
pdutype_v2c="-p TRAP"
pdutype_v2c_inform="-p INFORM"

version_v1="-v 1"
pdutype_v1="-p V1TRAP"
specific="-Cs 9"
generic="-Cg 6"
enterprise="-Ce 1.3.6.1.4.1.4069"

options_v1="${version_v1} ${pdutype_v1} ${community} ${uptime} ${enterprise} ${generic} ${specific}"
options_v2c="${version_v2c} ${pdutype_v2c} ${community} ${uptime} ${trapoid}"
options_v2c_inform="${version_v2c} ${pdutype_v2c_inform} ${community} ${uptime} ${trapoid}"

java ${classpath} ${snmpRequest} ${options_v2c} ${target} "1.3.6.1.4.4.4.4.23={i}42" "1.3.6.1.4.5.6.7.8.0={s}Avast ye scurvy dogs"
java ${classpath} ${snmpRequest} ${options_v1} ${target} "1.3.6.1.4.1.4069.78.22.4.4.1={s}Gar! shiver me timbers"
java ${classpath} ${snmpRequest} ${options_v2c_inform} ${target} "1.3.6.1.4.4.4.4.23={i}42" "1.3.6.1.4.5.6.7.8.0={s}This is to INFORM you..."

