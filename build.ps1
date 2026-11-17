param([ValidateSet('build','test','run','docs','integration')][string]$Task = 'build')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force build,lib | Out-Null
$driver = 'lib/postgresql-42.7.14.jar'
if (!(Test-Path $driver)) {
    Invoke-WebRequest 'https://jdbc.postgresql.org/download/postgresql-42.7.14.jar' -OutFile "$driver.download"
    Move-Item -LiteralPath "$driver.download" -Destination $driver
}
$sources = @(Get-ChildItem src/inventory/*.java | ForEach-Object FullName)
& javac -encoding UTF-8 -Xlint:all -d build -cp $driver @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
if ($Task -in 'test','integration') {
    $tests = @(Get-ChildItem tests/inventory/*.java | ForEach-Object FullName)
    & javac -encoding UTF-8 -d build -cp "build;$driver" @tests
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    & java -ea -cp "build;$driver" inventory.ItemTest
    if ($LASTEXITCODE -ne 0) { throw 'Validation tests failed' }
    & java -ea '-Djava.awt.headless=true' -cp "build;$driver" inventory.InventoryViewTest
    if ($LASTEXITCODE -ne 0) { throw 'Inventory view tests failed' }
    if ($Task -eq 'integration') {
        & java -ea -cp "build;$driver" inventory.DatabaseTest
        if ($LASTEXITCODE -ne 0) { throw 'Database tests failed' }
    }
}
if ($Task -eq 'run') { & java -cp "build;$driver" inventory.InventoryApp }
if ($Task -eq 'docs') {
    & javadoc -quiet -author -encoding UTF-8 -d docs/javadoc -sourcepath src -subpackages inventory
    if ($LASTEXITCODE -ne 0) { throw 'Javadoc failed' }
}
