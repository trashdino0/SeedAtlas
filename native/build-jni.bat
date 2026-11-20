@echo off
setlocal
cd /d "%~dp0"
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b 1

if not exist build mkdir build
if not exist build\obj mkdir build\obj

if not defined JAVA_HOME set JAVA_HOME=C:\Program Files\Java\jdk-21
if not exist "%JAVA_HOME%\include\jni.h" (
  echo error: JAVA_HOME does not point at a JDK: %JAVA_HOME%
  exit /b 1
)

cl /nologo /c /O2 /MT /D_WIN32 /D_USE_MATH_DEFINES /D_CRT_SECURE_NO_WARNINGS ^
  /I"cubiomes" ^
  /I"%JAVA_HOME%\include" ^
  /I"%JAVA_HOME%\include\win32" ^
  /Fo"build\obj\seedatlas_jni.obj" ^
  "src\seedatlas_jni.c"
if errorlevel 1 exit /b 1

link /nologo /DLL /OUT:build\seedatlas.dll ^
  build\obj\*.obj ^
  /LIBPATH:"%JAVA_HOME%\lib" jvm.lib
if errorlevel 1 exit /b 1

echo Built build\seedatlas.dll
