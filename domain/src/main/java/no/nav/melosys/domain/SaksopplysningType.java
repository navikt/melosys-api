package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum SaksopplysningType implements KodeverkTabell<SaksopplysningType> {
    
    ARBEIDSFORHOLD("ARBFORH", "Arbeidsforhold"),
    INNTEKT("INNTK", "Inntekt"),
    MEDLEMSKAP("MEDL", "Medlemskap"),
    ORGANISASJON("ORG", "Arbeidsgiver"),
    PERSONOPPLYSNING("PERSOPL", "Personopplysning"),
    PERSONHISTORIKK("PERSHIST", "Personhistorikk"),
    SOB_SAK("SOB_SAK", "Sak og behandling-sak"),
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
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<SaksopplysningType> {
        @Override
        protected SaksopplysningType[] getLovligeVerdier() {
            return SaksopplysningType.values();
        }
    }

}

