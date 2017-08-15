# Generic Installation on Windows 

## Requirements
- Install GitBash (https://www.git-scm.com/download/win), jdk (Java Development Kit) and Apache Ant (http://ant.apache.org/bindownload.cgi)

## Install Apache Ant and JDK

Install in terminal:

# To be done as system variables: (Run as Admin)
- set JAVA_HOME to Location to the JDK
- set ANT_HOME to Location to the Apache Ant
- set PATH=%PATH%;%JAVA_HOME%\bin;%ANT_HOME%\bin

	> set JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_101"
    > set ANT_HOME = "C:\apache-ant\bin"
    > set PATH="%PATH%;%JAVA_HOME%\bin;%ANT_HOME%\bin"



## Download
- 'Open a Terminal (Command Prompt/ GitBash/ Cygwin)'
- 'Clone this repo: https://github.com/fossasia/susi_server.git'
- `cd susi_server`

## Build and Run
	> cd susi_server
    
    	
    (if on running above command gives error, try to change the branch "master" and then to "development" by following commands) :
    > git checkout master
    > ant jar
    > java -jar dist/susiserver.jar 
	
	
    > git checkout development
    
    (And re-run) :
    > ant jar
    > java -jar dist/susiserver.jar

## Operate
- open `http://localhost:4000` in your browser

## Shut down
- 'Ctrl+C in the terminal'

