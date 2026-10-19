$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$jdk=$env:JAVA_HOME
if(-not $jdk -or -not(Test-Path "$jdk/bin/javac.exe")){$jdk='C:\Users\Shazin\AppData\Roaming\PrismLauncher\java\java-runtime-delta'}
New-Item -ItemType Directory -Force build/tests | Out-Null
& "$jdk/bin/javac.exe" -encoding UTF-8 -d build/tests app/src/main/java/app/shazin/aharemen/Allowance.java tests/AllowanceTest.java app/src/main/java/app/shazin/aharemen/ContactMatch.java tests/ContactMatchTest.java app/src/main/java/app/shazin/aharemen/Redemption.java tests/RedemptionTest.java app/src/main/java/app/shazin/aharemen/SliderScale.java tests/SliderScaleTest.java app/src/main/java/app/shazin/aharemen/WalletMath.java tests/WalletMathTest.java
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& "$jdk/bin/java.exe" -cp build/tests app.shazin.aharemen.AllowanceTest
if($LASTEXITCODE -ne 0){throw 'Tests failed'}
& "$jdk/bin/java.exe" -cp build/tests app.shazin.aharemen.ContactMatchTest
if($LASTEXITCODE -ne 0){throw 'Tests failed'}
& "$jdk/bin/java.exe" -cp build/tests app.shazin.aharemen.RedemptionTest
if($LASTEXITCODE -ne 0){throw 'Tests failed'}
& "$jdk/bin/java.exe" -cp build/tests app.shazin.aharemen.SliderScaleTest
if($LASTEXITCODE -ne 0){throw 'Tests failed'}
& "$jdk/bin/java.exe" -cp build/tests app.shazin.aharemen.WalletMathTest
if($LASTEXITCODE -ne 0){throw 'Tests failed'}
