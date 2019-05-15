--alter session set current_schema=<onsket_schema>;
create or replace trigger melosys_behandling_trg
after insert or update or delete on behandling
for each row
declare
  l_behandling_dvh  behandling_dvh%rowtype; 
  l_feillogg_dvh    feillogg_dvh%rowtype;
  l_resultat_dvh    behandlingsresultat%rowtype;
begin
    
    case 
    when inserting then l_behandling_dvh.dml_flagg := 'I';
    when updating  then l_behandling_dvh.dml_flagg := 'U';
    when deleting  then l_behandling_dvh.dml_flagg := 'D';
    end case;

    l_behandling_dvh.enhet       := '4530';
    l_behandling_dvh.kildetabell := 'BEHANDLING';

    
    case 
    when inserting or updating
    then 
        begin
            select r.*
            into l_resultat_dvh
            from behandlingsresultat r
            where behandling_id = :new.id;
        exception 
            when no_data_found
            then null;
        end;

        l_behandling_dvh.behandling_id                  := :new.id;
        l_behandling_dvh.fagsak_id                      := :new.saksnummer;
        l_behandling_dvh.status                         := :new.status;
        l_behandling_dvh.beh_type                       := :new.beh_type;
        l_behandling_dvh.resultat_type                  := l_resultat_dvh.resultat_type;
        l_behandling_dvh.registrert_tid                  := :new.registrert_dato;
        l_behandling_dvh.funksjonell_tid                := nvl(:new.endret_dato, :new.registrert_dato);
        l_behandling_dvh.registrert_av                  := :new.registrert_av;
        l_behandling_dvh.funksjonell_av                 := nvl(:new.endret_av, :new.registrert_av);
        l_behandling_dvh.behandlingsmaate               := l_resultat_dvh.behandlingsmaate;
        l_behandling_dvh.fastsatt_av_land               := l_resultat_dvh.fastsatt_av_land;
        l_behandling_dvh.henleggelse_grunn              := l_resultat_dvh.henleggelse_grunn;
        l_behandling_dvh.siste_opplysninger_hentet_dato := :new.siste_opplysninger_hentet_dato;
        l_behandling_dvh.dokumentasjon_svarfrist_dato   := :new.dokumentasjon_svarfrist_dato;
        l_behandling_dvh.initierende_journalpost_id     := :new.initierende_journalpost_id;
        l_behandling_dvh.initierende_dokument_id        := :new.initierende_dokument_id;

    when deleting
    then
        l_behandling_dvh.behandling_id                  := :old.id;
        l_behandling_dvh.fagsak_id                      := :old.saksnummer;
        l_behandling_dvh.status                         := :old.status;
        l_behandling_dvh.beh_type                       := :old.beh_type;
        l_behandling_dvh.registrert_tid                 := :old.registrert_dato;
        l_behandling_dvh.funksjonell_tid                := nvl(:old.endret_dato, :old.registrert_dato);
        l_behandling_dvh.registrert_av                  := :old.registrert_av;
        l_behandling_dvh.funksjonell_av                 := nvl(:old.endret_av, :old.registrert_av);
        l_behandling_dvh.siste_opplysninger_hentet_dato := :old.siste_opplysninger_hentet_dato;
        l_behandling_dvh.dokumentasjon_svarfrist_dato   := :old.dokumentasjon_svarfrist_dato;
        l_behandling_dvh.initierende_journalpost_id     := :old.initierende_journalpost_id;
        l_behandling_dvh.initierende_dokument_id        := :old.initierende_dokument_id;
    end case;
    

    insert into behandling_dvh
    (
	behandling_id,
	fagsak_id,
	-- beskrivende_koder           
	enhet,
	status,
	beh_type,
	resultat_type,
	-- tidsstyringsparameter                            
	registrert_tid,
	funksjonell_tid,
	registrert_av,
	funksjonell_av,
	-- 
	behandlingsmaate,
	fastsatt_av_land,
	henleggelse_grunn,
	siste_opplysninger_hentet_dato,
	dokumentasjon_svarfrist_dato,
	initierende_journalpost_id,
	initierende_dokument_id,
	dml_flagg,
    kildetabell
    )
    values
    (
	l_behandling_dvh.behandling_id,
	l_behandling_dvh.fagsak_id,
	-- beskrivende_koder           
	l_behandling_dvh.enhet,
	l_behandling_dvh.status,
	l_behandling_dvh.beh_type,
	l_behandling_dvh.resultat_type,
	-- tidsstyringsparameter                            
	l_behandling_dvh.registrert_tid,
	l_behandling_dvh.funksjonell_tid,
	l_behandling_dvh.registrert_av,
	l_behandling_dvh.funksjonell_av,
	-- 
	l_behandling_dvh.behandlingsmaate,
	l_behandling_dvh.fastsatt_av_land,
	l_behandling_dvh.henleggelse_grunn,
	l_behandling_dvh.siste_opplysninger_hentet_dato,
	l_behandling_dvh.dokumentasjon_svarfrist_dato,
	l_behandling_dvh.initierende_journalpost_id,
	l_behandling_dvh.initierende_dokument_id,
	l_behandling_dvh.dml_flagg,
    l_behandling_dvh.kildetabell
    );

exception
    when others 
    then 
 
        l_feillogg_dvh.kilde_tabell := 'BEHANDLING';
        l_feillogg_dvh.kilde_pk     := l_behandling_dvh.behandling_id;
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
        l_behandling_dvh.dml_flagg,
        l_feillogg_dvh.sqlcode,
        l_feillogg_dvh.sqlerrm
        );

end melosys_behandling_trg;
/