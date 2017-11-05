
Background
==========

This tool can be used with Apache ActiveMQ to dump and send JMS XML TextMessages for performance tests or to execute functional tests of the message processing.

This is not a very sophisticated implementation, i just published the tool because a former coworker requested a copy of it :-)

Features:

- read messages from a messagequeue and store them in the order of the queue to files 
  - read messages by queue-browsing (and do not remove them from the queue) or by fetching the from the queue
  - store messages by counts of 1000 (possibility to add additional messages)
- upload message files to a queue
  - read files and send them in sorted order from a directory
  - read files and send them in random order from a directory
  - read gzip compressed files
  - get queuenames from the filename
  

Example Directory:
```
<Counter > < Queuename         >
0000001000_FOO.BAR_BAZ_Barfoo.IN.xml
0000002000_FOO.BAR_BAZ_Barfoo.IN.xml
0000003000_FOO.BAR_BAZ_Barfoo.IN.xml
0000003100_FOO.BAR_BAZ_BarTTT.IN.xml
0000004200_FOO.BAR_BAZ_Barfoo.IN.xml
0000005000_FOO.BAR_BAZ_Barfoo.IN.xml.gz
0000005000_FOO.BAR_BAZ_Barfoo.IN.xml.gz
```

Help output:
```
$ java -jar target/messageutil-1.0-SNAPSHOT-jar-with-dependencies.jar

Usage: java -jar messageutil-1.0-SNAPSHOT-jar-with-dependencies.jar <options> <dir>

General options:
-u                        Broker url


Dumping options:
-d <queue>                Dump Messages from Queue
-b <inscrement>           Start increment counter at this number
-i <inscrement>           Increment by this number
-m                        Remove 'MANUAL' string in queuename while saving files
-c                        Consume messages instead of browsing  the messages


Replay options:
-r                        Replay messages
-q <queue>                Use a fixed queue for replay
                          (ignore queuename included in the filenames)
-p                        File pattern
                          Default: '\d{10}_(?<queuename>.+)\.xml'
-s                        Shuffle messages on sending
-t [a|e]#<name>#<time mofification> Modify all time information of the specified xml (e)lement or (a)ttribute before sending, separate them by ';'
                          (s=seconds,m=minutes,h=hours,d=days)
                          Example: 'e#ns0:LastModifiedDateTime#-2d;-2m' 't15m;+5s
```

How to extend and to use
=========================


  * Build project
    ```
    mvn package
    ```
  * Show help
    ```
    java -jar target/jms-message-util-jar-with-dependencies.jar -h
    ```
  * Dump queue "TEST" and store files to /tmp
    ```
    java -jar target/jms-message-util-jar-with-dependencies.jar -d TEST /tmp/
    java -jar target/jms-message-util-jar-with-dependencies.jar -u "tcp://127.0.0.1:61616?jms.useCompression=true" \
       -d FOO.BAR_BAZ_Barfoo-MANUAL.IN ~/temp/test-jms-dump
    ```
  * Send messages stored in /tmp  and send them in random order
    ```
    java -jar target/jms-message-util-jar-with-dependencies.jar -s  -r src/test/resources/xml/good/
    ```
  * Send Messages from /tmp to the specified queues and change the Timestamps in the XML files
    ```
    java -jar target/jms-message-util-jar-with-dependencies.jar -s  -r \
      -t "attribute#LastModifiedDateTime#-2d;+2m" \
      -t "element#ns0:CreatedDateTime#-2d;+2m" \
      -t "element#ns0:ModifiedDateTime#-2d;+2m" src/test/resources/xml/good/
    ```
  * Send messages of the directory /tmp to queue FOOQUEUE and ignore queuenames contained in the filesnames
    ```
    java -jar target/jms-message-util-jar-with-dependencies.jar -s -q FOOQUEUE -r -t "+2d" src/test/resources/xml/good/
    ```
