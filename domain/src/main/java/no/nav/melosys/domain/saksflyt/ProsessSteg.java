package no.nav.melosys.domain.saksflyt;

import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.exception.TekniskException;

public enum ProsessSteg implements Kodeverk {

    // NB! Disse skal være i logisk rekkefølge
    MOT_VURDER_AUTOMATISK_JFR("MOT_VURDER_AUTOMATISK_JFR", "Vurder om journalføring kan skje automatisk"),

    // Journalføring
    JFR_OPPRETT_SAK_OG_BEH("JFR_OPPRETT_SAK_OG_BEH", "Oppretter ny sak og behandling i Melosys"),
    REPLIKER_BEHANDLING("REPLIKER_BEHANDLING", "Replikerer den første, avsluttede behandlingen i Melosys og setter den til OPPRETTET"),
    JFR_OPPRETT_SØKNAD("JFR_OPPRETT_SØKNAD", "Oppretter ny søknad i Melosys"),
    OPPRETT_ARKIVSAK("OPPRETT_ARKIVSAK", "Oppretter arkivsak"),
    STATUS_BEH_OPPR("STATUS_BEH_OPPR", "Oppdater Sak og Behandling ved oppretting av behandling"),
    JFR_OPPDATER_SAKSRELASJON("JFR_OPPDATER_SAKSRELASJON", "Oppdaterer saksrelasjon hvis journalposten omhandler en SED"),
    JFR_FERDIGSTILL_JOURNALPOST("JFR_FERDIGSTILL_JOURNALPOST", "Ferdigstiller journalposten i Joark"),
    OPPDATER_OG_FERDIGSTILL_JOURNALPOST("OPPDATER_OG_FERDIGSTILL_JOURNALPOST", "Oppdaterer og ferdigstiller journalposten i Joark"),
    JFR_SETT_VURDER_DOKUMENT("JFR_SETT_VURDER_DOKUMENT", "Setter status til VURDER_DOKUMENT"),
    JFR_TILDEL_BEHANDLINGSOPPGAVE("JFR_TILDEL_BEHANDLINGSOPPGAVE", "Tildeler behandlingsoppgave for gjeldende fagsak til en saksbehandler"),
    HENT_REGISTER_OPPL("HENT_REGISTER_OPPL", "Henter registeropplysninger"),
    JFR_VURDER_INNGANGSVILKÅR("JFR_VURDER_INNGANGSVILKÅR", "Vurderer inngangsvilkår"),

    // Oppgave
    VURDER_GJENBRUK_OPPGAVE("VURDER_GJENBRUK_OPPGAVE", "Vurder om eksisterende oppgave skal gjenbrukes"),
    GJENBRUK_OPPGAVE("GJENBRUK_OPPGAVE", "Gjenbruker eksisterende oppgave"),
    OPPRETT_OPPGAVE("GSAK_OPPRETT_OPPGAVE", "Oppretter behandlingsoppgave"),

    SEND_FORVALTNINGSMELDING("SEND_FORVALTNINGSMELDING", "Send forvaltningsmelding til søker"),
    MANGELBREV("MANGELBREV", "Opprett mangelbrev"),

    FEILET_MASKINELT("FEILET_MASKINELT", "Feilet maskinelt"),

    //Anmodning om unntak
    AOU_VALIDERING("AOU_VALIDERING", "Validering av data for anmodning om unntak"),
    AOU_OPPDATER_RESULTAT("AOU_OPPDATER_RESULTAT", "Oppdatering av behandlingsresultat for anmodning om unntak"),
    AOU_AVKLAR_MYNDIGHET("AOU_AVKLAR_MYNDIGHET", "Avklaring av utenlandsk trygdemyndighet"),
    AOU_OPPDATER_MEDL("AOU_OPPDATER_MEDL", "Oppdatering av medlemskap med anmodning om unntak"),
    AOU_SEND_BREV("AOU_SEND_BREV", "Send orienteringsbrev og A001 for anmodning om unntak"),
    AOU_SEND_SED("AOU_SEND_SED","Send elektronisk SED A001"),
    AOU_OPPDATER_OPPGAVE("AOU_OPPDATER_OPPGAVE", "Oppdatering av oppgave med frist og beskrivelse"),

    //Svar anmodning om unntak
    AOU_SVAR_OPPRETT_ANMODNINGSPERIODESVAR("AOU_OPPRETT_ANMODNINGSPERIODESVAR","Oppretter svar for en anmodningsperiode"),
    AOU_SVAR_OPPDATER_BEHANDLING("AOU_OPPDATER_BEHANDLING","Oppdater behandling"),

    //Mottak anmodning om unntak
    AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE("AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE", "Opprett anmodningsperiode"),
    AOU_MOTTAK_SAK_OG_BEHANDLING_OPPRETTET("AOU_MOTTAK_SAK_OG_BEHANDLING_OPPRETTET", "Oppdaterer status på sak i sob til opprettet"),
    AOU_MOTTAK_AVSLUTT_TIDLIGERE_PERIODE("AOU_MOTTAK_AVSLUTT_TIDLIGERE_PERIODE", "Avslutter tidligere periode Medl hvis SED er endring"),
    AOU_MOTTAK_HENT_REGISTEROPPLYSNINGER("AOU_MOTTAK_HENT_REGISTEROPPLYSNINGER", "Henter saksopplysninger fra registre"),
    AOU_MOTTAK_REGISTERKONTROLL("AOU_MOTTAK_REGISTERKONTROLL", "Validerer informasjon om en unntaksperiode"),
    AOU_MOTTAK_OPPRETT_PERIODE_MEDL("AOU_MOTTAK_OPPRETT_PERIODE_MEDL", "Opprett periode under avklaring i Medl"),
    AOU_MOTTAK_OPPRETT_OPPGAVE("AOU_MOTTAK_OPPRETT_OPPGAVE", "Opprett oppgave for manuell behandling"),

    //Svar på mottatt anmodning om unntak
    AOU_MOTTAK_SVAR_OPPDATER_MEDL("AOU_MOTTAK_SVAR_OPPDATER_MEDL", "Oppdater periode i Medl"),
    AOU_MOTTAK_SVAR_SEND_SED("AOU_MOTTAK_SVAR_SEND_SED","Send svar-sed"),
    AOU_MOTTAK_SVAR_OPPRETT_JOURNALPOST("AOU_MOTTAK_SVAR_OPPRETT_JOURNALPOST", "Oppretter en journalpost av SEDen som skal sendes"),
    AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST("AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST", "Distribuerer (sender) journalposten"),
    AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET("AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET","Oppdaterer status på sak i sob til avsluttet"),

    //Iverksett Vedtak
    IV_FORKORT_PERIODE("IV_FORKORT_PERIODE", "Legger til endringsgrunn i AVKLARTEFAKTA for hvorfor perioden er forkortet"),
    IV_VALIDERING("IV_VALIDERING", "Validere iverksett vedtak"),
    IV_OPPDATER_RESULTAT("IV_OPPDATER_RESULTAT", "Oppdatering av behandlingsresultat"),
    IV_AVKLAR_MYNDIGHET("IV_AVKLAR_MYNDIGHET", "Avklaring av utenlandsk trygdemyndighet"),
    IV_AVKLAR_ARBEIDSGIVER("IV_AVKLAR_ARBEIDSGIVER", "Avklaring av norsk arbeidsgiver"),
    IV_OPPDATER_MEDL("IV_OPPDATER_MEDL", "Oppdatering av medlemskap"),
    IV_SEND_BREV("IV_SEND_BREV", "Send brev etter iverksett vedtak"),
    IV_SEND_SED("IV_SEND_SED", "Send SED etter iverksett vedtak"),
    IV_OPPRETT_AVGIFTSOPPGAVE("IV_OPPRETT_AVGIFTSOPPGAVE", "Oppretter en vurderingsoppgave for innregistrering i avgiftsystemet"),
    IV_AVSLUTT_BEHANDLING("IV_AVSLUTT_BEHANDLING", "Avslutt fagsak og aktiv behandling"),
    IV_STATUS_BEH_AVSL("IV_STATUS_BEH_AVSL", "Oppdater Sak og Behandling ved lukking av behandling"),

    //Utpek annet land
    UL_DISTRIBUER_JOURNALPOST("UL_DISTRIBUER_JOURNALPOST", "Distribuerer (sender) journalposten dersom den ble opprettet"),

    //Henlegg sak
    HS_OPPDATER_RESULTAT("HS_OPPDATER_RESULTAT", "Oppdatering av behandlingsresultat"),
    HS_HENLEGG_SAK("HS_HENLEGG_SAK", "Henlegg en sak"),
    HS_SEND_BREV("HS_SEND_BREV", "Opprett henleggelsesbrev"),

    // Videresend søknad
    VS_OPPDATER_RESULTAT("VS_OPPDATER_RESULTAT", "Oppdatering av behandlingsresultat"),
    VS_AVKLAR_MYNDIGHET("VS_AVKLAR_MYNDIGHET", "Avklaring av utenlandsk trygdemyndighet"),
    VS_SEND_ORIENTERINGSBREV("VS_SEND_ORIENTERINGSBREV", "Opprett orienteringsbrev og brev med vedlagt søknad"),
    VS_SEND_SOKNAD("VS_SEND_SOKNAD", "Opprett journalpost eller SED med søknad som vedlegg"),
    VS_DISTRIBUER_JOURNALPOST("VS_DISTRIBUER_JOURNALPOST", "Distribuerer (sender) journalposten dersom den ble opprettet"),

    //Mottak av SED
    SED_MOTTAK_OPPRETT_JFR_OPPG("SED_MOTTAK_OPPRETT_JFR_OPPG", "Oppretter journalføringsoppgave for SED som ikke skal behandles automatisk"),
    SED_MOTTAK_HENT_EESSI_MELDING("SED_MOTTAK_HENT_EESSI_MELDING", "Henter saksopplysninger fra mottatt SED"),
    SED_MOTTAK_RUTING("SED_MOTTAK_RUTING", "Bestemmer videre behandling for innkommende SED"),
    SED_MOTTAK_OPPRETT_NY_BEHANDLING("SED_MOTTAK_OPPRETT_NY_BEHANDLING", "Oppretter ny behandling for oppdatert SED"),
    SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH("SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH","Opprett fagsak og behandling"),
    SED_MOTTAK_OPPRETT_SAK("SED_MOTTAK_OPPRETT_SAK","Oppretter sak for ny behandling"),
    SED_MOTTAK_OPPDATER_SAKSRELASJON("SED_MOTTAK_OPPDATER_SAKSRELASJON","Oppdaterer saksrelasjon for ny gsak-sak"),
    SED_MOTTAK_FERDIGSTILL_JOURNALPOST("SED_MOTTAK_FERDIGSTILL_JOURNALPOST", "Journalføring av innkommende SED"),

    SED_GENERELL_SAK_HENT_PERSON("SED_GENERELL_SAK_HENT_PERSON", "Henter person tilknyttet behandling"),
    SED_GENERELL_SAK_OPPRETT_OPPGAVE("SED_GENERELL_SAK_OPPRETT_OPPGAVE", "Oppretter oppgave for behandling"),

    // Journalføring av mottatt anmodning om unntak (brev)
    JFR_AOU_BREV_OPPRETT_FAGSAK_OG_BEHANDLING("JFR_AOU_BREV_OPPRETT_FAGSAK_OG_BEHANDLING", "Opprett fagsak og behandling"),
    JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST("JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST", "Journalføring av anmodning om unntak brev"),
    JFR_AOU_BREV_OPPRETT_SEDDOKUMENT("JFR_AOU_BREV_OPPRETT_SEDDOKUMENT", "Oppretter sed-dokument"),

    //Arbeid i flere land, mottak av A003
    AFL_SAK_OG_BEHANDLING_OPPRETTET("AFL_SAK_OG_BEHANDLING_OPPRETTET","Oppdaterer status på sak i sob til opprettet"),
    AFL_AVSLUTT_TIDLIGERE_PERIODE("AFL_AVSLUTT_TIDLIGERE_PERIODE","Avslutter tidligere periode hvis oppdatert SED"),
    AFL_HENT_REGISTEROPPLYSNINGER("AFL_HENT_REGISTEROPPLYSNINGER","Innhenter registeropplysninger"),
    AFL_VURDER_INNGANGSVILKÅR("AFL_VURDER_INNGANGSVILKÅR","Vurderer inngangsvilkår når Norge er utpekt"),
    AFL_OPPRETT_BEHANDLINGSGRUNNLAG("AFL_OPPRETT_BEHANDLINGSGRUNNLAG","Oppretter behandlingsgrunnlag"),
    AFL_REGISTERKONTROLL("AFL_REGISTERKONTROLL","Utfører registerkontroll"),
    AFL_OPPRETT_OPPGAVE("AFL_OPPRETT_OPPGAVE","Oppretter oppgave til manuell behandling"),

    // Svar på A003
    AFL_SVAR_SEND_AVSLAG("AFL_SVAR_SEND_AVSLAG", "Send SED A004 til alle arbeidsland"),
    AFL_SVAR_AVSLUTT_BEHANDLING("AFL_SVAR_AVSLUTT_BEHANDLING", "Avslutter behandling etter utpeking er avslått"),

    //Unntak medlemskap
    REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET("REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET", "Oppdaterer status på sak i sob til opprettet"),
    REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE("REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE", "Avslutter tidligere periode i Medl hvis SED er endring"),
    REG_UNNTAK_OPPRETT_SEDDOKUMENT("REG_UNNTAK_OPPRETT_SEDDOKUMENT", "Oppretter sedinfo dokument"),
    REG_UNNTAK_HENT_REGISTEROPPLYSNINGER("REG_UNNTAK_HENT_REGISTEROPPLYSNINGER", "Henter saksopplysninger fra registre"),
    REG_UNNTAK_REGISTERKONTROLL("REG_UNNTAK_REGISTERKONTROLL", "Validerer informasjon om en unntaksperiode"),
    REG_UNNTAK_BESTEM_BEHANDLINGSMAATE("REG_UNNTAK_BESTEM_BEHANDLINGSMAATE", "Bestem om søknad skal registreres automatisk eller behandles manuelt"),
    REG_UNNTAK_OPPDATER_MEDL("REG_UNNTAK_OPPDATER_MEDL", "Sett periode endelig i MEDL"),
    REG_UNNTAK_VARSLE_UTLAND("REG_UNNTAK_VARSLE_UTLAND", "Varsler utland om godkjent unntaksperiode"),
    REG_UNNTAK_OPPRETT_OPPGAVE("REG_UNNTAK_OPPRETT_OPPGAVE","Opprett oppgave for manuell behandling"),
    REG_UNNTAK_AVSLUTT_BEHANDLING("REG_UNNTAK_AVSLUTT_BEHANDLING", "Avslutt behandling"),
    REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET("REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET", "Oppdaterer status på sak i sob til avsluttet"),
    REG_UNNTAK_PERIODE_IKKE_GODKJENT("REG_UNNTAK_PERIODE_IKKE_GODKJENT", "Unntaksperiode avvist av saksbehandler"),

    // Mottak av søknad
    MSA_OPPRETT_SAK_OG_BEHANDLING("MSA_OPPRETT_SAK_OG_BEHANDLING", "Opprett sak og behandling fra søknad fra Altinn"),
    MSA_OPPRETT_ARKIVSAK("MSA_OPPRETT_ARKIVSAK","Opprett arkivsak"),
    MSA_OPPRETT_OG_FERDIGSTILL_JOURNALPOST("MSA_OPPRETT_OG_FERDIGSTILL_JOURNALPOST", "Opprett og journalfør søknad fra Altinn"),
    MSA_HENT_REGISTEROPPLYSNINGER("MSA_HENT_REGISTEROPPLYSNINGER","Innhent registeropplysnigner"),
    MSA_VURDER_INNGANGSVILKÅR("MSA_VURDER_INNGANGSVILKÅR","Vurder inngangsvilkår"),
    MSA_OPPRETT_OPPGAVE("MSA_OPPRETT_OPPGAVE","Opprett oppgave for saksbehandling"),
    MSA_SEND_FORVALTNINGSMELDING("MSA_SEND_FORVALTNINGSMELDING","Send forvaltningsmelding"),

    FERDIG("FERDIG", "Prosessen er ferdig");


    private final String kode;
    private final String beskrivelse;

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

    public static ProsessSteg hentFørsteProsessStegForType(final ProsessType prosessType) throws TekniskException {
        switch (prosessType)  {
            case REGISTRERING_UNNTAK:
                return REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET;
            case ANMODNING_OM_UNNTAK_SVAR:
                return AOU_SVAR_OPPRETT_ANMODNINGSPERIODESVAR;
            case ANMODNING_OM_UNNTAK_MOTTAK:
                return AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE;
            case SED_GENERELL_SAK:
                return SED_GENERELL_SAK_HENT_PERSON;
            case ARBEID_FLERE_LAND:
                return AFL_SAK_OG_BEHANDLING_OPPRETTET;

            default:
                throw new TekniskException("Første steg for prosesstype" + prosessType + " er ukjent");
        }
    }
}
