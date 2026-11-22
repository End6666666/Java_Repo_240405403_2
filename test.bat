@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "JUNIT=lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar"
if exist bin rmdir /s /q bin
mkdir bin
if exist sources.txt del sources.txt
for /r src %%f in (*.java) do echo %%f>>sources.txt
javac -encoding UTF-8 -cp "%JUNIT%" -d bin @sources.txt
if errorlevel 1 goto :fail
del sources.txt
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp "bin;%JUNIT%" org.junit.runner.JUnitCore com.accountbook.test.EntryTest com.accountbook.test.QueryResultTest com.accountbook.test.RepositoryTest com.accountbook.test.ServiceTest
goto :end
:fail
echo.
echo [编译失败] 先修编译错误
:end
if exist sources.txt del sources.txt
echo.
pause
