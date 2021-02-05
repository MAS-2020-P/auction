@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\repo

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\agentCore-5.2.4.jar;"%REPO%"\slf4j-log4j12-1.7.10.jar;"%REPO%"\slf4j-api-1.7.10.jar;"%REPO%"\log4j-1.2.17.jar;"%REPO%"\gateway-5.2.4.jar;"%REPO%"\activemq-client-5.12.1.jar;"%REPO%"\geronimo-jms_1.1_spec-1.1.1.jar;"%REPO%"\hawtbuf-1.11.jar;"%REPO%"\geronimo-j2ee-management_1.1_spec-1.0.1.jar;"%REPO%"\activemq-broker-5.12.1.jar;"%REPO%"\activemq-openwire-legacy-5.12.1.jar;"%REPO%"\activemq-kahadb-store-5.12.1.jar;"%REPO%"\activemq-protobuf-1.1.jar;"%REPO%"\commons-net-3.3.jar;"%REPO%"\agentCoreAPI-5.2.4.jar;"%REPO%"\SimpleSpaceCore-2.6.4.jar;"%REPO%"\GetterSetterFinder-1.1.1.jar;"%REPO%"\FieldUtil-1.0.jar;"%REPO%"\spring-core-4.3.12.RELEASE.jar;"%REPO%"\commons-logging-1.2.jar;"%REPO%"\spring-context-4.3.12.RELEASE.jar;"%REPO%"\spring-aop-4.3.12.RELEASE.jar;"%REPO%"\spring-beans-4.3.12.RELEASE.jar;"%REPO%"\spring-expression-4.3.12.RELEASE.jar;"%REPO%"\webServer-5.2.1.jar;"%REPO%"\jetty-deploy-8.1.17.v20150415.jar;"%REPO%"\jetty-xml-8.1.17.v20150415.jar;"%REPO%"\jetty-webapp-8.1.17.v20150415.jar;"%REPO%"\jetty-servlet-8.1.17.v20150415.jar;"%REPO%"\jetty-security-8.1.17.v20150415.jar;"%REPO%"\jetty-server-8.1.17.v20150415.jar;"%REPO%"\jetty-continuation-8.1.17.v20150415.jar;"%REPO%"\jetty-jsp-8.1.17.v20150415.jar;"%REPO%"\javax.servlet.jsp-2.2.0.v201112011158.jar;"%REPO%"\javax.servlet-3.0.0.v201112011016.jar;"%REPO%"\org.apache.jasper.glassfish-2.2.2.v201112011158.jar;"%REPO%"\javax.servlet.jsp.jstl-1.2.0.v201105211821.jar;"%REPO%"\org.apache.taglibs.standard.glassfish-1.2.0.v201112081803.jar;"%REPO%"\javax.el-2.2.0.v201108011116.jar;"%REPO%"\com.sun.el-2.2.0.v201108011116.jar;"%REPO%"\org.eclipse.jdt.core-3.7.1.jar;"%REPO%"\jetty-websocket-8.1.17.v20150415.jar;"%REPO%"\jetty-util-8.1.17.v20150415.jar;"%REPO%"\jetty-io-8.1.17.v20150415.jar;"%REPO%"\jetty-http-8.1.17.v20150415.jar;"%REPO%"\jiac-aot-auction-1.1.0.jar
goto endInit

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS%  -classpath %CLASSPATH_PREFIX%;%CLASSPATH% -Dapp.name="Start_Observer" -Dapp.repo="%REPO%" -Dapp.home="%BASEDIR%" -Dbasedir="%BASEDIR%" de.dailab.jiactng.aot.auction.StartObserver %CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=%ERRORLEVEL%

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 @endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
