
@echo off
"%JAVA_HOME%\bin\java.exe" -Xmx4g -jar "treeview3-all-e33d6864.jar"

if ERRORLEVEL 1 "%JAVA_HOME%\bin\java.exe" -Xmx2g -jar "treeview3-all-e33d6864.jar"

if ERRORLEVEL 1 "%JAVA_HOME%\bin\java.exe" -Xmx1g -jar "treeview3-all-e33d6864.jar"

if ERRORLEVEL 1 java.exe -Xmx4g -jar "treeview3-all-e33d6864.jar"

if ERRORLEVEL 1 java.exe -Xmx2g -jar "treeview3-all-e33d6864.jar"

if ERRORLEVEL 1 java.exe -Xmx1g -jar "treeview3-all-e33d6864.jar"

if ERRORLEVEL 1 start "" /wait cmd /c "echo Error starting Java VM.  Please make sure you have at least java 1.7 installed.&echo(&pause"
