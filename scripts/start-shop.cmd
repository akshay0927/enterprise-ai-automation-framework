@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "OPEN=--open"
if /i "%~1"=="--no-browser" set "OPEN="
if not "%~1"=="" if /i not "%~1"=="--no-browser" goto usage
if not "%~2"=="" goto usage
set "JAVA=java.exe"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -XshowSettings:properties -version 2>&1 | findstr /R /C:"java.specification.version = 21$" >nul
if errorlevel 1 (
  echo A Java 21 JDK is required. Check JAVA_HOME or the java.exe on PATH. 1>&2
  exit /b 1
)
set "SOURCE=%~dp0..\src\main\java\shop\Shop.java"
if not exist "%SOURCE%" (
  echo Shop.java is missing. Run this launcher inside the feature branch checkout. 1>&2
  exit /b 1
)
"%JAVA%" "%SOURCE%" %OPEN%
exit /b %errorlevel%
:usage
echo Usage: start-shop.cmd [--no-browser] 1>&2
exit /b 2
