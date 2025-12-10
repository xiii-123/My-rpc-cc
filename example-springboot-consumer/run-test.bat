@echo off
chcp 65001
echo 设置编码为UTF-8

set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Duser.timezone=Asia/Shanghai

echo 开始运行QuickTest...
mvn exec:java -Dexec.mainClass="com.yupi.examplespringbootconsumer.QuickTest" %JAVA_OPTS%

pause