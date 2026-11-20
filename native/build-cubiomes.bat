@echo off
setlocal
cd /d "%~dp0"
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b 1

if not exist build mkdir build
if not exist build\obj mkdir build\obj

cl /nologo /c /O2 /MT /D_WIN32 /D_USE_MATH_DEFINES /D_CRT_SECURE_NO_WARNINGS /I"cubiomes" /Fo"build\obj\\" ^
  "cubiomes\noise.c" ^
  "cubiomes\biomes.c" ^
  "cubiomes\layers.c" ^
  "cubiomes\biomenoise.c" ^
  "cubiomes\generator.c" ^
  "cubiomes\finders.c" ^
  "cubiomes\util.c" ^
  "cubiomes\quadbase.c"

if errorlevel 1 exit /b 1

lib /nologo /OUT:build\cubiomes.lib build\obj\*.obj
if errorlevel 1 exit /b 1

echo Built build\cubiomes.lib
