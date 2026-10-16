$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
if(-not(Test-Path app/src/main/java/app/shazin/aharemen/ClientConfig.java)){throw 'Copy ClientConfig.java.example to app/src/main/java/app/shazin/aharemen/ClientConfig.java and configure it locally.'}
$jdk=$env:JAVA_HOME
if(-not $jdk -or -not(Test-Path "$jdk/bin/javac.exe")){
 $jdk='C:\Users\Shazin\AppData\Roaming\PrismLauncher\java\java-runtime-delta'
}
if(-not(Test-Path "$jdk/bin/javac.exe")){throw 'Set JAVA_HOME to a JDK 17 or newer.'}
$env:JAVA_HOME=$jdk
$env:PATH="$jdk\bin;"+$env:PATH
$bt=(Get-ChildItem tools\sdk-build-tools -Directory | Select-Object -First 1).FullName
$android=(Get-ChildItem tools\sdk-platform -Filter android.jar -Recurse | Select-Object -First 1).FullName
if(-not $bt -or -not $android){throw 'Android platform and build tools are missing.'}
function Run($exe,[string[]]$arguments){ & $exe @arguments; if($LASTEXITCODE -ne 0){throw "$exe failed ($LASTEXITCODE)"} }
New-Item -ItemType Directory -Force build\classes,build\dex,dist | Out-Null
Run "$bt\aapt2.exe" @('compile','--dir','app\src\main\res','-o','build\resources.zip')
Run "$bt\aapt2.exe" @('link','-o','build\base.apk','-I',$android,'--manifest','app\src\main\AndroidManifest.xml','--min-sdk-version','26','--target-sdk-version','36','build\resources.zip')
$sources=@(Get-ChildItem app\src\main\java -Filter *.java -Recurse | ForEach-Object FullName)
Run "$jdk\bin\javac.exe" (@('-encoding','UTF-8','--release','8','-classpath',$android,'-d','build\classes')+$sources)
Run "$jdk\bin\jar.exe" @('cf','build\classes.jar','-C','build\classes','.')
Run "$bt\d8.bat" @('--lib',$android,'--min-api','26','--output','build\dex','build\classes.jar')
Copy-Item build\base.apk build\unsigned.apk -Force
Push-Location build\dex
try {Run "$jdk\bin\jar.exe" @('uf','..\unsigned.apk','classes.dex')}finally{Pop-Location}
Run "$bt\zipalign.exe" @('-f','4','build\unsigned.apk','build\aligned.apk')
if(-not(Test-Path tools\development.keystore)){
 Run "$jdk\bin\keytool.exe" @('-genkeypair','-keystore','tools\development.keystore','-storepass','android','-keypass','android','-alias','mamen-dev','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Mamen Fresh Development')
}
Run "$bt\apksigner.bat" @('sign','--ks','tools\development.keystore','--ks-key-alias','mamen-dev','--ks-pass','pass:android','--out','dist\aharemen.apk','build\aligned.apk')
Run "$bt\apksigner.bat" @('verify','--verbose','dist\aharemen.apk')
Write-Host 'Built dist\aharemen.apk'
