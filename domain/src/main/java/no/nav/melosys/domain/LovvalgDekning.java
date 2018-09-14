package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum LovvalgDekning implements InterntKodeverkTabell<LovvalgDekning> {

    OMFATTET("OMFATTET", "Omfattet av Folketrygdloven"),
    IKKE_DEKKET("IKKE_DEKKET", "Ikke dekket i Folketrygden"),
    UNTATT("UNTATT", "Unttatt av Folketrygdloven");
    
    private String kode;
    private String beskrivelse;

    private LovvalgDekning(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<LovvalgDekning> {
        @Override
        protected LovvalgDekning[] getLovligeVerdier() {
            return LovvalgDekning.values();
        }
    }

}
