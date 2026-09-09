-- Envers-historikk for tekstblokker. revinfo finnes fra V91, så her legges kun
-- _aud-tabellene til: én for entiteten og én per @ElementCollection.
-- Ingen revend-kolonner: DefaultAuditStrategy skriver dem aldri (kun ValidityAuditStrategy gjør det).
create table tekstblokk_aud
(
    id              number(19, 0) not null,
    rev             number(10, 0) not null,
    revtype         number(3, 0),
    registrert_av   varchar2(99 char),
    registrert_dato timestamp,
    endret_av       varchar2(99 char),
    endret_dato     timestamp,
    tittel          varchar2(200 char),
    innhold         clob,
    type            varchar2(20 char),
    endret_av_navn  varchar2(200 char),
    slettet_dato    timestamp,
    primary key (id, rev)
);

alter table tekstblokk_aud
    add constraint fk_tekstblokk_aud_rev
        foreign key (rev)
            references revinfo;

create table tekstblokk_tag_aud
(
    tekstblokk_id number(19, 0)     not null,
    tag           varchar2(60 char) not null,
    rev           number(10, 0)     not null,
    revtype       number(3, 0),
    primary key (tekstblokk_id, tag, rev)
);

alter table tekstblokk_tag_aud
    add constraint fk_tekstblokk_tag_aud_rev
        foreign key (rev)
            references revinfo;

create table tekstblokk_sakstype_aud
(
    tekstblokk_id number(19, 0)     not null,
    sakstype      varchar2(30 char) not null,
    rev           number(10, 0)     not null,
    revtype       number(3, 0),
    primary key (tekstblokk_id, sakstype, rev)
);

alter table tekstblokk_sakstype_aud
    add constraint fk_tekstblokk_sakstype_aud_rev
        foreign key (rev)
            references revinfo;

create table tekstblokk_behandlingstema_aud
(
    tekstblokk_id   number(19, 0)     not null,
    behandlingstema varchar2(60 char) not null,
    rev             number(10, 0)     not null,
    revtype         number(3, 0),
    primary key (tekstblokk_id, behandlingstema, rev)
);

alter table tekstblokk_behandlingstema_aud
    add constraint fk_tekstblokk_behtema_aud_rev
        foreign key (rev)
            references revinfo;
