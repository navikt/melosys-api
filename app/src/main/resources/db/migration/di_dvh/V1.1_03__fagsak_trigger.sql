--alter session set current_schema=<onsket_schema>;
create or replace trigger melosys_fagsak_trg
after insert or update or delete on melosys_fagsak
for each row
declare
  l_fagsak_dvh    fagsak_dvh%rowtype;
  l_feillogg_dvh  feillogg_dvh%rowtype;
begin
    
    case 
    when inserting then l_fagsak_dvh.dml_flagg := 'I';
    when updating  then l_fagsak_dvh.dml_flagg := 'U';
    when deleting  then l_fagsak_dvh.dml_flagg := 'D';
    end case;

    
    case 
    when inserting or updating
    then 
        l_fagsak_dvh.fagsak_id       := :new.saksnummer;
        l_fagsak_dvh.gsak_saksnummer := :new.gsak_saksnummer;
        l_fagsak_dvh.fagsak_type     := :new.fagsak_type;
        l_fagsak_dvh.status          := :new.status;
        l_fagsak_dvh.registrert_tid  := :new.registrert_dato;
        l_fagsak_dvh.registrert_av   := :new.registrert_av;
        l_fagsak_dvh.funksjonell_av  := nvl(:new.endret_av, :new.registrert_av);
        l_fagsak_dvh.funksjonell_tid := nvl(:new.endret_dato, :new.registrert_dato);

    when deleting
    then
        l_fagsak_dvh.fagsak_id       := :old.saksnummer;
        l_fagsak_dvh.gsak_saksnummer := :old.gsak_saksnummer;
        l_fagsak_dvh.fagsak_type     := :old.fagsak_type;
        l_fagsak_dvh.status          := :old.status;
        l_fagsak_dvh.registrert_tid  := :old.registrert_dato;
        l_fagsak_dvh.registrert_av   := :old.registrert_av;
        l_fagsak_dvh.funksjonell_av  := nvl(:old.endret_av, :old.registrert_av);
        l_fagsak_dvh.funksjonell_tid := nvl(:old.endret_dato, :old.registrert_dato);
    end case;
    

    insert into fagsak_dvh
    (
	fagsak_id,
	gsak_saksnummer,
	fagsak_type,
	status,
	registrert_tid,
	funksjonell_tid,
	registrert_av,
	funksjonell_av,
	dml_flagg
    )
    values
    (
    l_fagsak_dvh.fagsak_id,
	l_fagsak_dvh.gsak_saksnummer,
	l_fagsak_dvh.fagsak_type,
	l_fagsak_dvh.status,
	l_fagsak_dvh.registrert_tid,
	l_fagsak_dvh.funksjonell_tid,
	l_fagsak_dvh.registrert_av,
	l_fagsak_dvh.funksjonell_av,
	l_fagsak_dvh.dml_flagg
    );

exception
    when others 
    then 
 
        l_feillogg_dvh.kilde_tabell := 'FAGSAK';
        l_feillogg_dvh.kilde_pk     := l_fagsak_dvh.fagsak_id;
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
        l_fagsak_dvh.dml_flagg,
        l_feillogg_dvh.sqlcode,
        l_feillogg_dvh.sqlerrm
        );

end;
/