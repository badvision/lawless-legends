@echo off
setlocal

REM Set GraalVM home (adjust path as needed)
set GRAALVM_HOME=C:\Program Files\GraalVM\graalvm-gluon-22.1.0.1
set PATH=%GRAALVM_HOME%\bin;%PATH%

cd /d "%~dp0"

echo Building native executable for Windows x86_64...
call mvn clean package
call mvn gluonfx:build

if not exist "target\gluonfx\x86_64-windows" (
    echo Build failed - output directory not found
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════════════
echo Build complete!
echo Executable: target\gluonfx\x86_64-windows\outlaweditor.exe
echo ════════════════════════════════════════════════════════════
echo.
echo To create an installer, use Inno Setup or similar tool
echo to package target\gluonfx\x86_64-windows\outlaweditor.exe

endlocal
