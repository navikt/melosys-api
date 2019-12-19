# MEDLEMSKAP OG LOVVALG

Prosjektets wiki-side: [https://confluence.adeo.no/display/TEESSI/Team+MELOSYS](https://confluence.adeo.no/display/TEESSI/Team+MELOSYS)
Jenkins: [http://eessi2-jenkins.adeo.no/](http://eessi2-jenkins.adeo.no/)

For lokal kjøring under utvikling, kan man definere environment-variabel
`MELOSYS_CONFIG_LOCATION` med sti til en valgfri mappe, og i den mappen opprette en fil ved navn `melosys-app.properties`, 
som inneholder alle properties man ønsker å sette/overstyre. Ved å kjøre `ApplicationLocal.java` fra IntelliJ,
vil alle disse verdiene leses inn og settes før applikasjonen startes.

Eksempel på fil: 
```
systemuser.password=<passord>
melosys.security.melosys_ad_group=0000-ga-Melosys
spring.datasource.password=<passord>
ldap.password=<passord>

javax.net.ssl.keyStore=C:/dev/nav_truststore_nonproduction_ny2.jts
javax.net.ssl.trustStore=C:/dev/nav_truststore_nonproduction_ny2.jts
keyStorePassword=<passord>
trustStorePassword=<passord>
com.sun.jndi.ldap.object.disableEndpointIdentification=true
```
