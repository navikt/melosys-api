package no.nav.melosys.saksflytapi.domain;


public enum ProsessType {
    //alfabetisk rekkefølge
    ANMODNING_OM_UNNTAK("ANMODNING_OM_UNNTAK", "Anmodning om unntak"),
    ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING("ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING", "Mottar anmodning om unntak - ny behandling"),
    ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK("ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK", "Mottar anmodning om unntak - ny sak"),
    ANMODNING_OM_UNNTAK_MOTTAK_SVAR("ANMODNING_OM_UNNTAK_MOTTAK_SVAR", "Svar på mottatt anmodning om unntak"),
    ANMODNING_OM_UNNTAK_SVAR("ANMODNING_OM_UNNTAK_SVAR", "Mottar svar på anmodning om unntak"),
    ANNULLER_SAK("ANNULLER_SAK", "Annullerer en sak, fjerner medl perioder og kansellerer faktura"),
    ARBEID_FLERE_LAND_NY_BEHANDLING("ARBEID_FLERE_LAND_NY_BEHANDLING", "Mottak av A003 - ny behandling"),
    ARBEID_FLERE_LAND_NY_SAK("ARBEID_FLERE_LAND_NY_SAK", "Mottak av A003 - ny sak"),
    HENLEGG_SAK("HENLEGG_SAK", "Henlegg en sak"),
    // IVERKSETT_EOS_PENSJONIST_AVGIFT og IVERKSETT_VEDTAK_AARSAVREGNING beholdes bevisst på NORMAL (ikke HØY):
    // dette er avgifts-/årsavregning-iverksettelse, ikke saksbehandler-trigget lovvalgs-/folketrygd-vedtak. Et
    // manuelt fattet årsavregning-vedtak går også gjennom IVERKSETT_VEDTAK_AARSAVREGNING; trengs HØY i et enkelttilfelle,
    // settes det per kall (ProsessinstansService.opprett...(prioritet)). Batch-presset ligger på OPPRETT_NY_BEHANDLING_AARSAVREGNING (LAV).
    IVERKSETT_EOS_PENSJONIST_AVGIFT("IVERKSETT_EOS_PENSJONIST_AVGIFT", "Iverksett EØS pensjonist avgift"),
    IVERKSETT_VEDTAK_AARSAVREGNING("IVERKSETT_VEDTAK_AARSAVREGNING", "Iverksett vedtak for en årsavregning"),
    IVERKSETT_VEDTAK_EOS("IVERKSETT_VEDTAK_EOS", "Iverksett vedtak EOS", ProsessPrioritet.HØY),
    IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE("IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE", "Iverksett nytt vedtak etter lovvalgsperioden har blitt forkortet", ProsessPrioritet.HØY),
    IVERKSETT_VEDTAK_FTRL("IVERKSETT_VEDTAK_FTRL", "Iverksett vedtak Folketrygdloven", ProsessPrioritet.HØY),
    IVERKSETT_VEDTAK_IKKE_YRKESAKTIV("IVERKSETT_VEDTAK_IKKE_YRKESAKTIV", "Iverksett vedtak Ikke yrkesaktiv", ProsessPrioritet.HØY),
    IVERKSETT_VEDTAK_TRYGDEAVTALE("IVERKSETT_VEDTAK_TRYGDEAVTALE", "Iverksett vedtak Trygdeavtale", ProsessPrioritet.HØY),
    JFR_ANDREGANG_NY_BEHANDLING("JFR_ANDREGANG_NY_BEHANDLING", "Journalføring på eksisterende sak og opprett ny behandling", ProsessPrioritet.HØY),
    JFR_ANDREGANG_REPLIKER_BEHANDLING("JFR_ANDREGANG_REPLIKER_BEHANDLING", "Journalføring på eksisterende sak og repliker tidligere behandling", ProsessPrioritet.HØY),
    JFR_KNYTT("JFR_KNYTT", "Journalføring på eksisterende sak", ProsessPrioritet.HØY),
    JFR_NY_SAK_BRUKER("JFR_NY_SAK_BRUKER", "Journalføring med ny sak og søknad for bruker", ProsessPrioritet.HØY),
    JFR_NY_SAK_VIRKSOMHET("JFR_NY_SAK_VIRKSOMHET", "Journalføring med ny sak for virksomhet", ProsessPrioritet.HØY),
    MANGLENDE_INNBETALING_VARSELBREV("MANGLENDE_INNBETALING_VARSELBREV","Send brev om manglende innbetaling"),
    MOTTAK_SED("MOTTAK_SED", "Mottak av SED for journalføring og videre ruting"),
    MOTTAK_SED_JOURNALFØRING("MOTTAK_SED_JOURNALFØRING", "Mottak av SED som kun skal journalføres"),
    MOTTAK_SOKNAD_ALTINN("MOTTAK_SOKNAD_ALTINN", "Mottak av elektronisk søknad fra altinn"),
    MELOSYS_MOTTAK_DIGITAL_SØKNAD("MELOSYS_MOTTAK_DIGITAL_SØKNAD", "Mottak av digital søknad"),
    MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD("MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD", "Mottak av digital søknad for eksisterende sak"),
    OPPDATER_FAKTURAMOTTAKER("OPPDATER_FAKTURAMOTTAKER", "Oppdaterer fakturamottaker i faktureringskomponent"),
    OPPRETT_NY_BEHANDLING_AARSAVREGNING("OPPRETT_NY_BEHANDLING_ARSAVREGNING", "Oppretter årsavregningbehandling på aktuell bruker", ProsessPrioritet.LAV),
    OPPRETT_NY_BEHANDLING_FOR_SAK("OPPRETT_NY_BEHANDLING_FOR_SAK", "Oppretter ny behandling og ny oppgave for eksisterende sak"),
    OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING("OPPRETT_NY_BEHANDLING_FTRL_MANGLENDE_INNBETALING", "Oppretter ny behandling og send varselbrev om manglende innbetaling"),
    OPPRETT_NY_SAK_EOS_FRA_OPPGAVE("OPPRETT_NY_SAK_EOS_FRA_OPPGAVE", "Oppretter ny sak (fra journalført dokument) for EU/EØS"),
    OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE("OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE", "Oppretter ny sak (fra journalført dokument) for FTRL og Trygdeavtale"),
    OPPRETT_OG_DISTRIBUER_BREV("OPPRETT_OG_DISTRIBUER_BREV", "Opprett, journalfør og distribuer brev"),
    OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK("OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK", "Oppretter ny replikert behandling og ny oppgave for eksisterende sak"),
    OPPRETT_SAK("OPPRETT_SAK", "Oppretter ny sak og ny oppgave"),
    REGISTRERE_UNNTAK_FRA_MEDLEMSKAP("REGISTRERE_UNNTAK_FRA_MEDLEMSKAP", "Registrere unntak fra medlemskap"),
    REGISTRERING_UNNTAK_AVVIS("REGISTRERING_UNNTAK_AVVIS", "Avviser en untaksperiode og avslutter behandling"),
    REGISTRERING_UNNTAK_GODKJENN("REGISTRERING_UNNTAK_GODKJENN", "Godkjenner en untaksperiode og avslutter behandling"),
    REGISTRERING_UNNTAK_NY_BEHANDLING("REGISTRERING_UNNTAK_NY_BEHANDLING", "Registrering av unntak - ny behandling"),
    REGISTRERING_UNNTAK_NY_SAK("REGISTRERING_UNNTAK_NY_SAK", "Registrering av unntak - ny sak"),
    SATSENDRING("SATSENDRING", "Oppretter og behandler en satsendring - iverksetter og fakturerer", ProsessPrioritet.LAV),
    SATSENDRING_TILBAKESTILL_NY_VURDERING("SATSENDRING_TILBAKESTILL_NY_VURDERING", "Behandler satsendring hvor aktiv ny vurdering tilbakestilles, slik at nye satser brukes", ProsessPrioritet.LAV),
    SEND_BREV("SEND_BREV", "Send brev til én mottaker via doksys"),
    UTPEKING_AVVIS("UTPEKING_AVVIS", "Avviser utpeking mottatt i en A003"),
    VIDERESEND_SOKNAD("VIDERESEND_SOKNAD", "Videresend søknad");

    private final String kode;
    private final String beskrivelse;
    private final ProsessPrioritet prioritet;

    ProsessType(String kode, String beskrivelse) {
        this(kode, beskrivelse, ProsessPrioritet.NORMAL);
    }

    ProsessType(String kode, String beskrivelse, ProsessPrioritet prioritet) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
        this.prioritet = prioritet;
    }

    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    /**
     * Default-prioritet for prosesstypen. Kan overstyres per kall ved opprettelse
     * (se {@code ProsessinstansService} og {@link Prosessinstans#hentPrioritet()}).
     */
    public ProsessPrioritet getPrioritet() {
        return prioritet;
    }
}
