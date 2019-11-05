package no.nav.melosys.domain.saksflyt;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum ProsessType implements Kodeverk {

    //alfabetisk rekkefølge
    ANMODNING_OM_UNNTAK("ANMODNING_OM_UNNTAK", "Anmodning om unntak"),
    ANMODNING_OM_UNNTAK_MOTTAK("ANMODNING_OM_UNNTAK_MOTTAK", "Mottar anmodning om unntak"),
    ANMODNING_OM_UNNTAK_MOTTAK_SVAR("ANMODNING_OM_UNNTAK_MOTTAK_SVAR", "Svar på mottatt anmodning om unntak"),
    ANMODNING_OM_UNNTAK_SVAR("ANMODNING_OM_UNNTAK_SVAR", "Mottar svar på anmodning om unntak"),
    FORVALTNINGSMELDING_SEND("FORVALTNINGSMELDING_SEND", "Sender forvaltningsmelding"),
    HENLEGG_SAK("HENLEGG_SAK", "Henlegg en sak"),
    IVERKSETT_VEDTAK("IVERKSETT_VEDTAK", "Iverksett vedtak"),
    IVERKSETT_VEDTAK_FORKORT_PERIODE("IVERKSETT_VEDTAK_FORKORT_PERIODE", "Iverksett nytt vedtak etter lovvalgsperioden har blitt forkortet"),
    JFR_AOU_BREV("JFR_AOU_BREV", "Journalføring av mottatt anmodning om unntak (brev)"),
    JFR_KNYTT("JFR_KNYTT", "Journalføring på eksisterende sak"),
    JFR_NY_BEHANDLING("JFR_NY_BEHANDLING", "Journalføring på eksisterende sak oppretter en ny behandling"),
    JFR_NY_SAK("JFR_NY_SAK", "Journalføring med ny sak og søknad"),
    MANGELBREV("MANGELBREV", "Opprett mangelbrev"),
    MOTTAK_SED("MOTTAK_SED", "Mottak av SED for journalføring og videre ruting"),
    MOTTAK_SED_JOURNALFØRING("MOTTAK_SED_JOURNALFØRING", "Mottak av SED som kun skal journalføres"),
    OPPFRISKNING("OPPFRISKNING", "Oppfriskning av saksopplysninger"),
    REGISTRERING_UNNTAK("REGISTRERING_UNNTAK", "Registrering av unntak"),
    SED_GENERELL_SAK("SED_OPPRETT_SAK", "Oppretter sak for SED for generell behandling"),
    VIDERESEND_SOKNAD("VIDERESEND_SOKNAD", "Videresend søknad");

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
