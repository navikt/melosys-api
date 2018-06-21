package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum ProsessSteg implements KodeverkTabell<ProsessSteg> {

    // Disse skal være i logisk rekkefølge
    
    MOT_VURDER_AUTOMATISK_JFR("VURDER_AUTOMATISK_JFR", "Vurder om journalføring kan skje automatisk"),

    JFR_AVSLUTT_OPPGAVE("JFR_AVSLUTT_OPPGAVE", "Avslutter journalføringsoppgaven i GSAK"),
    JFR_AKTOER_ID("JFR_AKTOER_ID", "Henter aktørID"),
    JFR_OPPRETT_SAK_OG_BEH("JFR_OPPRETT_SAK_OG_BEH", "Oppretter ny sak og behandling i Melosys"),
    JFR_OPPRETT_GSAK_SAK("JFR_OPPRETT_GSAK_SAK", "Oppretter Sak i GSAK"),
    JFR_OPPDATER_JOURNALPOST("JFR_OPPDATER_JOURNALPOST", "Oppdaterer journalposten i Joark"),
    JFR_FERDIGSTILL_JOURNALPOST("JFR_FERDIGSTILL_JOURNALPOST", "Ferdigstiller journalposten i Joark"),
    JFR_HENT_PERS_OPPL("JFR_HENT_PERS_OPPL", "Hent personopplysninger fra TPS"),
    JFR_VURDER_INNGANGSVILKÅR("JFR_VURDER_INNGANGSVILKÅR", "Vurderer inngangsvilkår"),
    JFR_OPPRETT_OPPGAVE("JFR_OPPRETT_OPPGAVE", "Oppretter oppgave i GSAK"),

    HENT_ARBF_OPPL("HENT_ARBF_OPPL", "Hent arbeidsforholdopplysninger fra AAREG"),
    HENT_INNT_OPPL("HENT_INNT_OPPL", "Hent inntektopplysninger fra INNTK"),
    HENT_ORG_OPPL("HENT_ORG_OPPL", "Hent organisasjoner fra EREG"),
    HENT_MEDL_OPPL("HENT_MEDL_OPPL", "Hent medlemskapsopplysninger fra MEDL"),

    MANUELL_VURD("MANUELL_VURD", "Manuell vurdering"), // FIXME: Fjernes av MELOSYS-1338

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
