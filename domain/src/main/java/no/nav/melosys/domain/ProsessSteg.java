package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum ProsessSteg implements KodeverkTabell<ProsessSteg> {

    // Disse skal være i logisk rekkefølge
    
    // TODO (MELOSYS-13909: Rekkefølgen må revideres. JFR_AVSLUTT_OPPGAVE bør kanskje komme før JFR_VALIDERING. 
    
    MOT_VURDER_AUTOMATISK_JFR("VURDER_AUTOMATISK_JFR", "Vurder om journalføring kan skje automatisk"), // FIXMWE: Ikke i bruk

    // Journalføring
    JFR_VALIDERING("JFR_VALIDERING", "Grunnleggende validering"),
    JFR_AVSLUTT_OPPGAVE("JFR_AVSLUTT_OPPGAVE", "Avslutter journalføringsoppgaven i GSAK"),
    JFR_AKTØR_ID("JFR_AKTOER_ID", "Henter aktørID"),
    JFR_OPPRETT_SAK_OG_BEH("JFR_OPPRETT_SAK_OG_BEH", "Oppretter ny sak og behandling i Melosys"),
    JFR_OPPRETT_GSAK_SAK("JFR_OPPRETT_GSAK_SAK", "Oppretter Sak i GSAK"),
    STATUS_BEH_OPPR("STATUS_BEH_OPPR", "Oppdater Sak og Behandling ved oppretting av behandling"),
    JFR_OPPDATER_JOURNALPOST("JFR_OPPDATER_JOURNALPOST", "Oppdaterer journalposten i Joark"),
    JFR_FERDIGSTILL_JOURNALPOST("JFR_FERDIGSTILL_JOURNALPOST", "Ferdigstiller journalposten i Joark"),
    JFR_HENT_PERS_OPPL("JFR_HENT_PERS_OPPL", "Hent personopplysninger fra TPS"),
    JFR_VURDER_INNGANGSVILKÅR("JFR_VURDER_INNGANGSVILKÅR", "Vurderer inngangsvilkår"),

    // Hent saksopplysninger
    HENT_ARBF_OPPL("HENT_ARBF_OPPL", "Hent arbeidsforholdopplysninger fra AAREG"),
    HENT_INNT_OPPL("HENT_INNT_OPPL", "Hent inntektopplysninger fra INNTK"),
    HENT_ORG_OPPL("HENT_ORG_OPPL", "Hent organisasjoner fra EREG"),
    HENT_MEDL_OPPL("HENT_MEDL_OPPL", "Hent medlemskapsopplysninger fra MEDL"),
    OPPRETT_OPPGAVE("OPPRETT_OPPGAVE", "Oppretter oppgave i GSAK"),

    FEILET_MASKINELT("FEILET_MASKINELT", "Feilet maskinelt"),

    // FIXME: Prosessinstansen må få steg FATTET_VEDTAK når vedtak er fattet slik at status oppdateres i Sak og Behandling.
    FATTET_VEDTAK("FATTET_VEDTAK", "Saksbehandler har fattet vedtak i Melosys"),
    STATUS_BEH_AVSL("STATUS_BEH_AVSL", "Oppdater Sak og Behandling ved lukking av behandling");

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
