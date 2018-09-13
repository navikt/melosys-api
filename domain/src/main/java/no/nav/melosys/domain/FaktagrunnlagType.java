package no.nav.melosys.domain;

import javax.persistence.Converter;

/**
 * Dette kodeverket angir hvilket felter fra en saksopplysning som er lagt til grunn for behandlingen.
 * FIXME: Dette enumet slettes hvis SaksopplysningType gir oss det vi trenger
 */
public enum FaktagrunnlagType implements InterntKodeverkTabell<FaktagrunnlagType> {

    PERSONOPPLYSNING("PERSONOPPLYSNING", "Personopplysning");

    private String kode;
    private String beskrivelse;

    private FaktagrunnlagType(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<FaktagrunnlagType> {
        @Override
        protected FaktagrunnlagType[] getLovligeVerdier() {
            return FaktagrunnlagType.values();
        }
    }

}