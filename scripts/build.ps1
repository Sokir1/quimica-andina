$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path $PSScriptRoot -Parent
Push-Location $raizProyecto
try {
    New-Item -ItemType Directory -Force build/classes | Out-Null
    $fuentes = Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
    [IO.File]::WriteAllLines((Join-Path $raizProyecto 'build/sources.txt'), $fuentes, [Text.UTF8Encoding]::new($false))
    & javac -encoding UTF-8 -d build/classes '@build/sources.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }
} finally { Pop-Location }
