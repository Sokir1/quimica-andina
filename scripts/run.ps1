$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path "$raizProyecto/lib/mysql-connector-j-8.4.0.jar")) {
    throw 'Agregue MySQL Connector/J 8.4.0 en lib. Consulte README.md.'
}
& "$PSScriptRoot/build.ps1"
& java -cp "$raizProyecto/build/classes;$raizProyecto/lib/mysql-connector-j-8.4.0.jar" cl.quimicaandina.asistencia.Principal
if ($LASTEXITCODE -ne 0) { throw 'Falló la ejecución.' }
