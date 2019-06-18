package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum ProsessSteg implements Kodeverk {

    // NB! Disse skal være i logisk rekkefølge
    MOT_VURDER_AUTOMATISK_JFR("MOT_VURDER_AUTOMATISK_JFR", "Vurder om journalføring kan skje automatisk"),

    // Journalføring
    JFR_VALIDERING("JFR_VALIDERING", "Grunnleggende validering"),
    JFR_VURDER_JOURNALFOERINGSTYPE("JFR_VURDER_JOURNALFOERINGSTYPE", "Nytt innkommende dokument. Saksbehandler vurderer behov for opprettelse av ny behandling."),
    JFR_AKTØR_ID("JFR_AKTØR_ID", "Henter aktørID"),
    JFR_OPPRETT_SAK_OG_BEH("JFR_OPPRETT_SAK_OG_BEH", "Oppretter ny sak og behandling i Melosys"),
    REPLIKER_BEHANDLING("REPLIKER_BEHANDLING", "Replikerer den første, avsluttede behandlingen i Melosys og setter den til OPPRETTET"),
    JFR_OPPRETT_SØKNAD("JFR_OPPRETT_SØKNAD", "Oppretter ny søknad i Melosys"),
    JFR_OPPRETT_GSAK_SAK("JFR_OPPRETT_GSAK_SAK", "Oppretter Sak i GSAK"),
    STATUS_BEH_OPPR("STATUS_BEH_OPPR", "Oppdater Sak og Behandling ved oppretting av behandling"),
    JFR_OPPDATER_JOURNALPOST("JFR_OPPDATER_JOURNALPOST", "Oppdaterer journalposten i Joark"),
    JFR_FERDIGSTILL_JOURNALPOST("JFR_FERDIGSTILL_JOURNALPOST", "Ferdigstiller journalposten i Joark"),
    JFR_SETT_VURDER_DOKUMENT("JFR_SETT_VURDER_DOKUMENT", "Setter status til VURDER_DOKUMENT"),
    JFR_TILDEL_BEHANDLINGSOPPGAVE("JFR_TILDEL_BEHANDLINGSOPPGAVE", "Tildeler behandlingsoppgave for gjeldende fagsak til en saksbehandler"),
    JFR_HENT_PERS_OPPL("JFR_HENT_PERS_OPPL", "Hent personopplysninger fra TPS"),
    JFR_VURDER_INNGANGSVILKÅR("JFR_VURDER_INNGANGSVILKÅR", "Vurderer inngangsvilkår"),

    // Hent saksopplysninger
    HENT_ARBF_OPPL("HENT_ARBF_OPPL", "Hent arbeidsforholdopplysninger fra AAREG"),
    HENT_INNT_OPPL("HENT_INNT_OPPL", "Hent inntektopplysninger fra INNTK"),
    HENT_ORG_OPPL("HENT_ORG_OPPL", "Hent organisasjoner fra EREG"),
    HENT_MEDL_OPPL("HENT_MEDL_OPPL", "Hent medlemskapsopplysninger fra MEDL"),
    HENT_SOB_SAKER("HENT_SOB_SAKER", "Hent saker fra Sak og behandling"),
    OPPFRISK_SAKSOPPLYSNINGER("OPPFRISK_SAKSOPPLYSNINGER", "Oppfrisking av saksopplysninger"),

    //Gsak
    GSAK_OPPRETT_OPPGAVE("GSAK_OPPRETT_OPPGAVE", "Oppretter oppgave i GSAK"),

    SEND_FORVALTNINGSMELDING("SEND_FORVALTNINGSMELDING", "Send forvaltningsmelding til søker"),

    FEILET_MASKINELT("FEILET_MASKINELT", "Feilet maskinelt"),

    //Anmodning om unntak
    AOU_VALIDERING("AOU_VALIDERING", "Validering av data for anmodning om unntak"),
    AOU_OPPDATER_RESULTAT("AOU_OPPDATER_RESULTAT", "Oppdatering av behandlingsresultat for anmodning om unntak"),
    AOU_AVKLAR_MYNDIGHET("AOU_AVKLAR_MYNDIGHET", "Avklaring av utenlandsk trygdemyndighet"),
    AOU_OPPDATER_MEDL("AOU_OPPDATER_MEDL", "Oppdatering av medlemskap med anmodning om unntak"),
    AOU_SEND_BREV("AOU_SEND_BREV", "Send orienteringsbrev og A001 for anmodning om unntak"),
    AOU_SEND_SED("AOU_SEND_SED","Send elektronisk SED A001"),

    //Iverksett Vedtak
    IV_FORKORT_PERIODE("IV_FORKORT_PERIODE", "Legger til endringsgrunn i AVKLARTEFAKTA for hvorfor perioden er forkortet"),
    IV_VALIDERING("IV_VALIDERING", "Validere iverksett vedtak"),
    IV_OPPDATER_RESULTAT("IV_OPPDATER_RESULTAT", "Oppdatering av behandlingsresultat"),
    IV_AVKLAR_MYNDIGHET("IV_AVKLAR_MYNDIGHET", "Avklaring av utenlandsk trygdemyndighet"),
    IV_AVKLAR_ARBEIDSGIVER("IV_AVKLAR_ARBEIDSGIVER", "Avklaring av norsk arbeidsgiver"),
    IV_OPPDATER_MEDL("IV_OPPDATER_MEDL", "Oppdatering av medlemskap"),
    IV_SEND_BREV("IV_SEND_BREV", "Send brev etter iverksett vedtak"),
    IV_SEND_SED("IV_SEND_SED", "Send SED etter et eller annet"),
    IV_AVSLUTT_BEHANDLING("IV_AVSLUTT_BEHANDLING", "Avslutt fagsak og aktiv behandling"),
    IV_STATUS_BEH_AVSL("IV_STATUS_BEH_AVSL", "Oppdater Sak og Behandling ved lukking av behandling"),

    MANGELBREV("MANGELBREV", "Opprett mangelbrev"),

    //Henlegg sak
    HS_OPPDATER_RESULTAT("HS_OPPDATER_RESULTAT", "Oppdatering av behandlingsresultat"),
    HS_HENLEGG_SAK("HS_HENLEGG_SAK", "Henlegg en sak"),
    HS_SEND_BREV("HS_SEND_BREV", "Opprett henleggelsesbrev"),

    //Unntak medlemskap
    REG_UNNTAK_OPPRETT_SAK_OG_BEH("REG_UNNTAK_OPPRETT_SAK_OG_BEH","Opprett sak og behandling"),
    REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET("REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET", "Oppdaterer status på sak i sob til opprettet"),
    REG_UNNTAK_FERDIGSTILL_JOURNALPOST("REG_UNNTAK_FERDIGSTILL_JOURNALPOST","Ferdigstiller journalpost"),
    REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE("REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE", "Avslutter tidligere periode i Medl hvis SED er endring"),
    REG_UNNTAK_OPPRETT_SEDDOKUMENT("REG_UNNTAK_OPPRETT_SEDDOKUMENT", "Oppretter sedinfo dokument"),
    REG_UNNTAK_HENT_PERSON("REG_UNNTAK_HENT_PERSON","Henter person tilknyttet SED"),
    REG_UNNTAK_HENT_MEDLEMSKAP("REG_UNNTAK_HENT_REGISTEROPPLYSNINGER", "Henter opplysninger om medlemskap"),
    REG_UNNTAK_HENT_YTELSER("REG_UNNTAK_HENT_YTELSER", "Henter opplysninger om ytelser"),
    REG_UNNTAK_REGISTERKONTROLL("REG_UNNTAK_REGISTERKONTROLL", "Validerer informasjon om en unntaksperiode"),
    REG_UNNTAK_BESTEM_BEHANDLINGSMAATE("REG_UNNTAK_BESTEM_BEHANDLINGSMAATE", "Bestem om søknad skal registreres automatisk eller behandles manuelt"),
    REG_UNNTAK_OPPDATER_MEDL("REG_UNNTAK_OPPDATER_MEDL", "Sett periode endelig i MEDL"),
    REG_UNNTAK_OPPRETT_OPPGAVE("REG_UNNTAK_OPPRETT_OPPGAVE","Opprett oppgave for manuell behandling"),
    REG_UNNTAK_AVSLUTT_BEHANDLING("REG_UNNTAK_AVSLUTT_BEHANDLING", "Avslutt behandling"),
    REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET("REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET", "Oppdaterer status på sak i sob til avsluttet"),
    REG_UNNTAK_PERIODE_IKKE_GODKJENT("REG_UNNTAK_PERIODE_IKKE_GODKJENT", "Unntaksperiode avvist av saksbehandler"),
    REG_UNNTAK_UNDER_AVKLARING("REG_UNNTAK_UNDER_AVKLARING", "Unntaksperiode under avklaring"),

    FERDIG("FERDIG", "Prosessen er ferdig");


    private String kode;
    private String beskrivelse;

    ProsessSteg(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
