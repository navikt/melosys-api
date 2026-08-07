-- Sakstema som tredje avgrensningsdimensjon på tekstblokker, mellom sakstype og
-- behandlingstema. Samme semantikk som de to andre: tom mengde = gjelder alle.
--
-- Ingen egen indeks på sakstema-kolonnen, i motsetning til V165: spørringene slår opp på
-- tekstblokk_id, som PK-en dekker som ledende kolonne. En indeks på tre distinkte verdier
-- i en liten tabell ville kun kostet skriveoverhead.
CREATE TABLE TEKSTBLOKK_SAKSTEMA
(
    tekstblokk_id NUMBER(19)        NOT NULL,
    sakstema      VARCHAR2(60 CHAR) NOT NULL,
    CONSTRAINT pk_tekstblokk_sakstema PRIMARY KEY (tekstblokk_id, sakstema),
    CONSTRAINT fk_tekstblokk_sakstema_tekstblokk FOREIGN KEY (tekstblokk_id) REFERENCES TEKSTBLOKK (id) ON DELETE CASCADE
);

-- Envers-historikk, likt mønster som V166.
create table tekstblokk_sakstema_aud
(
    tekstblokk_id number(19, 0)     not null,
    sakstema      varchar2(60 char) not null,
    rev           number(10, 0)     not null,
    revtype       number(3, 0),
    primary key (tekstblokk_id, sakstema, rev)
);

alter table tekstblokk_sakstema_aud
    add constraint fk_tekstblokk_sakstema_aud_rev
        foreign key (rev)
            references revinfo;
