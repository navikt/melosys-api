--alter session set current_schema=<onsket_schema>;
update aktor_dvh
set rolle = 'TRYGDEMYNDIGHET'
where rolle = 'MYNDIGHET';
