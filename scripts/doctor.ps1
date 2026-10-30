# ============================================================================
# scripts/doctor.ps1
# Repeatable, read-only environment health check for the Smart Food Delivery
# Management System.
#
# Usage (from the repository root):
#   .\scripts\doctor.ps1
#
# This script NEVER installs, modifies, or deletes anything. It only reads
# paths, runs version commands, probes ports, and inspects the current cwd.
# Exit codes: 0 when there are no failures, 1 when at least one check fails.
# Compatible with Windows PowerShell 5.1.
# ============================================================================

[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'

$repoRoot = Split-Path -Parent $PSScriptRoot
$checks = New-Object System.Collections.ArrayList

# ----------------------------------------------------------------------------
# Activate the project-local portable toolchain, if it is present, so the
# checks report what the build will actually use. This only sets
# process-scoped environment variables; it installs and modifies nothing.
# ----------------------------------------------------------------------------
$portableJdkPath = Join-Path -Path $repoRoot -ChildPath '.tools\jdk21'
$portableMavenPath = Join-Path -Path $repoRoot -ChildPath '.tools\maven'

if (Test-Path -LiteralPath (Join-Path -Path $portableJdkPath -ChildPath 'bin\java.exe') -PathType Leaf) {
    $env:JAVA_HOME = $portableJdkPath
    $env:Path = (Join-Path -Path $portableJdkPath -ChildPath 'bin') + ';' + $env:Path
}

if (Test-Path -LiteralPath (Join-Path -Path $portableMavenPath -ChildPath 'bin\mvn.cmd') -PathType Leaf) {
    $env:MAVEN_HOME = $portableMavenPath
    $env:Path = (Join-Path -Path $portableMavenPath -ChildPath 'bin') + ';' + $env:Path
}

function Add-Check {
    param(
        [string]$Name,
        [string]$Required,
        [string]$Found,
        [string]$Status,
        [string]$Hint
    )

    [void]$checks.Add([pscustomobject][ordered]@{
        Name     = $Name
        Required = $Required
        Found    = $Found
        Status   = $Status
        Hint     = $Hint
    })
}

function Invoke-External {
    param(
        [string]$FilePath,
        [string[]]$Arguments = @()
    )

    try {
        $output = & $FilePath @Arguments 2>&1 | Out-String
        return [pscustomobject]@{
            Output   = $output.Trim()
            ExitCode = $LASTEXITCODE
            Error    = $null
        }
    }
    catch {
        return [pscustomobject]@{
            Output   = ''
            ExitCode = -1
            Error    = $_.Exception.Message
        }
    }
}

function Test-TcpPort {
    param(
        [string]$ComputerName,
        [int]$Port,
        [int]$TimeoutMilliseconds
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($ComputerName, $Port, $null, $null)
        $waitHandle = $asyncResult.AsyncWaitHandle
        $connected = $waitHandle.WaitOne($TimeoutMilliseconds, $false)
        if (-not $connected) {
            return $false
        }

        $client.EndConnect($asyncResult)
        return $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

# ----------------------------------------------------------------------------
# 1. JDK 21
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$javaExecutable = Join-Path -Path $repoRoot -ChildPath '.tools\jdk21\bin\java.exe'
$javaCommand = Get-Command -Name 'java' -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
$bundledJavaExists = Test-Path -LiteralPath $javaExecutable -PathType Leaf
$javaToRun = $null
$javaSource = 'not found'

if ($bundledJavaExists) {
    $javaToRun = $javaExecutable
    $javaSource = 'portable toolchain'
}
elseif ($javaCommand) {
    $javaToRun = $javaCommand.Path
    $javaSource = 'PATH'
}

if (-not $javaToRun) {
    Add-Check -Name 'JDK 21' -Required 'Yes' -Found 'Java not found' -Status 'FAIL' `
        -Hint 'Run the portable toolchain setup and then dot-source .\scripts\env.ps1.'
}
else {
    $javaResult = Invoke-External -FilePath $javaToRun -Arguments @('-version')
    $javaVersion = $null
    if ($javaResult.ExitCode -eq 0 -and $javaResult.Output -match 'version\s+"([^"]+)"') {
        $javaVersion = $Matches[1]
    }

    if (-not $javaVersion) {
        if ($bundledJavaExists) {
            Add-Check -Name 'JDK 21' -Required 'Yes' -Found ('java.exe failed to run at {0}' -f $javaToRun) `
                -Status 'FAIL' -Hint 'Repair the portable JDK 21 installation.'
        }
        else {
            Add-Check -Name 'JDK 21' -Required 'Yes' -Found ('Could not read the version of {0}' -f $javaToRun) `
                -Status 'WARN' -Hint 'Run the portable toolchain setup and then dot-source .\scripts\env.ps1.'
        }
    }
    elseif (-not $bundledJavaExists) {
        Add-Check -Name 'JDK 21' -Required 'Yes' `
            -Found ('Portable JDK missing; found Java {0} on PATH' -f $javaVersion) `
            -Status 'WARN' -Hint 'Run the portable toolchain setup so .tools\jdk21 is available.'
    }
    elseif ($javaVersion -like '21.*' -or $javaVersion -eq '21') {
        Add-Check -Name 'JDK 21' -Required 'Yes' `
            -Found ('Java {0} ({1})' -f $javaVersion, $javaSource) `
            -Status 'PASS' -Hint '-'
    }
    else {
        Add-Check -Name 'JDK 21' -Required 'Yes' `
            -Found ('Java {0} ({1}); JDK 21 required' -f $javaVersion, $javaSource) `
            -Status 'WARN' -Hint 'Install/repair the portable JDK 21 toolchain.'
    }
}

# ----------------------------------------------------------------------------
# 2. Maven
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$mavenExecutable = Join-Path -Path $repoRoot -ChildPath '.tools\maven\bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $mavenExecutable -PathType Leaf)) {
    Add-Check -Name 'Maven 3.9.x' -Required 'Yes' `
        -Found ('Missing {0}' -f $mavenExecutable) `
        -Status 'FAIL' -Hint 'Run the portable toolchain setup so .tools\maven is available.'
}
else {
    $mavenResult = Invoke-External -FilePath $mavenExecutable -Arguments @('-v')
    if ($mavenResult.ExitCode -ne 0) {
        $mavenError = $mavenResult.Error
        if (-not $mavenError) {
            $mavenError = 'mvn -v returned a non-zero exit code'
        }
        Add-Check -Name 'Maven 3.9.x' -Required 'Yes' -Found $mavenError `
            -Status 'FAIL' -Hint 'Run .\scripts\env.ps1 first, then check the portable Maven installation.'
    }
    else {
        $mavenVersion = $null
        if ($mavenResult.Output -match 'Apache Maven\s+([0-9][0-9A-Za-z.\-]*)') {
            $mavenVersion = $Matches[1]
        }

        $mavenJavaVersion = $null
        if ($mavenResult.Output -match 'Java version:\s*([^,\r\n]+)') {
            $mavenJavaVersion = $Matches[1].Trim()
        }

        if (-not $mavenJavaVersion) {
            Add-Check -Name 'Maven 3.9.x' -Required 'Yes' `
                -Found 'mvn -v succeeded but its Java version could not be read' `
                -Status 'FAIL' -Hint 'Ensure JAVA_HOME points to .tools\jdk21 and JAVA_HOME is honoured.'
        }
        elseif (-not ($mavenJavaVersion -like '21.*' -or $mavenJavaVersion -eq '21')) {
            Add-Check -Name 'Maven 3.9.x' -Required 'Yes' `
                -Found ('Maven {0} is running on Java {1}, not Java 21' -f $mavenVersion, $mavenJavaVersion) `
                -Status 'FAIL' -Hint 'Dot-source .\scripts\env.ps1 so Maven uses the portable JDK 21.'
        }
        else {
            if (-not $mavenVersion) {
                $mavenVersion = 'version unknown'
            }
            Add-Check -Name 'Maven 3.9.x' -Required 'Yes' `
                -Found ('Maven {0}; Java {1}' -f $mavenVersion, $mavenJavaVersion) `
                -Status 'PASS' -Hint '-'
        }
    }
}

# ----------------------------------------------------------------------------
# 3. Node.js
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$nodeCommand = Get-Command -Name 'node' -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $nodeCommand) {
    Add-Check -Name 'Node.js 20+' -Required 'Yes' -Found 'node not found on PATH' `
        -Status 'FAIL' -Hint 'Install Node.js 20 LTS or newer and ensure node is on PATH.'
}
else {
    $nodeResult = Invoke-External -FilePath $nodeCommand.Path -Arguments @('-v')
    $nodeVersion = $null
    if ($nodeResult.ExitCode -eq 0 -and $nodeResult.Output -match '(v?[0-9]+\.[0-9]+\.[0-9]+)') {
        $nodeVersion = $Matches[1]
    }

    if (-not $nodeVersion) {
        Add-Check -Name 'Node.js 20+' -Required 'Yes' -Found 'node -v failed' `
            -Status 'FAIL' -Hint 'Check the Node.js installation and PATH.'
    }
    else {
        $nodeMajorText = $nodeVersion.TrimStart('v').Split('.')[0]
        $nodeMajor = [int]$nodeMajorText
        if ($nodeMajor -ge 20) {
            Add-Check -Name 'Node.js 20+' -Required 'Yes' -Found $nodeVersion `
                -Status 'PASS' -Hint '-'
        }
        else {
            Add-Check -Name 'Node.js 20+' -Required 'Yes' `
                -Found ('{0}; major version 20 or newer required' -f $nodeVersion) `
                -Status 'FAIL' -Hint 'Install Node.js 20 LTS or newer.'
        }
    }
}

# ----------------------------------------------------------------------------
# 4. npm
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$npmCommand = Get-Command -Name 'npm' -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $npmCommand) {
    Add-Check -Name 'npm' -Required 'Yes' -Found 'npm not found on PATH' `
        -Status 'FAIL' -Hint 'Install Node.js (which includes npm) and ensure npm is on PATH.'
}
else {
    $npmResult = Invoke-External -FilePath $npmCommand.Path -Arguments @('-v')
    if ($npmResult.ExitCode -eq 0 -and $npmResult.Output) {
        Add-Check -Name 'npm' -Required 'Yes' -Found $npmResult.Output -Status 'PASS' -Hint '-'
    }
    else {
        Add-Check -Name 'npm' -Required 'Yes' -Found 'npm -v failed' `
            -Status 'FAIL' -Hint 'Check the Node.js/npm installation and PATH.'
    }
}

# ----------------------------------------------------------------------------
# 5. Git
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$gitCommand = Get-Command -Name 'git' -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $gitCommand) {
    Add-Check -Name 'Git' -Required 'Yes' -Found 'git not found on PATH' `
        -Status 'FAIL' -Hint 'Install Git for Windows and ensure git is on PATH.'
}
else {
    $gitResult = Invoke-External -FilePath $gitCommand.Path -Arguments @('--version')
    if ($gitResult.ExitCode -ne 0 -or -not $gitResult.Output) {
        Add-Check -Name 'Git' -Required 'Yes' -Found 'git --version failed' `
            -Status 'FAIL' -Hint 'Check the Git installation and PATH.'
    }
    else {
        $gitInsideWorkTree = 'unknown'
        $insideResult = Invoke-External -FilePath $gitCommand.Path -Arguments @('rev-parse', '--is-inside-work-tree')
        if ($insideResult.ExitCode -eq 0) {
            if ($insideResult.Output.Trim() -eq 'true') {
                $gitInsideWorkTree = 'yes'
            }
            else {
                $gitInsideWorkTree = 'no'
            }
        }
        else {
            $gitInsideWorkTree = 'no'
        }

        $gitVersionLine = ($gitResult.Output -split "`r?`n")[0]
        Add-Check -Name 'Git' -Required 'Yes' `
            -Found ('{0}; cwd inside git work tree: {1}' -f $gitVersionLine, $gitInsideWorkTree) `
            -Status 'PASS' -Hint '-'
    }
}

# ----------------------------------------------------------------------------
# 6. MySQL port 3306
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
# TcpClient with a 1500 ms timeout is much faster than Test-NetConnection.
$mysqlReachable = Test-TcpPort -ComputerName '127.0.0.1' -Port 3306 -TimeoutMilliseconds 1500
if ($mysqlReachable) {
    Add-Check -Name 'MySQL port 3306' -Required 'Yes' `
        -Found '127.0.0.1:3306 reachable' -Status 'PASS' -Hint '-'
}
else {
    Add-Check -Name 'MySQL port 3306' -Required 'Yes' `
        -Found '127.0.0.1:3306 not reachable within 1500 ms' `
        -Status 'FAIL' -Hint 'Start MySQL 8 and ensure it listens on port 3306.'
}

# ----------------------------------------------------------------------------
# 7. MySQL client
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$mysqlClientCommand = Get-Command -Name 'mysql' -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $mysqlClientCommand) {
    if ($mysqlReachable) {
        $mysqlClientHint = 'MySQL server is reachable. Install the MySQL client or add mysql.exe to PATH for manual SQL.'
    }
    else {
        $mysqlClientHint = 'Install the MySQL client or add mysql.exe to PATH for manual SQL.'
    }
    Add-Check -Name 'MySQL client' -Required 'No' -Found 'mysql not found on PATH' `
        -Status 'WARN' -Hint $mysqlClientHint
}
else {
    $mysqlClientResult = Invoke-External -FilePath $mysqlClientCommand.Path -Arguments @('--version')
    if ($mysqlClientResult.ExitCode -eq 0 -and $mysqlClientResult.Output) {
        $mysqlClientVersion = $mysqlClientResult.Output.Trim()
        Add-Check -Name 'MySQL client' -Required 'No' -Found $mysqlClientVersion `
            -Status 'PASS' -Hint '-'
    }
    else {
        Add-Check -Name 'MySQL client' -Required 'No' -Found 'mysql --version failed' `
            -Status 'WARN' -Hint 'Check the MySQL client installation and PATH.'
    }
}

# ----------------------------------------------------------------------------
# 8. Ports 5173 and 8080
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$port5173InUse = Test-TcpPort -ComputerName '127.0.0.1' -Port 5173 -TimeoutMilliseconds 1500
$port8080InUse = Test-TcpPort -ComputerName '127.0.0.1' -Port 8080 -TimeoutMilliseconds 1500

if ($port5173InUse) {
    $port5173State = '5173 occupied'
}
else {
    $port5173State = '5173 free'
}
if ($port8080InUse) {
    $port8080State = '8080 occupied'
}
else {
    $port8080State = '8080 free'
}

if ($port5173InUse -or $port8080InUse) {
    Add-Check -Name 'Ports 5173 and 8080' -Required 'No' `
        -Found ('{0}; {1}' -f $port5173State, $port8080State) `
        -Status 'WARN' -Hint 'Stop the process listening on the occupied port or change the configured port.'
}
else {
    Add-Check -Name 'Ports 5173 and 8080' -Required 'No' `
        -Found ('{0}; {1}' -f $port5173State, $port8080State) `
        -Status 'PASS' -Hint '-'
}

# ----------------------------------------------------------------------------
# 9. .env file
# ----------------------------------------------------------------------------
$ErrorActionPreference = 'Continue'
$dotEnvPath = Join-Path -Path $repoRoot -ChildPath '.env'
if (Test-Path -LiteralPath $dotEnvPath -PathType Leaf) {
    Add-Check -Name '.env file' -Required 'No' -Found 'Present at repo root' `
        -Status 'PASS' -Hint '-'
}
else {
    Add-Check -Name '.env file' -Required 'No' -Found 'Missing at repo root' `
        -Status 'WARN' -Hint 'Copy .env.example to .env and fill in real values.'
}

# ----------------------------------------------------------------------------
# Report
# ----------------------------------------------------------------------------
Write-Host ''
$checks | Format-Table -Property Status, Name, Found -AutoSize -Wrap | Out-Host

$issueChecks = @($checks | Where-Object { $_.Status -ne 'PASS' -and $_.Hint -ne '-' })
if ($issueChecks.Count -gt 0) {
    Write-Host 'Hints:'
    foreach ($issue in $issueChecks) {
        Write-Host ('  [{0}] {1} - {2}' -f $issue.Status, $issue.Name, $issue.Hint)
    }
    Write-Host ''
}

$passed = @($checks | Where-Object { $_.Status -eq 'PASS' }).Count
$warnings = @($checks | Where-Object { $_.Status -eq 'WARN' }).Count
$failed = @($checks | Where-Object { $_.Status -eq 'FAIL' }).Count

Write-Host ('{0} passed, {1} warnings, {2} failed' -f $passed, $warnings, $failed)

if ($failed -gt 0) {
    exit 1
}
exit 0
