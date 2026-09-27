<#
.SYNOPSIS
  本地(Windows) 打包源码 -> 上传服务器 -> 触发服务器端自动构建与重启。
.EXAMPLE
  ./deploy/push-deploy.ps1 -Server root@1.2.3.4
.EXAMPLE
  ./deploy/push-deploy.ps1 -Server root@1.2.3.4 -SshKey C:\keys\id_rsa -SkipFrontend
#>
param(
  [Parameter(Mandatory = $true)][string]$Server,
  [string]$SshKey = '',
  [switch]$SkipBackend,
  [switch]$SkipFrontend,
  [string]$AdminApiBaseUrl = ''
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$remoteSrc = '/opt/electrical-repair-shop/src'
$remoteTar = '/tmp/app-src.tar.gz'
$localTar = Join-Path $env:TEMP 'app-src.tar.gz'

$sshOpts = @()
if ($SshKey) { $sshOpts = @('-i', $SshKey) }

Write-Host "==> 打包源码 (排除 node_modules/target/dist/.git): $repoRoot"
if (Test-Path $localTar) { Remove-Item $localTar -Force }
$excludes = @(
  'node_modules', 'target', 'dist', '.git', '.idea', '.vscode',
  'e2e/test-results', 'e2e/playwright-report', '*.log'
)
$exArgs = @()
foreach ($e in $excludes) { $exArgs += @('--exclude', $e) }
& tar -czf $localTar @exArgs -C $repoRoot .
if ($LASTEXITCODE -ne 0) { throw 'tar 打包失败' }

Write-Host "==> 上传到 $Server`:$remoteTar"
& scp @sshOpts $localTar "${Server}:${remoteTar}"
if ($LASTEXITCODE -ne 0) { throw 'scp 上传失败' }

$flags = @()
if ($SkipBackend) { $flags += '--skip-backend' }
if ($SkipFrontend) { $flags += '--skip-frontend' }
$flagStr = ($flags -join ' ')

$envPrefix = ''
if ($AdminApiBaseUrl) { $envPrefix = "ADMIN_API_BASE_URL='$AdminApiBaseUrl' " }
$remoteCmd = "${envPrefix}bash ${remoteSrc}/deploy/remote-deploy.sh ${remoteTar} ${flagStr}"

Write-Host "==> 触发服务器构建与部署"
& ssh @sshOpts $Server $remoteCmd
if ($LASTEXITCODE -ne 0) { throw '远程构建/部署失败' }

Write-Host "==> 完成。日志查看： ssh $Server 'journalctl -u electrical-backend -n 100 --no-pager'"
