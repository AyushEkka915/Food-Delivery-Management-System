# ============================================================================
# scripts/env.ps1
# Activate the project-local, portable JDK 21 and Maven 3.9.9 toolchain for
# the current PowerShell session only.
#
# Usage (recommended, keeps the environment in your current shell):
#   . .\scripts\env.ps1
#
# The script can also be invoked directly:
#   & .\scripts\env.ps1
#
# No execution policy is changed. The portable toolchain is expected at:
#   <repo>\.tools\jdk21
#   <repo>\.tools\maven
# ============================================================================

$repoRoot = Split-Path -Parent $PSScriptRoot

$javaHome = Join-Path -Path $repoRoot -ChildPath '.tools\jdk21'
$mavenHome = Join-Path -Path $repoRoot -ChildPath '.tools\maven'
$javaBin = Join-Path -Path $javaHome -ChildPath 'bin'
$mavenBin = Join-Path -Path $mavenHome -ChildPath 'bin'

$missingPaths = @()
if (-not (Test-Path -LiteralPath $javaHome -PathType Container)) {
    $missingPaths += $javaHome
}
if (-not (Test-Path -LiteralPath $mavenHome -PathType Container)) {
    $missingPaths += $mavenHome
}

if ($missingPaths.Count -gt 0) {
    Write-Host 'ERROR: The portable toolchain is incomplete.' -ForegroundColor Red
    foreach ($missingPath in $missingPaths) {
        Write-Host ('  Missing expected path: {0}' -f $missingPath) -ForegroundColor Red
    }
    Write-Host 'Run the project toolchain setup before activating the environment.' -ForegroundColor Red
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:MAVEN_HOME = $mavenHome
$env:Path = '{0};{1};{2}' -f $javaBin, $mavenBin, $env:Path

Write-Host ('JAVA_HOME  = {0}' -f $env:JAVA_HOME)
Write-Host ('MAVEN_HOME = {0}' -f $env:MAVEN_HOME)
Write-Host ''

# java -version writes its output to stderr, so capture it with 2>&1.
Write-Host 'java -version:'
$javaVersionOutput = & java -version 2>&1 | Out-String
Write-Host $javaVersionOutput.TrimEnd()

Write-Host ''
Write-Host 'mvn -v:'
$mavenVersionOutput = & mvn -v 2>&1 | Out-String
Write-Host $mavenVersionOutput.TrimEnd()
