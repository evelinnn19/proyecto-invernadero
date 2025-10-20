@echo off
rem === Configurar entorno Java ===
set "JAVA_HOME=C:\Program Files\Java\jdk-22"
set "PATH=%JAVA_HOME%\bin;%PATH%"

title  Sistemas Operativos Distribuidos - Invernadero
color 0A

echo ============================================
echo      Iniciando todos los proyectos...
echo ============================================
echo.

REM === CONTROLADOR CENTRAL (PRINCIPAL Y BACKUP) ===
echo  Iniciando Controlador Central (principal)...
cd ControladorCentral
if exist "target" (
    for %%f in (target\*.jar) do start "Controlador Central Principal" cmd /k "java -jar %%f"
) else (
    start "Controlador Central Principal" cmd /k "java -jar dist\ControladorCentral.jar"
)
cd ..

timeout /t 3 /nobreak >nul

echo  Iniciando Controlador Central (backup)...
cd ControladorCentral
if exist "target" (
    for %%f in (target\*.jar) do start "Controlador Central Backup" cmd /k "java -jar %%f backup"
) else (
    start "Controlador Central Backup" cmd /k "java -jar dist\ControladorCentral.jar backup"
)
cd ..

timeout /t 3 /nobreak >nul



REM === SERVIDOR DE EXCLUSION MUTUA (MAVEN) ===
echo  Iniciando Servidor de Exclusion Mutua...
cd ServidorExclusionMutua
if exist "target" (
    for %%f in (target\*.jar) do start "Servidor Exclusion Mutua" cmd /k "java -jar %%f"
) else (
    start "Servidor Exclusion Mutua" cmd /k "java -jar dist\ServidorExclusionMutua.jar"
)
cd ..
timeout /t 3 /nobreak >nul


REM === SISTEMA DE FERTIRRIGACION ===
echo  Iniciando Sistema de Fertirrigacion...
cd SistemaFertirrigacion
if exist "target" (
    for %%f in (target\*.jar) do start "Sistema Fertirrigacion" cmd /k "java -jar %%f"
) else (
    start "Sistema Fertirrigacion" cmd /k "java -jar dist\SistemaFertirrigacion.jar"
)
cd ..
timeout /t 3 /nobreak >nul


REM === SENSORES DE HUMEDAD (ANT) ===
echo  Iniciando Sensores de Humedad...
for /L %%i in (1,1,5) do (
    if exist "SensorHumedad%%i\dist\SensorHumedad%%i.jar" (
        start "Sensor Humedad %%i" cmd /k "cd SensorHumedad%%i && java -jar dist\SensorHumedad%%i.jar"
    )
)
timeout /t 2 /nobreak >nul


REM === SENSORES CLIMÁTICOS (MAVEN) ===
echo  Iniciando Sensores Climáticos...
cd SensorLluvia
if exist "target" (
    for %%f in (target\*.jar) do start "Sensor Lluvia" cmd /k "java -jar %%f"
) else (
    start "Sensor Lluvia" cmd /k "java -jar dist\SensorLluvia.jar"
)
cd ..

cd SensorRadiacion
if exist "target" (
    for %%f in (target\*.jar) do start "Sensor Radiacion" cmd /k "java -jar %%f"
) else (
    start "Sensor Radiacion" cmd /k "java -jar dist\SensorRadiacion.jar"
)
cd ..

cd SensorTemperatura
if exist "target" (
    for %%f in (target\*.jar) do start "Sensor Temperatura" cmd /k "java -jar %%f"
) else (
    start "Sensor Temperatura" cmd /k "java -jar dist\SensorTemperatura.jar"
)
cd ..
timeout /t 2 /nobreak >nul


REM === ELECTROVALVULAS ===
echo  Iniciando Electrovalvulas...
for /L %%i in (1,1,7) do (
    if exist "ElectroValvula%%i\dist\ElectroValvula%%i.jar" (
        start "Electrovalvula %%i" cmd /k "cd ElectroValvula%%i && java -jar dist\ElectroValvula%%i.jar"
    )
)
timeout /t 2 /nobreak >nul


echo.
echo ============================================
echo  Todos los componentes del invernadero fueron iniciados.
echo  (Cada ventana corresponde a un módulo independiente.)
echo ============================================
pause
