--disse tabellene trenger noen grants slik at brukeren til DVH kan lese dem. 
--alter session set current_schema = <onsket_schema>;
create table behandling_dvh 
(
	-- nøkkel_verdier         
	trans_id                       number(19,0) GENERATED ALWAYS AS IDENTITY, 
	behandling_id                  number(19,0) not null, 
	fagsak_id                      varchar2(99) not null, 
	-- beskrivende_koder           
	enhet                          varchar2(10) ,		  
	status                         varchar2(99) not null, 
	beh_type                       varchar2(99) not null, 
	resultat_type                  varchar2(99) ,         
	-- tidsstyringsparameter                            
	registrert_tid                 timestamp(6) not null, 
	funksjonell_tid                timestamp(6) not null, 
	trans_tid                      timestamp(6) default current_timestamp not null, 
	registrert_av                  varchar2(99) not null, 
	funksjonell_av                 varchar2(99) not null, 
	-- 
	behandlingsmaate               varchar2(99),          
	fastsatt_av_land               varchar2(99),          
	henleggelse_grunn              varchar2(99),  	      
	siste_opplysninger_hentet_dato timestamp(6),          
	dokumentasjon_svarfrist_dato   timestamp(6),          
	initierende_journalpost_id     varchar2(99),          
	initierende_dokument_id        varchar2(99),          
	dml_flagg 		               varchar(1)   not null, 
    kildetabell                    varchar(30)  not null, 
	constraint pk_behandling_dvh primary key (trans_id)
) column store compress for query high
  partition by range(funksjonell_tid) interval(numtoyminterval(1,'month'))
( partition p0 values less than ( to_date('20060101','yyyymmdd') ) )
;

create table fagsak_dvh 
(
	-- nøkkel_verdier
	trans_id        number(19,0) GENERATED ALWAYS AS IDENTITY, 
	fagsak_id       varchar2(99) not null, 
	gsak_saksnummer number(19,0) , 
	fagsak_type     varchar2(99) , 
	status          varchar2(99) not null, 
	registrert_tid  timestamp(6) not null, 
	funksjonell_tid timestamp(6) not null, 
	trans_tid       timestamp(6) default current_timestamp not null, 
	registrert_av   varchar2(99) , 
	funksjonell_av  varchar2(99) ,
	dml_flagg 		varchar(1)   not null, -- I(nsert), U(pdate), D(elete)
	constraint pk_fagsak_dvh primary key (trans_id)
) column store compress for query high
  partition by range(funksjonell_tid) interval(numtoyminterval(1,'month'))
( partition p0 values less than ( to_date('20060101','yyyymmdd') ) )
;
create table aktor_dvh 
(
    trans_id             number(19,0) GENERATED ALWAYS AS IDENTITY, 
	id                   number(19,0) not null , 
	aktoer_id            varchar2(99), 
	institusjon_id       varchar2(99), 
	rolle                varchar2(99) not null , 
	orgnr                varchar2(99), 
	saksnummer           varchar2(99) not null , 
	utenlandsk_person_id varchar2(99), 
	representerer        varchar2(99), 
	registrert_tid       timestamp(6) not null , 
	funksjonell_tid      timestamp(6) not null , 
	trans_tid            timestamp(6) default current_timestamp not null, 
	registrert_av        varchar2(99), 
	funksjonell_av       varchar2(99),
	dml_flagg 		     varchar(1)   not null, -- I(nsert), U(pdate), D(elete)
	constraint pk_aktor_dvh primary key (trans_id)
) column store compress for query high
  partition by range(funksjonell_tid) interval(numtoyminterval(1,'month'))
( partition p0 values less than ( to_date('20060101','yyyymmdd') ) )
;
create table feillogg_dvh
(
    trans_id     NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    trans_tid    timestamp default current_timestamp not null,
    kilde_tabell varchar2(30) not null,
    kilde_pk     NUMBER(19) null,
    dml_flagg    char(1) null,
    sqlcode      varchar2(20) null,
    sqlerrm      varchar2(1000) null,   
    CONSTRAINT pk_feillogg_dvh PRIMARY KEY (trans_id)
);

