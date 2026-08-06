-- Sakstema som tredje avgrensningsdimensjon på tekstblokker, mellom sakstype og
-- behandlingstema. Samme semantikk som de to andre: tom mengde = gjelder alle.
CREATE TABLE TEKSTBLOKK_SAKSTEMA
(
    tekstblokk_id NUMBER(19)   NOT NULL,
    sakstema      VARCHAR2(60) NOT NULL,
    CONSTRAINT pk_tekstblokk_sakstema PRIMARY KEY (tekstblokk_id, sakstema),
    CONSTRAINT fk_tekstblokk_sakstema_tekstblokk FOREIGN KEY (tekstblokk_id) REFERENCES TEKSTBLOKK (id) ON DELETE CASCADE
);

CREATE INDEX idx_tekstblokk_sakstema_sakstema ON TEKSTBLOKK_SAKSTEMA (sakstema);

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
