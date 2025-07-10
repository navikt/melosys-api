package no.nav.melosys.saksflytapi.domain;


public enum ProsessSteg {

    AVKLAR_ARBEIDSGIVER("AVKLAR_ARBEIDSGIVER", "Avklaring av norsk arbeidsgiver"),
    AVKLAR_MYNDIGHET("AVKLAR_MYNDIGHET", "Avklaring av utenlandsk trygdemyndighet"),
    AVSLUTT_SAK_OG_BEHANDLING("AVSLUTT_SAK_OG_BEHANDLING", "Avslutt fagsak og aktiv behandling"),
    AVSLUTT_TIDLIGERE_MEDL_PERIODE("AVSLUTT_TIDLIGERE_MEDL_PERIODE", "Avslutter tidligere periode i Medl"), // Gjelder Lovvalgperiode
    AVSLUTT_TIDLIGERE_MEDL_ANMODNINGSPERIODE("AVSLUTT_TIDLIGERE_MEDL_ANMODNINGSPERIODE", "Avslutter tidligere anmodningsperiode i Medl"),
    BESTEM_BEHANDLINGMÅTE_SED("BESTEM_BEHANDLINGMÅTE_SED", "Bestemmer videre behandlingsmåte for en mottatt SED etter registerkontroll"),
    BESTEM_BEHANDLINGSMÅTE_SVAR_ANMODNING_UNNTAK("BESTEM_BEHANDLINGSMÅTE_SVAR_ANMODNING_UNNTAK", "Oppdaterer behandling eller fatter vedtak etter mottak av svar på anmodning om unntak"),
    BESTILL_BREV("BESTILL_BREV", "Bestill brev til én mottaker via doksys"),
    DISTRIBUER_JOURNALPOST("DISTRIBUER_JOURNALPOST", "Distribuer journalpost"),
    DISTRIBUER_JOURNALPOST_UTLAND("DISTRIBUER_JOURNALPOST_UTLAND", "Distribuerer (sender) en journalpost til utlanlandsk myndighet"),
    GJENBRUK_OPPGAVE("GJENBRUK_OPPGAVE", "Gjenbruker eksisterende oppgave"),
    HENT_MOTTAKERINSTITUSJON_FORKORTET_PERIODE("HENT_MOTTAKERINSTITUSJON_FORKORTET_PERIODE", "Henter mottakerinstitusjon fra tidligere sendt BUC ved forkortet periode"),
    HENT_REGISTEROPPLYSNINGER("HENT_REGISTER_OPPL", "Henter registeropplysninger"),
    JFR_SETT_VURDER_DOKUMENT("JFR_SETT_VURDER_DOKUMENT", "Setter status til VURDER_DOKUMENT"),
    JFR_TILDEL_BEHANDLINGSOPPGAVE("JFR_TILDEL_BEHANDLINGSOPPGAVE", "Tildeler behandlingsoppgave for gjeldende fagsak til en saksbehandler"),
    LAGRE_ANMODNINGSPERIODE_MEDL("LAGRE_ANMODNINGSPERIODE_MEDL", "Lagrer en anmodningsperiode som under avklaring i MEDL"),
    LAGRE_LOVVALGSPERIODE_MEDL("LAGRE_LOVVALGSPERIODE_MEDL", "Lagrer en lovvalgsperiode i MEDL som foreløpig eller endelig"),
    LAGRE_MEDLEMSKAPSPERIODE_MEDL("LAGRE_MEDLEMSKAPSPERIODE_MEDL", "Lagrer en medlemskapsperiode (Folketrygden) i MEDL som endelig"),
    OPPDATER_FAKTURAMOTTAKER("OPPDATER_FAKTURAMOTTAKER", "Oppdaterer fakturamottaker på eventuell faktura til sak"),
    OPPDATER_OG_FERDIGSTILL_JOURNALPOST("OPPDATER_OG_FERDIGSTILL_JOURNALPOST", "Oppdaterer og ferdigstiller journalposten i Joark"),
    OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT("OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT", "Oppdaterer oppgave med frist og beskrivelse"),
    OPPDATER_SAKSRELASJON("OPPDATER_SAKSRELASJON", "Oppdaterer saksrelasjon mellom melosys-sak og rina-sak"),
    OPPRETT_ANMODNINGSPERIODESVAR("ANMODNINGSPERIODESVAR", "Oppretter svar for en anmodningsperiode"),
    OPPRETT_ANMODNINGSPERIODE_FRA_SED("OPPRETT_ANMODNINGSPERIODE_FRA_SED", "Oppretter anmodningsperiode fra SED"),
    OPPRETT_ARKIVSAK("OPPRETT_ARKIVSAK", "Oppretter arkivsak"),
    OPPRETT_FAKTURASERIE("OPPRETT_FAKTURASERIE", "Oppretter fakturaserie i faktureringskomponenten"),
    OPPRETT_MOTTATTEOPPLYSNINGER("OPPRETT_MOTTATTEOPPLYSNINGER", "Oppretter mottatteopplysninger"),
    OPPRETT_OG_JOURNALFØR_BREV("OPPRETT_BREV", "Opprett nytt brev"),
    OPPRETT_TIDLIGERE_JOURNALPOSTER_FOR_SAK("OPPRETT_TIDLIGERE_JOURNALPOSTER_FOR_SAK", "Oppretter journalposter tilbake i tid dersom de har tidligere har blitt markert som uidentifiserbare"),
    OPPRETT_OG_FERDIGSTILL_JOURNALPOST_FRA_ALTINN("OPPRETT_OG_FERDIGSTILL_JOURNALPOST_FRA_ALTINN", "Oppretter og ferdigstiller journalpost fra Altinn"),
    OPPRETT_OPPGAVE("OPPRETT_OPPGAVE", "Oppretter behandlingsoppgave"),
    OPPRETT_SAK_OG_BEH("OPPRETT_SAK_OG_BEH", "Oppretter ny sak og behandling i Melosys"),
    OPPRETT_NY_BEHANDLING("OPPRETT_NY_BEHANDLING", "Oppretter ny behandling i Melosys"),
    OPPRETT_SAK_OG_BEHANDLING_FRA_ALTINN_SØKNAD("OPPRETT_SAK_OG_BEHANDLING_FRA_ALTINN_SØKNAD", "Oppretter fagsak og behandling fra mottatt Altinn-søknad"),
    OPPRETT_SEDDOKUMENT("OPPRETT_SEDDOKUMENT", "Oppretter saksopplysning fra SED"),
    OPPRETT_SED_GRUNNLAG("OPPRETT_SED_GRUNNLAG", "Oppretter mottatteopplysninger fra SED"),
    REGISTERKONTROLL("REGISTERKONTROLL", "Utfører registerkontroll for en behandling"),
    REPLIKER_BEHANDLING("REPLIKER_BEHANDLING", "Replikerer den første, avsluttede behandlingen i Melosys og setter den til OPPRETTET"),
    SED_MOTTAK_FERDIGSTILL_JOURNALPOST("SED_MOTTAK_FERDIGSTILL_JOURNALPOST", "Journalføring av innkommende SED"),
    SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH("SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH", "Opprett fagsak og behandling"),
    SED_MOTTAK_OPPRETT_NY_BEHANDLING("SED_MOTTAK_OPPRETT_NY_BEHANDLING", "Oppretter ny behandling for oppdatert SED"),
    SED_MOTTAK_RUTING("SED_MOTTAK_RUTING", "Bestemmer videre behandling for innkommende SED"),
    SEND_ANMODNING_OM_UNNTAK("SEND_ANMODNING_OM_UNNTAK", "Sender anmodning om unntak til utenlandsk trygdemyndighet"),
    SEND_FORVALTNINGSMELDING("SEND_FORVALTNINGSMELDING", "Send forvaltningsmelding til søker"),
    SEND_GODKJENNING_REGISTRERING_UNNTAK("SEND_GODKJENNING_REGISTRERING_UNNTAK", "Varsler utland om godkjent unntaksperiode"),
    SEND_HENLEGGELSESBREV("SEND_HENLEGGELSESBREV", "Send henleggelsesbrev"),
    SEND_ORIENTERINGSBREV_VIDERESENDING_SØKNAD("SEND_ORIENTERINGSBREV_VIDERESENDING_SØKNAD", "Sender orienteringsbrev til bruker ved videresending av søknad"),
    SEND_ORIENTERING_ANMODNING_UNNTAK("SEND_ORIENTERING_ANMODNING_UNNTAK", "Send orienteringsbrev til bruker ved anmodning om unntak"),
    SEND_SVAR_ANMODNING_UNNTAK("SEND_SVAR_ANMODNING_UNNTAK", "Sender svar på anmodning om unntak"),
    SEND_VEDTAKSBREV_INNLAND("SEND_VEDTAKSBREV_INNLAND", "Sender vedtaksbrev innland"),
    SEND_VEDTAK_UTLAND("SEND_VEDTAK_UTLAND", "Sender vedtaket til utland"),
    @Deprecated
    SOB_BEHANDLING_AVSLUTTET("SOB_BEHANDLING_AVSLUTTET", "Oppdaterer SOB (Sak Og Behandling) om avsluttet behandling"),
    @Deprecated
    SOB_BEHANDLING_OPPRETTET("SOB_BEHANDLING_OPPRETTET", "Oppdaterer SOB (Sak Og Behandling) om opprettet behandling"),
    @Deprecated
    UKJENT("UKJENT", "Ukjent steg"),
    UTPEKING_SEND_AVSLAG("UTPEKING_SEND_AVSLAG", "Send SED A004 til alle arbeidsland"),
    VIDERESEND_SØKNAD("VIDERESEND_SØKNAD", "Oppretter journalpost eller sender SED med søknad som vedlegg"),
    VURDER_INNGANGSVILKÅR("VURDER_INNGANGSVILKÅR", "Vurderer inngangsvilkår"),
    SEND_MANGLENDE_INNBETALING_VARSELBREV("SEND_MANGLENDE_INNBETALING_VARSELBREV", "Sender varselbrev om manglende innbetaling"),
    OPPRETT_MANGLENDE_INNBETALING_BEHANDLING("OPPRETT_MANGLENDE_INNBETALING_BEHANDLING", "Oppretter ny behandling for manglende innbetaling"),
    KANSELLER_FAKTURASERIE("KANSELLER_FAKTURASERIE", "Kansellerer fakturaserie og tilhørende fakturaer"),
    SEND_MELDING_OM_VEDTAK("SEND_MELDING_OM_VEDTAK", "Sender melding om vedtak"),
    OPPRETT_AARSAVREGNING_BEHANDLING("OPPRETT_AARSAVREGNING_BEHANDLING", "Oppretter ny behandling for årsavregning"),
    BEREGN_OG_SEND_FAKTURA("BEREGN_OG_SEND_FAKTURA","Beregner trygdeavgift og sender faktura"),
    OPPRETT_SATSBEHANDLING("OPPRETT_SATSBEHANDLING", "Oppretter satsendringsbehandling basert på tidligere behandling"),
    SEND_FAKTURA_AARSAVREGNING("SEND_FAKTURA_AARSAVREGNING", "Sender faktura for årsavregning"),
    TILBAKESTILL_TRYGDEAVGIFT("TILBAKESTILL_TRYGDEAVGIFT", "Tilbakestiller trygdeavgift for en behandling slik at nye satser brukes"),
    RESET_ÅPNE_ÅRSAVREGNINGER("RESET_ÅPNE_ÅRSAVREGNINGER", "Resetter åpne årsavregninger for å bruke grunnlag fra ny vurdering.");


    private final String kode;
    private final String beskrivelse;

    ProsessSteg(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
