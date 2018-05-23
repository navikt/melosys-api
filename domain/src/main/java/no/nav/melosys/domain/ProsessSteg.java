package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum ProsessSteg implements KodeverkTabell<ProsessSteg> {

    JFR_VURDER_SAKSFLYT("JFR_VURDER_SAKSFLYT", "Vurder om journalføring kan skje automatisk"),
    JFR_AKTOER_ID("JFR_AKTOER_ID", "Henter aktørID"),
    JFR_OPPRETT_SAK("JFR_OPPRETT_SAK", "Oppretter ny sak i Melosys"),
    JFR_OPPRETT_GSAK_SAK("JFR_OPPRETT_GSAK_SAK", "Oppretter Sak i GSAK"),
    JFR_OPPDATER_JOURNALPOST("JFR_OPPDATER_JOURNALPOST", "Oppdaterer journalposten i Joark"),
    JFR_FERDIGSTILL_JOURNALPOST("JFR_FERDIGSTILL_JOURNALPOST", "Ferdigstiller journalposten i Joark"),
    JFR_AVSLUTT_OPPGAVE("JFR_AVSLUTT_OPPGAVE", "Avslutter journalføringsoppgaven i GSAK"),
    OPPRETT_OPPGAVE("OPPRETT_OPPGAVE", "Oppretter oppgave i GSAK"),
    A1_HENT_PERS_OPPL("A1_HENT_PERS_OPPL", "A1 hent personopplysninger"),
    A1_HENT_ARBF_OPPL("A1_HENT_ARBF_OPPL", "A1 hent arbeidsforhold"),
    FEILET_MASKINELT("FEILET_MASKINELT", "Feilet maskinelt"),
    FERDIG("FERDIG", "Ferdig");

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
