package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum Behandlingsstatus implements InterntKodeverkTabell<Behandlingsstatus> {

    OPPRETTET("OPPRETTET", "Opprettet"),
    UNDER_BEHANDLING("UNDER_BEHANDLING", "Under behandling"),
    AVVENT_DOK_UTL("AVVENT_DOK_UTL", "Avventer dokumentasjon fra utlandet"),
    AVVENT_DOK_PART ("AVVENT_DOK_PART", "Avventer dokumentasjon fra en part"),
    AVSLUTTET("AVSLUTTET", "Avsluttet");
    
    private String kode;
    private String beskrivelse;

    Behandlingsstatus(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Behandlingsstatus> {
        @Override
        protected Behandlingsstatus[] getLovligeVerdier() {
            return Behandlingsstatus.values();
        }
    }

}
