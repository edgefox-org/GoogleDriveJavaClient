GoogleDriveJavaClient
=====================

Implementation of Google Drive Client in Java

It supports everything except:
 - syncing shared files (TBD)
 - multiple parents for file/directory (won't fix)
 - edit feature for google docs, google spreadsheets, google presentations (won't fix)

JDK: 1.7+
OS: MacOS X, Linux(actualy, tested under Ubuntu 13.04 only)

Assemble instructions: mvn assembly:assembly

How to run:
 - cd $PROJECT_DIR/target
 - java -jar GoogleDriveJavaClient-1.0-SNAPSHOT.jar
 - enter path to the directory you would like to sync
 - authorize the application via following provided link
  
Startup algorithm:
 - try to download all content(except shared resources) of your google drive if needed
 - try to upload/create local unsynced files/dirs
 - scheduling task that will sync local and remote storage once for 15 seconds

Notes: 
 - startup could take a while, depending on amount of data you have remotely/locally, so be patient
 - google docs, google spreadsheets, google presentations are exposed locally as pdf files, because of lack of functionality to update them via APIs

