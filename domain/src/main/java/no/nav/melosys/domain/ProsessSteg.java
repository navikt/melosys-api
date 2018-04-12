package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum ProsessSteg implements KodeverkTabell<ProsessSteg> {

    A1_JOURF("A1_JOURF", "A1 journalføring"),
    A1_HENT_PERS_OPPL("A1_HENT_PERS_OPPL", "A1 hent personopplysninger"),
    A1_HENT_ARBF_OPPL("A1_HENT_ARBF_OPPL", "A1 hent arbeidsforhold"),
    FEILET_MASKINELT("FEILET_MASKINELT", "Feilet maskinelt");

    private String kode;
    private String beskrivelse;

    private ProsessSteg(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<ProsessSteg> {
        @Override
        protected ProsessSteg[] getLovligeVerdier() {
            return ProsessSteg.values();
        }
    }

}
