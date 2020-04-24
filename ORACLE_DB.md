# Oppsett og kjøring av Oracle database i Docker

For å sette opp et docker-image av en Oracle 18 XE database, må du først laste ned binærfilene til Oracle
 og bygge imaget selv. Instruksjoner for dette finnes på 
 https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance. Velg samme Oracle-versjon som melosys
 bruker (Oracle Database 18c (18.4.0) Express Edition (XE)). Det er videre gitt at imaget har navn og tag som følgende:
  `oracle/database:18.4.0-xe`
 
Kjør opp databasen med følgende kommando:
 `docker run --name oracle -d -p 51521:1521 -p 55500:5500 -e ORACLE_PWD=MyPassword1 -e ORACLE_CHARACTERSET=AL32UTF8 oracle/database:18.4.0-xe`.
 Dette tar fort 10 miutter.. Du kan følge med på loggen for å se når databasen er klar til bruk med `docker logs <containerID> -f`.
 
Når databasen har startet opp kan du aksessere containeren med kommandoen `docker exec -it --user=oracle oracle bash`.
Etter du har logget det inn i containeren kan du videre logge inn i databasen med ` sqlplus sys@XEPDB1 as sysdba`. 
Du blir her promptet med passord som vi skrev når vi kjørte opp containeren, `MyPassword1`. 

Videre kan vi opprette en bruker som vi kan bruke til å utvikle mot, samt gi nødvendige tilganger:
`create user melosys identified by melosys`. Altså, brukernavn `melosys` og passord `melosys`. For å gi nødvendige tilganger:
`grant all privileges to melosys`.

Databasen er nå klar til bruk. For å koble opp med f.eks. intellij, kan følgende url brukes: `jdbc:oracle:thin:@//localhost:51521/XEPDB1` 

For å stoppe containeren: `docker stop <containerID>`. NB; sletter du containeren så må du bygge den på nytt igjen, som er ganske tidskrevende. 
For å slippe dette kan du finne igjen ID-en til docker-containeren med `docker ps -a`, og videre kjøre `docker start <containerID>`.