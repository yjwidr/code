setlocal enabledelayedexpansion 
IF DEFINED JAVA_HOME (
  SET JAVA="%JAVA_HOME%\bin\java.exe"
) ELSE (
  FOR %%I IN (java.exe) DO set JAVA=%%~$PATH:I
)
IF NOT EXIST %JAVA% (
  ECHO Could not find any executable java binary. Please install java in your PATH or set JAVA_HOME 1>&2
  EXIT /B 1
)

set EXECUTABLE=%CD%\TaskEngineServices.exe
set SERVICE_ID=NetBrainTaskEngine
goto checkExe

if errorlevel 1 (
	echo Warning: Could not start JVM to detect version, defaulting to x64:
	goto:eof
)

:checkExe
if EXIST "%EXECUTABLE%" goto :okExe
echo TaskEngineServices.exe was not found...

:okExe
if "x%1x" == "xx" goto displayUsage
set SERVICE_CMD=%1
shift
if "x%1x" == "xx" goto checkServiceCmd
set SERVICE_ID=%1
:checkServiceCmd


if /i %SERVICE_CMD% == install goto doInstall
if /i %SERVICE_CMD% == remove goto doRemove
if /i %SERVICE_CMD% == start goto doStart
if /i %SERVICE_CMD% == stop goto doStop
if /i %SERVICE_CMD% == restart goto doRestart
if /i %SERVICE_CMD% == status goto doStatus
if /i %SERVICE_CMD% == test goto doTest
echo Unknown option "%SERVICE_CMD%"

:displayUsage
echo Usage: services.bat install^|remove^|start^|stop^|status^|restart^|test
goto:eof

:doInstall
echo Installing service      :  "%SERVICE_ID%"
echo Using JAVA_HOME:  "%JAVA_HOME%"
"%EXECUTABLE%" install 
if not errorlevel 1 goto installed
goto:eof
:installed
echo The service '%SERVICE_ID%' has been installed.
sc config "%SERVICE_ID%" displayname="NetBrain Task Engine Service"
goto:eof

:doRemove
"%EXECUTABLE%" status > s.txt
for /f "delims==" %%a IN (s.txt)  do set ip=%%a
if "%ip%"=="Started"  (
	echo The service with id 'NetBrainTaskEngine' is running, please stop it first.
	goto:eof
)
"%EXECUTABLE%" uninstall
if not errorlevel 1 goto removed
goto:eof
:removed
echo The service '%SERVICE_ID%' has been removed
goto:eof

:doStart
"%EXECUTABLE%" status > s.txt
for /f "delims==" %%a IN (s.txt)  do set ip=%%a
if "%ip%"=="Started"  (
	goto started
)
"%EXECUTABLE%" start
if not errorlevel 1 goto started
goto:eof
:started
echo The service '%SERVICE_ID%' has been started
goto:eof

:doStop
"%EXECUTABLE%" status > s.txt
for /f "delims==" %%a IN (s.txt)  do set ip=%%a
if "%ip%"=="Stopped"  (
	goto stopped 
)
"%EXECUTABLE%" stop
if not errorlevel 1 goto stopped
goto:eof
:stopped
echo The service '%SERVICE_ID%' has been stopped
goto:eof

:doStatus
"%EXECUTABLE%" status
goto:eof

:doRestart
"%EXECUTABLE%" status > s.txt
for /f "delims==" %%a IN (s.txt)  do set ip=%%a
"%EXECUTABLE%" stop
"%EXECUTABLE%" restart
if not errorlevel 1 goto restart
goto:eof
:restart
echo The service '%SERVICE_ID%' has been restart
goto:eof

:doTest
"%EXECUTABLE%" test
goto:eof

ENDLOCAL
