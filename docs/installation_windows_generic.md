#Generic Installation on Windows 

## Requirements
- Install GitBash (https://www.git-scm.com/download/win), jdk (Java Development Kit) and Apache Ant (http://ant.apache.org/bindownload.cgi)

##Install Apache Ant and JDK

Install in terminal:
Open Command Prompt (Run as Administrator)
- 'set JAVA_HOME="<Location to the JDK>"''
- 'set ANT_HOME= "<Location to the Apache Ant>"''
- 'set PATH=%PATH%;%JAVA_HOME%\bin;%ANT_HOME%\bin'


## Download
- 'Open a Terminal (Command Prompt/ GitBash/ Cygwin)'
- 'Clone this repo: https://github.com/fossasia/susi_server.git'
- `cd susi_server`

## Build
- `ant jar`

## Run
- `java -jar dist/susiserver.jar`

## Operate
- open `http://localhost:4000` in your browser

## Shut down
- 'Ctrl+C in the terminal'

