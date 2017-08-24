package no.nav.melosys.domain;

import javax.persistence.Converter;

//FIXME (farjam): Ikke revidert for v0

public enum VilkaarsResultatUtfallType implements Kodeverk<VilkaarsResultatUtfallType> {

    OPPFYLT("OPPFYLT"),
    IKKE_OPPFYLT("IKKE_OPPFYLT");

    private String kode;

    private VilkaarsResultatUtfallType(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<VilkaarsResultatUtfallType> {
        @Override
        protected VilkaarsResultatUtfallType[] getLovligeVerdier() {
            return VilkaarsResultatUtfallType.values();
        }
    }

}
