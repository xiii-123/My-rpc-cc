@echo off
echo ==========================================
echo     yu-rpc ETCD配置中心JUnit测试
echo ==========================================
echo.

echo 编译测试代码...
cd %~dp0
mvn clean compile test-compile
if %ERRORLEVEL% neq 0 (
    echo ❌ 编译失败
    pause
    exit /b 1
)
echo ✅ 编译成功
echo.

echo 运行JUnit测试...
echo.
echo 可用的测试类:
echo 1. EtcdManagementControllerTest - 完整的API功能测试
echo 2. ManualEtcdTest - 手动交互式测试
echo.

echo 运行完整API功能测试...
mvn test -Dtest=EtcdManagementControllerTest
echo.

echo 如需运行手动测试，请使用:
echo mvn exec:java -Dexec.mainClass="com.yupi.yurpc.admin.ManualEtcdTest"
echo.
pause