package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum BehandlingStatus implements Kodeverk<BehandlingStatus> {

    OPPRETTET("OPPR"),
    KLARGJORT("KLAR"),
    UTREDES("UTRED"),
    FATTER_VEDTAK("F_VED"),
    IVERKSETTER_VEDTAK("I_VED"),
    AVSLUTTET("AVSLU");
    
    private String kode;

    private BehandlingStatus(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<BehandlingStatus> {
        @Override
        protected BehandlingStatus[] getLovligeVerdier() {
            return BehandlingStatus.values();
        }
    }

}
