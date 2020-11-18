package no.nav.melosys.domain.saksflyt;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum ProsessType implements Kodeverk {

    //alfabetisk rekkefølge
    ANMODNING_OM_UNNTAK("ANMODNING_OM_UNNTAK", "Anmodning om unntak"),
    ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK("ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK", "Mottar anmodning om unntak - ny sak"),
    ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING("ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING", "Mottar anmodning om unntak - ny behandling"),
    ANMODNING_OM_UNNTAK_MOTTAK_SVAR("ANMODNING_OM_UNNTAK_MOTTAK_SVAR", "Svar på mottatt anmodning om unntak"),
    ANMODNING_OM_UNNTAK_SVAR("ANMODNING_OM_UNNTAK_SVAR", "Mottar svar på anmodning om unntak"),
    ARBEID_FLERE_LAND_NY_SAK("ARBEID_FLERE_LAND_NY_SAK","Mottak av A003 - ny sak"),
    ARBEID_FLERE_LAND_NY_BEHANDLING("ARBEID_FLERE_LAND_NY_BEHANDLING","Mottak av A003 - ny behandling"),
    DISTRIBUER_JOURNALPOST("DISTRIBUER_JOURNALPOST", "Distribuer journalpost"),
    FORVALTNINGSMELDING_SEND("FORVALTNINGSMELDING_SEND", "Sender forvaltningsmelding"),
    HENLEGG_SAK("HENLEGG_SAK", "Henlegg en sak"),
    IVERKSETT_VEDTAK("IVERKSETT_VEDTAK", "Iverksett vedtak"),
    IVERKSETT_VEDTAK_FORKORT_PERIODE("IVERKSETT_VEDTAK_FORKORT_PERIODE", "Iverksett nytt vedtak etter lovvalgsperioden har blitt forkortet"),
    JFR_KNYTT("JFR_KNYTT", "Journalføring på eksisterende sak"),
    JFR_NY_BEHANDLING("JFR_NY_BEHANDLING", "Journalføring på eksisterende sak oppretter en ny behandling"),
    JFR_NY_SAK("JFR_NY_SAK", "Journalføring med ny sak og søknad"),
    MANGELBREV("MANGELBREV", "Opprett mangelbrev"),
    MOTTAK_SED("MOTTAK_SED", "Mottak av SED for journalføring og videre ruting"),
    MOTTAK_SED_JOURNALFØRING("MOTTAK_SED_JOURNALFØRING", "Mottak av SED som kun skal journalføres"),
    MOTTAK_SOKNAD_ALTINN("MOTTAK_SOKNAD_ALTINN", "Mottak av elektronisk søknad fra altinn"),
    OPPRETT_NY_SAK("OPPRETT_NY_SAK", "Oppretter ny sak (fra journalført dokument)"),
    REGISTRERING_UNNTAK_GODKJENN("REGISTRERING_UNNTAK_GODKJENN", "Godkjenner en untaksperiode og avslutter behandling"),
    REGISTRERING_UNNTAK_AVVIS("REGISTRERING_UNNTAK_AVVIS", "Avviser en untaksperiode og avslutter behandling"),
    REGISTRERING_UNNTAK_NY_SAK("REGISTRERING_UNNTAK_NY_SAK", "Registrering av unntak - ny sak"),
    REGISTRERING_UNNTAK_NY_BEHANDLING("REGISTRERING_UNNTAK_NY_BEHANDLING", "Registrering av unntak - ny behandling"),
    UTPEKING_AVVIS("UTPEKING_AVVIS", "Avviser utpeking mottatt i en A003"),
    VIDERESEND_SOKNAD("VIDERESEND_SOKNAD", "Videresend søknad"),
    OPPRETT_OG_JOURNALFØR_FORVALTNINGSMELDING("OPPRETT_OG_DISTRIBUER_JOURNALPOST", "Opprett, journalfør og distribuer forvaltningsmelding");

    private String kode;
    private String beskrivelse;

    ProsessType(String kode, String beskrivelse) {
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
