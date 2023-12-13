create sequence hibernate_sequence;

create table revinfo
(
    rev      number(10, 0) not null,
    revtstmp number(19, 0),
    primary key (rev)
);

create table aktoer_aud
(
    id                    number(19, 0) not null,
    rev                   number(10, 0) not null,
    revtype               number(3, 0),
    revend                number(10, 0),
    revend_tstmp          timestamp,
    endret_av             varchar2(255 char),
    endret_dato           timestamp,
    registrert_av         varchar2(255 char),
    registrert_dato       timestamp,
    aktoer_id             varchar2(255 char),
    eu_eos_institusjon_id varchar2(255 char),
    orgnr                 varchar2(255 char),
    person_ident          varchar2(255 char),
    representerer         varchar2(255 char),
    rolle                 varchar2(255 char),
    trygdemyndighet_land  varchar2(255 char),
    utenlandsk_person_id  varchar2(255 char),
    saksnummer            varchar2(255 char),
    primary key (id, rev)
);

alter table aktoer_aud
    add constraint FK9o4s2gk35uxs15b66eau0nf12
        foreign key (rev)
            references revinfo;

alter table aktoer_aud
    add constraint FK9jvccby4h1r4x3gwhd7kxfmf2
        foreign key (revend)
            references revinfo;

create table fullmakt_aud
(
    id           number(19, 0) not null,
    rev          number(10, 0) not null,
    revtype      number(3, 0),
    revend       number(10, 0),
    revend_tstmp timestamp,
    type         varchar2(255 char),
    aktoer_id    number(19, 0),
    primary key (id, rev)
);

alter table fullmakt_aud
    add constraint FK5hmq7sdaex0bsje78pddijyq4
        foreign key (rev)
            references revinfo;

alter table fullmakt_aud
    add constraint FKgu2tfomt3g5yovbllcvu8cqwe
        foreign key (revend)
            references revinfo;
