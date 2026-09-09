-- Nullbar: eksisterende rader og feilede oppslag faller tilbake til å vise ident.
ALTER TABLE TEKSTBLOKK
    ADD endret_av_navn VARCHAR2(200) NULL;
