package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum ProsessType implements InterntKodeverkTabell<ProsessType> {

    //alfabetisk rekkefølge
    IVERKSETT_VEDTAK("IVERKSETT_VEDTAK", "Iverksett vedtak"),
    JFR_KNYTT("JFR_KNYTT", "Journalføring på eksisterende sak"),
    JFR_NY_BEHANDLING("JFR_NY_BEHANDLING", "Journalføring på eksisterende sak oppretter en ny behandling"),
    JFR_NY_SAK("JFR_NY_SAK", "Journalføring med ny sak og søknad"),
    MANGELBREV("MANGELBREV", "Opprett mangelbrev"),
    MOTTAK("MOTTAK", "Journalføring av mottatt sak"),
    OPPFRISKNING("OPPFRISKNING", "Oppfriskning av saksopplysninger");

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
