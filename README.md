# MEDLEMSKAP OG LOVVALG

Prosjektets wiki-side: [https://confluence.adeo.no/display/TEESSI/Team+MELOSYS](https://confluence.adeo.no/display/TEESSI/Team+MELOSYS)
Jenkins: [http://eessi2-jenkins.adeo.no/](http://eessi2-jenkins.adeo.no/)


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
