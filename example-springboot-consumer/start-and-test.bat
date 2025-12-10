@echo off
chcp 65001
echo Setting encoding to UTF-8

set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8

echo === Starting RPC Test Environment ===

echo Step 1: Start Service Provider in background...
cd /d "D:\Work-Files\my-rpc-cc\yu-rpc\example-provider"
start "RPC Provider" cmd /c "mvn exec:java -Dexec.mainClass=\"com.yupi.example.provider.ProviderExample\" %JAVA_OPTS%"

echo Waiting 5 seconds for provider to start...
timeout /t 5 /nobreak

echo Step 2: Running RPC Consumer Test...
cd /d "D:\Work-Files\my-rpc-cc\yu-rpc\example-springboot-consumer"
mvn exec:java -Dexec.mainClass="com.yupi.examplespringbootconsumer.QuickTest" %JAVA_OPTS%

echo Test completed!
pause