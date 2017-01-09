# repoCleaner

## Download the binary. 

```
powershell -Command "$proxy = [System.Net.WebRequest]::GetSystemWebProxy();$proxy.Credentials = [System.Net.CredentialCache]::DefaultCredentials;$wc = new-object system.net.WebClient;$wc.proxy = $proxy;$wc.DownloadFile('https://jitpack.io/com/github/baloise/repoCleaner/-SNAPSHOT/repoCleaner--SNAPSHOT.jar', '%userprofile%\repoCleaner.jar');"
```


## Run
```
java -jar "%userprofile%\repoCleaner.jar"
```