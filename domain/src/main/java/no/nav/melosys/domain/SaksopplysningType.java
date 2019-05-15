package no.nav.melosys.domain;

import javax.persistence.Converter;

import no.nav.melosys.domain.kodeverk.InterntKodeverkTabell;

public enum SaksopplysningType implements InterntKodeverkTabell<SaksopplysningType> {

    ARBFORH("ARBFORH", "Arbeidsforhold"),
    INNTK("INNTK", "Inntekt"),
    MEDL("MEDL", "Medlemskap"),
    ORG("ORG", "Arbeidsgiver"),
    PERSHIST("PERSHIST", "Personhistorikk"),
    PERSOPL("PERSOPL", "Personopplysning"),
    SEDOPPL("SEDOPPL", "SED-opplysninger"),
    SOB_SAK("SOB_SAK", "Sak og behandling-sak"),
    SØKNAD("SØKNAD", "Søknad");

    private String kode;
    private String beskrivelse;

    SaksopplysningType(String kode, String beskrivelse) {
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
    public static class DbConverter extends InterntKodeverkTabell.DbKonverterer<SaksopplysningType> {
        @Override
        protected SaksopplysningType[] getLovligeVerdier() {
            return SaksopplysningType.values();
        }
    }

}

