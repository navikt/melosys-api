-- Null = aktiv. Sletting skjuler raden i stedet for å fjerne den, slik at en admin
-- som sletter ved en feil ikke mister innholdet permanent.
ALTER TABLE TEKSTBLOKK
    ADD slettet_dato TIMESTAMP NULL;
