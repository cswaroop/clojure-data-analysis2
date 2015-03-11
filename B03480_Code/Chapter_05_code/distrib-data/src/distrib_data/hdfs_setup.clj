
;;;; Also, see the HDFS example in distrib-data.cascalog-setup.

(ns distrib-data.hdfs-setup)

;;;; This primarily involved following http://hadoop.apache.org/docs/r0.20.2/quickstart.html
;;;;
;;;; > brew install hadoop
;;;; > vim /usr/local/Cellar/hadoop/1.0.4/libexec/conf/core-site.xml
;;;; > cat /usr/local/Cellar/hadoop/1.0.4/libexec/conf/core-site.xml
;;;; <?xml version="1.0"?>
;;;; <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
;;;; 
;;;; <!-- Put site-specific property overrides in this file. -->
;;;; 
;;;; <configuration>
;;;;   <property>
;;;;     <name>fs.default.name</name>
;;;;     <value>hdfs://localhost:9000</value>
;;;;   </property>
;;;;   <property>
;;;;     <name>dfs.replication</name>
;;;;     <value>1</value>
;;;;   </property>
;;;; </configuration>
;;;; > vim /usr/local/Cellar/hadoop/1.0.4/libexec/conf/mapred-site.xml
;;;; > cat /usr/local/Cellar/hadoop/1.0.4/libexec/conf/mapred-site.xml
;;;; <?xml version="1.0"?>
;;;; <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
;;;; 
;;;; <!-- Put site-specific property overrides in this file. -->
;;;; 
;;;; <configuration>
;;;;   <property>
;;;;     <name>mapred.job.tracker</name>
;;;;     <value>localhost:9001</value>
;;;;   </property>
;;;; </configuration>
;;;; > vim /usr/local/Cellar/hadoop/1.0.4/libexec/conf/hdfs-site.xml
;;;; > cat /usr/local/Cellar/hadoop/1.0.4/libexec/conf/hdfs-site.xml
;;;; <?xml version="1.0"?>
;;;; <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
;;;; 
;;;; <!-- Put site-specific property overrides in this file. -->
;;;; 
;;;; <configuration>
;;;;   <property>
;;;;     <name>dfs.replication</name>
;;;;     <value>1</value>
;;;;   </property>
;;;; </configuration>
;;;; > hadoop namenode -format
;;;; > start-all.sh
;;;; > hadoop fs -put 092011\ Stack\ Overflow 092011-stack-overflow
;;;; > hadoop fs -ls
;;;; > hadoop fs -ls 092011-stack-overflow


