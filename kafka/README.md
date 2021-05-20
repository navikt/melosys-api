# Kafka
Dokumentasjon Kafka-NAIS: https://doc.nais.io/addons/kafka/

## Opprettelse av topics

1. Lag en katalog med ønsket topic navn under [kafka-aiven](kafka-aiven)
2. Legg til filene ```dev-vars.yaml, prod-vars.yaml og topic.yaml``` for henholdsvis template variabler til dev, prod og selve topic definisjonen.
3. Sjekk GHA action kjører OK.
4. Sjekk at topics er ressursen er opprettet og klar i dev/prod-gcp klustrene ```kubectl  get topic```
