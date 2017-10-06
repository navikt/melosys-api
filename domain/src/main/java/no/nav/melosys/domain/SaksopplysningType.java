package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum SaksopplysningType implements Kodeverk<SaksopplysningType> {
    
    ARBEIDSFORHOLD("ARBEIDSFORHOLD", "Arbeidsforhold"),
    ORGANISASJON("ORGANISASJON", "Arbeidsgiver"),
    PERSONOPPLYSNING("PERSONOPPLYSNING", "Personopplysning"),
    SØKNAD("SØKNAD", "Søknad");
    
    private String kode;
    private String beskrivelse;

    private SaksopplysningType(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends Kodeverk.DbKonverterer<SaksopplysningType> {
        @Override
        protected SaksopplysningType[] getLovligeVerdier() {
            return SaksopplysningType.values();
        }
    }

}

