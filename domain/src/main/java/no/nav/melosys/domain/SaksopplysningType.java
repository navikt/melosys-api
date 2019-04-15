package no.nav.melosys.domain;

import javax.persistence.Converter;

import no.nav.melosys.domain.kodeverk.InterntKodeverkTabell;

public enum SaksopplysningType implements InterntKodeverkTabell<SaksopplysningType> {
    
    ARBEIDSFORHOLD("ARBFORH", "Arbeidsforhold"),
    INNTEKT("INNTK", "Inntekt"),
    MEDLEMSKAP("MEDL", "Medlemskap"),
    ORGANISASJON("ORG", "Arbeidsgiver"),
    PERSONHISTORIKK("PERSHIST", "Personhistorikk"),
    PERSONOPPLYSNING("PERSOPL", "Personopplysning"),
    SED_OPPLYSNINGER("SEDOPPL", "SED-opplysninger"),
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<SaksopplysningType> {
        @Override
        protected SaksopplysningType[] getLovligeVerdier() {
            return SaksopplysningType.values();
        }
    }

}

