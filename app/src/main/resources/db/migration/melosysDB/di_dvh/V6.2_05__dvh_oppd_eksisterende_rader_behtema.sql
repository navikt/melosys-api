-- Migrere eksisterende rader --
update BEHANDLING_DVH BDVH set (beh_type, beh_tema) = (select beh_type, beh_tema from BEHANDLING B where B.ID = BDVH.BEHANDLING_ID);
ALTER TABLE BEHANDLING_DVH MODIFY beh_tema NOT NULL;

-- Oppdater kodeverk-view --
create or replace view kodeverk_view_dvh
as
select kode, navn, 'FAGSAK_STATUS' kode_type     from FAGSAK_STATUS union all
select kode, navn, 'BEHANDLING_STATUS' kode_type from BEHANDLING_STATUS union all
select kode, navn, 'BEHANDLINGSMAATE' kode_type  from BEHANDLINGSMAATE union all
select kode, navn, 'BEHRESULTAT_TYPE' kode_type  from BEHANDLINGSRESULTAT_TYPE union all
select kode, navn, 'BEHANDLING_TYPE' kode_type   from BEHANDLING_TYPE union all
select kode, navn, 'BEHANDLING_TEMA' kode_type   from BEHANDLING_TEMA union all
select kode, navn, 'ROLLE_TYPE' kode_type        from ROLLE_TYPE union all
select kode, navn, 'FAGSAK_TYPE' kode_type       from FAGSAK_TYPE
;