param([switch]$SkipBuild,[switch]$Test,[switch]$Restart)
$ErrorActionPreference='Stop'
$workspace=Split-Path $PSScriptRoot -Parent
$repo=$workspace
$runtime=Join-Path $workspace '.local/mesh'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
if (!$env:HKUER_LOCAL_CONFIG) { $env:HKUER_LOCAL_CONFIG = Join-Path $runtime 'application-local.properties' }
if(!(Test-Path -LiteralPath $env:HKUER_LOCAL_CONFIG)){throw 'Run scripts/init-local.ps1 and configure the databases first'}
$processRuntime=if($Test){Join-Path $workspace '.local/mesh-test'}else{$runtime}
if(Test-Path "$processRuntime/backend.pid"){
    $existingPid=[int](Get-Content "$processRuntime/backend.pid")
    $existing=Get-CimInstance Win32_Process -Filter "ProcessId=$existingPid" -ErrorAction SilentlyContinue
    if($existing -and $existing.Name -eq 'java.exe' -and ($existing.CommandLine -like '*application-mesh.yml*' -or $existing.CommandLine -like '*-Dhkuer.launcher=mesh*')){
        if(!$Restart){Write-Output "Mesh backend already running (PID $existingPid). Use -Restart to restart.";return}
        Stop-Process -Id $existingPid
        Wait-Process -Id $existingPid -ErrorAction SilentlyContinue
    }
}
if(!$SkipBuild){
    Push-Location $repo
    try { & $maven '-Dmaven.compiler.useIncrementalCompilation=false' -DskipTests package; if($LASTEXITCODE -ne 0){throw 'Build failed'} }
    finally {Pop-Location}
}
$arguments=@('-Dhkuer.launcher=mesh','-jar','target/Hkuer.jar')
$port=9091
if($Test){
    $arguments+='--mesh.mail-mode=local'
    $arguments+='--mesh.cloud-mode=local'
    $runtime=Join-Path $workspace '.local/mesh-test'
    New-Item -ItemType Directory -Path $runtime -Force | Out-Null
    $port=9092
    $arguments+=@('--server.port=9092','--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/hkuer_mesh_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai','--spring.data.redis.database=3','--mesh.redis-prefix=hkuer:mesh:test:',"--mesh.dev.root=$($runtime.Replace('\','/'))",'--mesh.worker-id=2','--jwt.cookie.access-name=mesh_test_access_token','--jwt.cookie.refresh-name=mesh_test_refresh_token')
}
$process=Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $repo -WindowStyle Hidden -RedirectStandardOutput "$runtime/backend.log" -RedirectStandardError "$runtime/backend-error.log" -PassThru
$process.Id | Set-Content "$runtime/backend.pid"
Write-Output "Mesh backend starting on 127.0.0.1:$port (PID $($process.Id))."
for($attempt=0;$attempt -lt 45;$attempt++){
    if($process.HasExited){throw "Backend exited. See $runtime/backend.log and backend-error.log"}
    try {
        $health=Invoke-RestMethod -Uri "http://127.0.0.1:$port/mesh/public/session" -TimeoutSec 2
        if($health.code -eq '200'){Write-Output "Mesh backend ready: http://127.0.0.1:$port";return}
    } catch { }
    Start-Sleep -Seconds 1
}
throw "Backend did not become ready. See $runtime/backend.log"
