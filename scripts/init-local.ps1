param([string]$DatabaseUser='hkuer_mesh')
$ErrorActionPreference='Stop'
if($DatabaseUser -notmatch '^[a-zA-Z0-9_]{1,32}$'){throw 'DatabaseUser must contain only letters, digits and underscores (1-32 characters).'}
$workspace=Split-Path $PSScriptRoot -Parent
$runtime=Join-Path $workspace '.local/mesh'
if(Test-Path -LiteralPath $runtime){throw 'Local runtime directory already exists. Review it manually; this script never overwrites credentials.'}
function New-RandomHex([int]$Bytes){
    $buffer=New-Object byte[] $Bytes
    $generator=[Security.Cryptography.RandomNumberGenerator]::Create()
    try {$generator.GetBytes($buffer)} finally {$generator.Dispose()}
    return ([BitConverter]::ToString($buffer)).Replace('-','').ToLowerInvariant()
}
$databasePassword=New-RandomHex 24
$adminPassword=New-RandomHex 16
$jwtSecret=New-RandomHex 48
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$utf8=New-Object Text.UTF8Encoding($false)
$template=[IO.File]::ReadAllText((Join-Path $workspace 'config/application-local.properties.example'))
$template=$template.Replace('spring.datasource.username=hkuer_mesh',"spring.datasource.username=$DatabaseUser").Replace('REPLACE_WITH_DATABASE_PASSWORD',$databasePassword).Replace('REPLACE_WITH_AT_LEAST_64_RANDOM_CHARACTERS',$jwtSecret).Replace('REPLACE_WITH_RANDOM_ADMIN_PASSWORD',$adminPassword)
[IO.File]::WriteAllText((Join-Path $runtime 'application-local.properties'),$template,$utf8)
$secrets=@{adminPassword=$adminPassword;databasePassword=$databasePassword;databaseUser=$DatabaseUser;jwtSecret=$jwtSecret}
[IO.File]::WriteAllText((Join-Path $runtime 'secrets.json'),($secrets|ConvertTo-Json),$utf8)
# No connection is made and no existing user/password is altered here.
# CREATE USER fails if this dedicated account already exists, so inspect that case manually.
$sql=@"
CREATE DATABASE IF NOT EXISTS hkuer_mesh_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hkuer_mesh_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER '$DatabaseUser'@'localhost' IDENTIFIED BY '$databasePassword';
GRANT ALL PRIVILEGES ON hkuer_mesh_dev.* TO '$DatabaseUser'@'localhost';
GRANT ALL PRIVILEGES ON hkuer_mesh_test.* TO '$DatabaseUser'@'localhost';
"@
[IO.File]::WriteAllText((Join-Path $runtime 'bootstrap.sql'),$sql,$utf8)
Write-Output 'Created ignored local credentials and database bootstrap SQL. No databases have been modified.'
Write-Output "Import $runtime/bootstrap.sql with your MySQL administrator account, then configure Redis if necessary."
Write-Output 'The generated admin password is in .local/mesh/secrets.json. Do not commit or share this directory.'
