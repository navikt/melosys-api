--alter session set current_schema=<onsket_schema>;
create or replace view kodeverk_view_dvh
as
select kode, navn, 'FAGSAK_STATUS' kode_type     from MELOSYS_FAGSAK_STATUS union all
select kode, navn, 'BEHANDLING_STATUS' kode_type from MELOSYS_BEHANDLING_STATUS union all
select kode, navn, 'BEHANDLINGSMAATE' kode_type  from MELOSYS_BEHANDLINGSMAATE union all
select kode, navn, 'BEHRESULTAT_TYPE' kode_type  from MELOSYS_BEHRESULTAT_TYPE union all
select kode, navn, 'BEHANDLING_TYPE' kode_type   from MELOSYS_BEHANDLING_TYPE union all
select kode, navn, 'ROLLE_TYPE' kode_type        from MELOSYS_ROLLE_TYPE union all
select kode, navn, 'FAGSAK_TYPE' kode_type       from MELOSYS_FAGSAK_TYPE
;

