param(
    [Parameter(Position = 0)]
    [ValidateSet('build', 'test', 'demo', 'interactivo')]
    [string]$Accion = 'demo'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$proyecto = Split-Path -Parent $PSScriptRoot
$salidaMain = Join-Path $proyecto 'out/main'
$salidaTest = Join-Path $proyecto 'out/test'

function Get-JdkTool([string]$Nombre) {
    if ($env:JAVA_HOME) {
        $ruta = Join-Path $env:JAVA_HOME "bin/$Nombre.exe"
        if (-not (Test-Path -LiteralPath $ruta)) {
            throw "JAVA_HOME no contiene $Nombre.exe. Configure un JDK 21."
        }
        return $ruta
    }
    return (Get-Command $Nombre -ErrorAction Stop).Source
}

try {
    $compilador = Get-JdkTool 'javac'
    $ejecutor = Get-JdkTool 'java'
    $fuentes = @(Get-ChildItem -LiteralPath (Join-Path $proyecto 'src/main/java') -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName })
    New-Item -ItemType Directory -Force -Path $salidaMain | Out-Null
    & $compilador --release 21 -encoding UTF-8 -Xlint:all -Werror -d $salidaMain @fuentes
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion. Se requiere JDK 21 o superior.' }

    switch ($Accion) {
        'build' { Write-Host 'Compilacion Java 21 correcta.' }
        'test' {
            $pruebas = @(Get-ChildItem -LiteralPath (Join-Path $proyecto 'src/test/java') -Recurse -Filter '*.java' |
                ForEach-Object { $_.FullName })
            New-Item -ItemType Directory -Force -Path $salidaTest | Out-Null
            & $compilador --release 21 -encoding UTF-8 -Xlint:all -Werror -cp $salidaMain -d $salidaTest @pruebas
            if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion de pruebas.' }
            & $ejecutor -cp "$salidaMain$([IO.Path]::PathSeparator)$salidaTest" com.empresa.gestion.app.Pruebas
            if ($LASTEXITCODE -ne 0) { throw 'Las pruebas fallaron.' }
        }
        default {
            & $ejecutor -cp $salidaMain com.empresa.gestion.app.Main "--$Accion"
            if ($LASTEXITCODE -ne 0) { throw 'La aplicacion termino con error.' }
        }
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
