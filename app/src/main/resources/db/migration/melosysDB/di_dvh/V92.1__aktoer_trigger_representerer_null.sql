--alter session set current_schema=<onsket_schema>;
create or replace trigger melosys_aktoer_trg
    after insert or update or delete on aktoer
    for each row
declare
    l_aktoer_dvh    aktor_dvh%rowtype;
    l_feillogg_dvh  feillogg_dvh%rowtype;
begin

    case
        when inserting then l_aktoer_dvh.dml_flagg := 'I';
        when updating  then l_aktoer_dvh.dml_flagg := 'U';
        when deleting  then l_aktoer_dvh.dml_flagg := 'D';
        end case;


    case
        when inserting or updating
            then

                l_aktoer_dvh.id                         := :new.id;
                l_aktoer_dvh.aktoer_id                  := :new.aktoer_id;
                l_aktoer_dvh.eu_eos_institusjon_id      := :new.eu_eos_institusjon_id;
                l_aktoer_dvh.rolle                      := :new.rolle;
                l_aktoer_dvh.orgnr                      := :new.orgnr;
                l_aktoer_dvh.saksnummer                 := :new.saksnummer;
                l_aktoer_dvh.utenlandsk_person_id       := :new.utenlandsk_person_id;
                l_aktoer_dvh.trygdemyndighet_land       := :new.trygdemyndighet_land;
                l_aktoer_dvh.registrert_tid             := :new.registrert_dato;
                l_aktoer_dvh.funksjonell_tid            := nvl(:new.endret_dato, :new.registrert_dato);
                l_aktoer_dvh.registrert_av              := :new.registrert_av;
                l_aktoer_dvh.funksjonell_av             := nvl(:new.endret_av, :new.registrert_av);

        when deleting
            then

                l_aktoer_dvh.id                     := :old.id;
                l_aktoer_dvh.aktoer_id              := :old.aktoer_id;
                l_aktoer_dvh.eu_eos_institusjon_id  := :old.eu_eos_institusjon_id;
                l_aktoer_dvh.rolle                  := :old.rolle;
                l_aktoer_dvh.orgnr                  := :old.orgnr;
                l_aktoer_dvh.saksnummer             := :old.saksnummer;
                l_aktoer_dvh.utenlandsk_person_id   := :old.utenlandsk_person_id;
                l_aktoer_dvh.trygdemyndighet_land   := :old.trygdemyndighet_land;
                l_aktoer_dvh.registrert_tid         := :old.registrert_dato;
                l_aktoer_dvh.funksjonell_tid        := nvl(:old.endret_dato, :old.registrert_dato);
                l_aktoer_dvh.registrert_av          := :old.registrert_av;
                l_aktoer_dvh.funksjonell_av         := nvl(:old.endret_av, :old.registrert_av);
        end case;


    insert into aktor_dvh
    (
        id,
        aktoer_id,
        eu_eos_institusjon_id,
        rolle,
        orgnr,
        saksnummer,
        utenlandsk_person_id,
        representerer,
        trygdemyndighet_land,
        registrert_tid,
        funksjonell_tid,
        registrert_av,
        funksjonell_av,
        dml_flagg
    )
    values
        (
            l_aktoer_dvh.id,
            l_aktoer_dvh.aktoer_id,
            l_aktoer_dvh.eu_eos_institusjon_id,
            l_aktoer_dvh.rolle,
            l_aktoer_dvh.orgnr,
            l_aktoer_dvh.saksnummer,
            l_aktoer_dvh.utenlandsk_person_id,
            null,
            l_aktoer_dvh.trygdemyndighet_land,
            l_aktoer_dvh.registrert_tid,
            l_aktoer_dvh.funksjonell_tid,
            l_aktoer_dvh.registrert_av,
            l_aktoer_dvh.funksjonell_av,
            l_aktoer_dvh.dml_flagg
        );

exception
    when others
        then

            l_feillogg_dvh.kilde_tabell := 'AKTOER';
            l_feillogg_dvh.kilde_pk     := l_aktoer_dvh.id;
            l_feillogg_dvh.sqlcode      := substr(sqlcode,1,20);
            l_feillogg_dvh.sqlerrm      := substr(sqlerrm,1,1000);

            insert into feillogg_dvh
            (
                kilde_tabell,
                kilde_pk,
                dml_flagg,
                sqlcode,
                sqlerrm
            )
            values
                (
                    l_feillogg_dvh.kilde_tabell,
                    l_feillogg_dvh.kilde_pk,
                    l_aktoer_dvh.dml_flagg,
                    l_feillogg_dvh.sqlcode,
                    l_feillogg_dvh.sqlerrm
                );

end;
/
