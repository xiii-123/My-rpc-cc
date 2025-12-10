@echo off
chcp 65001
echo Setting encoding to UTF-8

set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8

echo === Full RPC Test Environment ===

echo Step 1: Start Spring Boot Provider in background...
cd /d "D:\Work-Files\my-rpc-cc\yu-rpc\example-springboot-provider"
start "Spring Boot Provider" cmd /c "mvn spring-boot:run %JAVA_OPTS%"

echo Waiting 10 seconds for provider to start...
timeout /t 10 /nobreak

echo Step 2: Running RPC Consumer Test...
cd /d "D:\Work-Files\my-rpc-cc\yu-rpc\example-springboot-consumer"
mvn exec:java -Dexec.mainClass="com.yupi.examplespringbootconsumer.QuickTest" %JAVA_OPTS%

echo Test completed!
pause