-- Utkast er ukvalitetssikret vedtakstekst og filtreres bort for ikke-administratorer.
-- Ingen CHECK: TekstblokkStatus-enumet eier semantikken, som for type (se ADR-0009).
-- Eksisterende blokker er publiserte.
ALTER TABLE TEKSTBLOKK
    ADD status VARCHAR2(20) DEFAULT 'PUBLISERT' NOT NULL;

-- Nullbar i audit-tabellen: Envers krever nullbare audit-kolonner, og revisjoner
-- fra før V167 har uansett ingen status.
ALTER TABLE TEKSTBLOKK_AUD
    ADD status VARCHAR2(20) NULL;
