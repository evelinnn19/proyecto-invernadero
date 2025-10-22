@echo off
rem === Configurar entorno Java ===
set "JAVA_HOME=C:\Program Files\Java\jdk-24"
set "PATH=%JAVA_HOME%\bin;%PATH%"

title Sistema Distribuido - Invernadero con Tolerancia a Fallos
color 0A

echo ============================================
echo    Sistema Distribuido con Anillo
echo    Puerto Clientes: 20000 (transparente)
echo ============================================
echo.

REM === SERVIDOR DE EXCLUSION MUTUA ===
echo [1/4] Iniciando Servidor de Exclusion Mutua...
cd ServidorExclusionMutua
if exist "target" (
    for %%f in (target\*.jar) do (
        start "Servidor Exclusion Mutua" cmd /k "color 0E && title Servidor Exclusion Mutua && java -jar %%f"
    )
) else (
    start "Servidor Exclusion Mutua" cmd /k "color 0E && title Servidor Exclusion Mutua && java -jar dist\ServidorExclusionMutua.jar"
)
cd ..
timeout /t 3 /nobreak >nul

REM === NODOS DEL ANILLO (compiten por puerto 20000) ===
echo [2/4] Iniciando Anillo de Nodos...
echo       Solo el lider abrira el puerto 20000
echo.
cd ControladorCentral

if exist "target" (
    REM Buscar el primer JAR y usarlo
    for %%f in (target\*.jar) do (
        start "Nodo 3 [ID MAYOR]" cmd /k "color 0B && title Nodo 3 && java -cp %%f ControladorCentral.Main 3 5003 5001"
        timeout /t 1 /nobreak >nul
        start "Nodo 2 [BACKUP]" cmd /k "color 0B && title Nodo 2 && java -cp %%f ControladorCentral.Main 2 5002 5003"
        timeout /t 1 /nobreak >nul
        start "Nodo 1 [BACKUP]" cmd /k "color 0B && title Nodo 1 && java -cp %%f ControladorCentral.Main 1 5001 5002"
        goto :nodos_iniciados
    )
) else (
    start "Nodo 3 [ID MAYOR]" cmd /k "color 0B && title Nodo 3 && java -cp dist\ControladorCentral.jar ControladorCentral.Main 3 5003 5001"
    timeout /t 1 /nobreak >nul
    start "Nodo 2 [BACKUP]" cmd /k "color 0B && title Nodo 2 && java -cp dist\ControladorCentral.jar ControladorCentral.Main 2 5002 5003"
    timeout /t 1 /nobreak >nul
    start "Nodo 1 [BACKUP]" cmd /k "color 0B && title Nodo 1 && java -cp dist\ControladorCentral.jar ControladorCentral.Main 1 5001 5002"
)
:nodos_iniciados
cd ..

echo [3/4] Esperando eleccion del lider...
timeout /t 5 /nobreak >nul

REM === CLIENTES (no saben del anillo) ===
echo [4/4] Iniciando Clientes...
cd SensorHumedad1
if exist "dist\SensorHumedad1.jar" (
    start "Sensor Humedad 1" cmd /k "color 0D && title Sensor Humedad 1 && java -jar dist\SensorHumedad1.jar"
)
cd ..
timeout /t 2 /nobreak >nul

REM Agrega aqui tus otros sensores/clientes
REM cd SensorTemperatura
REM start "Sensor Temperatura" cmd /k "java -jar dist\SensorTemperatura.jar"
REM cd ..

echo.
echo ============================================
echo    SISTEMA INICIADO
echo ============================================
echo.
echo Estado:
echo   - Anillo formado: 3 nodos (puertos 5001, 5002, 5003)
echo   - Lider actual: Nodo 3 (puerto clientes: 20000)
echo   - Clientes conectandose a: localhost:20000
echo.
echo PRUEBA DE TOLERANCIA A FALLOS:
echo   1. Los clientes estan en puerto 20000
echo   2. Cierra la ventana "Nodo 3" (lider actual)
echo   3. Nodo 2 detectara la falla (10 seg)
echo   4. Nodo 2 ganara eleccion y abrira puerto 20000
echo   5. Clientes se reconectaran automaticamente
echo.
echo Presiona cualquier tecla para detener todo...
pause >nul

REM === DETENER TODO ===
echo.
echo Deteniendo sistema...
taskkill /FI "WindowTitle eq Nodo*" /F >nul 2>&1
taskkill /FI "WindowTitle eq Servidor*" /F >nul 2>&1
taskkill /FI "WindowTitle eq Sensor*" /F >nul 2>&1
echo Sistema detenido.
timeout /t 2 /nobreak >nul