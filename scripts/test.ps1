$ErrorActionPreference = 'Stop'
& "$PSScriptRoot/build.ps1"
$raizProyecto = Split-Path $PSScriptRoot -Parent
Push-Location $raizProyecto
try {
    & javac -encoding UTF-8 -cp build/classes -d build/classes src/test/java/Pruebas.java
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de pruebas.' }
    & java -cp build/classes Pruebas
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas.' }
} finally { Pop-Location }
