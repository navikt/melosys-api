package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingStatus implements Kodeverk<BehandlingStatus> {

    // FIXME (farjam): Hva er riktige koder for disse?
    OPPRETTET("OPPR", "Opprettet"),
    UNDER_BEHANDLING("UBEH", "Under behandling"),
    FORELØPIG("FORL", "Foreløpig lovvalg"),
    AVSLUTTET("AVSLU", "Avsluttet");
    
    private String kode;
    private String beskrivelse;

    private BehandlingStatus(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends Kodeverk.DbKonverterer<BehandlingStatus> {
        @Override
        protected BehandlingStatus[] getLovligeVerdier() {
            return BehandlingStatus.values();
        }
    }

}
