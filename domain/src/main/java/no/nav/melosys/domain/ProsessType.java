package no.nav.melosys.domain;

import javax.persistence.Converter;

import no.nav.melosys.domain.kodeverk.InterntKodeverkTabell;

public enum ProsessType implements InterntKodeverkTabell<ProsessType> {

    //alfabetisk rekkefølge
    ANMODNING_OM_UNNTAK("ANMODNING_OM_UNNTAK", "Anmodning om unntak"),
    HENLEGG_SAK("HENLEGG_SAK", "Henlegg en sak"),
    IVERKSETT_VEDTAK("IVERKSETT_VEDTAK", "Iverksett vedtak"),
    IVERKSETT_VEDTAK_FORKORT_PERIODE("IVERKSETT_VEDTAK_FORKORT_PERIODE", "Iverksett nytt vedtak etter lovvalgsperioden har blitt forkortet"),
    JFR_KNYTT("JFR_KNYTT", "Journalføring på eksisterende sak"),
    JFR_NY_BEHANDLING("JFR_NY_BEHANDLING", "Journalføring på eksisterende sak oppretter en ny behandling"),
    JFR_NY_SAK("JFR_NY_SAK", "Journalføring med ny sak og søknad"),
    MANGELBREV("MANGELBREV", "Opprett mangelbrev"),
    OPPFRISKNING("OPPFRISKNING", "Oppfriskning av saksopplysninger"),
    REGISTRERING_UNNTAK("REGISTRERING_UNNTAK", "Registrering av unntak");

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

    @Converter
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<ProsessType> {
        @Override
        protected ProsessType[] getLovligeVerdier() {
            return ProsessType.values();
        }
    }

}
