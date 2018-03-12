package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum ProsessSteg implements Kodeverk<ProsessSteg> {

    // FIXME (farjam EESSI2-291): Venter på tilstandsmodellen
    A001_JOURF("A1_JOURF", "A001 journalføring"),
    A001_HENT_PERS_OPPL("A001_HENT_PERS_OPPL", "A001 hent personopplysninger"),
    A001_HENT_ARBF_OPPL("A001_HENT_ARBF_OPPL", "A001 hent arbeidsforhold"),
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
    public static class DbKonverterer extends Kodeverk.DbKonverterer<ProsessSteg> {
        @Override
        protected ProsessSteg[] getLovligeVerdier() {
            return ProsessSteg.values();
        }
    }

}
