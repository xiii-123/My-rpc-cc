@echo off
echo ==========================================
echo     yu-rpc ETCD配置中心测试脚本
echo ==========================================
echo.

echo 检查ETCD服务状态...
netstat -an | findstr :2379
if %ERRORLEVEL% neq 0 (
    echo ❌ ETCD服务未运行在端口2379
    echo 请先启动ETCD服务
    echo 示例: docker run -d --name etcd-server --publish 2379:2379 --publish 2380:2380 --env ALLOW_NONE_AUTHENTICATION=yes --env ETCD_ADVERTISE_CLIENT_URLS=http://0.0.0.0:2379 quay.io/coreos/etcd:v3.5.0
    pause
    exit /b 1
)
echo ✅ ETCD服务正在运行
echo.

echo 启动后端测试服务...
cd %~dp0
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8"
pause