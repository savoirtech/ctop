ctop
====

Camel Karaf Top Command

 ctop - display and update sorted information about Camel Context and Routes.

Description:

 ctop displays periodically Camel Context metrics, and routes sorted by throughput.

Building from source:
===

To build, invoke:
 
 mvn install

CTop installation:
===

CTop requires Apache Camel to be present in the container.

 feature:install camel


To install in Karaf, invoke from console:

 install -s mvn:com.savoirtech.karaf.commands/ctop


To execute command on Karaf, invoke:

 aetos:ctop


To exit ctop, press control + c

