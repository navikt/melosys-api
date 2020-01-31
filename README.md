# MEDLEMSKAP OG LOVVALG

Prosjektets wiki-side: [https://confluence.adeo.no/display/TEESSI/Team+MELOSYS](https://confluence.adeo.no/display/TEESSI/Team+MELOSYS)
Jenkins: [http://eessi2-jenkins.adeo.no/](http://eessi2-jenkins.adeo.no/)

## Lokal kjøring

For enklest mulig lokal kjøring under utvikling, kan man gjøre følgende:
* Overstyre environment-variable for lokal kjøring:
  * Opprette og definere en .env-fil, gjerne utenfor prosjekt-katalogen, f.eks.:
    ```
    spring.profiles.active=local
    
    systemuser.password=<passord>
    melosys.security.melosys_ad_group=0000-ga-Melosys
    spring.datasource.password=<passord>
    ldap.password=<passord>
    com.sun.jndi.ldap.object.disableEndpointIdentification=true
    ```
  * Installere IntelliJ-pluginen EnvFile
  * Gå til *Edit Configurations* for `Application.java` ->
        fane *EnvFile* -> *Enable EnvFile* og deretter legg til den definerte .env-filen

* Legge til NAVs keystore i JVM-en permanent, fordi det ikke virker som det er mulig å få til via .env-filen.
  * Last ned nyeste keystore-fil (https://fasit.adeo.no/api/v2/resources/8599733/file/keystore)
  * Flytt filen til *JAVA_HOME/lib/security*
  * Enten:
    * Døp om filen til `jssecacerts`. JVM-en bruker denne filen hvis den eksisterer, i stedet for `cacerts`
    * eller hvis du allerede har andre sertfikater du trenger i `cacerts`, kjør følgende kommando 
     for å merge keystore-filen inn i `cacerts`:
    `keytool -importkeystore -noprompt -srcstorepass <passord_for_nav_keystore> -srckeystore nav_truststore_nonproduction_ny2.jts -deststorepass changeit -destkeystore cacerts` 

## Integrasjonstester

For å kjøre integrasjonstester på Jenkins, må man angi en gyldig truststore ved kjøring.
Denne kan lastes ned fra et miljø på NAIS, f.eks. `t8` i `dev-fss` og konverteres til PKCS #12-format:

```
kubectl -n t8 get cm ca-bundle-jks -o jsonpath='{.binaryData.ca-bundle\.jks}' \
    | base64 -d > t8-ca-bundle.jks
keytool -importkeystore -srckeystore t8-ca-bundle.jks -destkeystore t8-ca-bundle.p12 \
    -srcstoretype JKS -deststoretype PKCS12 -keyalg RSA -deststorepass hunter2
```

(Du vil bli bedt om å oppgi passord for jks-fila, noe du kan finne i miljøvariabelen
`NAV_TRUSTSTORE_PASSWORD` i en pod som bruker NAV truststore.)

Den konverterte fila med tilhørende passord kan deretter legges inn i Jenkins som en credential av typen
`Certificate` med valget `Upload PKCS#12 certificate`.
