logback-access-gelf :  A GELF Appender for Logback Access
==========================================

Use this appender to log access logs to a Graylog2 server via GELF messages.

This extends the functionality for [logback-gelf](https://github.com/Moocar/logback-gelf) for sending access log to Graylog Server.

Using Logback Access
--------------------
Repository Configuration :


    <repositories>
         <repository>
             <snapshots>
                 <enabled>false</enabled>
             </snapshots>
             <id>bintray-biplabnayak-maven</id>
             <name>bintray</name>
             <url>http://dl.bintray.com/biplabnayak/maven</url>
         </repository>
      </repositories>
      
Dependency :

    <dependency>
    	<groupId>com.capgemini</groupId>
    	<artifactId>logback-access-gelf</artifactId>
    	<version>0.0.4</version>
    </dependency>

Configuring Logback-Access
---------------------

Add the following to your logback-access.xml configuration file.

    <configuration>
        <appender name="GELF" class="com.capgemini.logbackaccess.gelf.AccessLogGelfAppender">
        		<facility>sample-service-access</facility>
        		<graylog2ServerHost>localhost</graylog2ServerHost>
        		<graylog2ServerPort>8003</graylog2ServerPort>
        		<useMarker>true</useMarker>
        		<graylog2ServerVersion>0.9.6</graylog2ServerVersion>
        		<requestURI>true</requestURI>
        		<remoteHost>true</remoteHost>
        		<protocol>true</protocol>
        		<method>true</method>
        		<statusCode>true</statusCode>
        		<contentLength>true</contentLength>
        		<responseContent>true</responseContent>
        		<headers>Test-Header</headers>
        	</appender>
         <appender-ref ref="GELF" />
    </configuration>

Properties
----------

*   **facility**: The name of your service. Appears in facility column in graylog2-web-interface. Defaults to "GELF"
*   **graylog2ServerHost**: The hostname of the graylog2 server to send messages to. Defaults to "localhost"
*   **graylog2ServerPort**: The graylog2ServerPort of the graylog2 server to send messages to. Defaults to 12201
*   **staticAdditionalFields**: See static additional fields below. Defaults to empty
*   **contentLength**: Boolean : to send content length to graglog
*   **header**: String,: List all headers you want to log with comma(,) separated.


Static Additional Fields
-----------------

Use static additional fields when you want to add a static key value pair to every GELF message. Key is the additional
field key (and should thus begin with an underscore). The value is a static string.

E.g in the appender configuration:

        <appender name="GELF" class="GelfAppender">
            ...
            <staticAdditionalField>_node_name:www013</staticAdditionalField>
            ...
        </appender>
        ...


Publish to bintray
-----------------

Add below configuration to maven settings.xml.
 
 
     		<server>
     		    <id>bintray-biplabnayak-maven</id>
     		    <username>biplabnayak</username>
     		    <password>***secret api key***</password>
     		</server>
    
   
Command :


            mvn clean deploy
