package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum Behandlingsstatus implements InterntKodeverkTabell<Behandlingsstatus> {

    OPPRETTET("OPPRETTET", "Behandlingen er opprettet"),
    UNDER_BEHANDLING("UNDER_BEHANDLING", "Behandlingen pågår"),
    AVVENT_DOK_UTL("AVVENT_DOK_UTL", "Avventer svar fra utenlandsk trygdemyndighet"),
    AVVENT_DOK_PART ("AVVENT_DOK_PART", "Avventer svar fra part i saken"),
    TIDSFRIST_UTLOEPT("TIDSFRIST_UTLOEPT", "Tidsfristen er utløpt på etterspurte opplysninger"),
    VURDER_DOKUMENT("VURDER_DOKUMENT", "Vurder dokument"),
    FORELOEPIG_LOVVALG("FORELOEPIG_LOVVALG", "Avventer svar på foreløpig lovvalg"),
    ANMODNING_UNNTAK_SENDT("ANMODNING_UNNTAK_SENDT", "Anmodning om unntak er sendt"),
    IVERKSETTER_VEDTAK("IVERKSETTER_VEDTAK", "Vedtak iverksettes"),
    AVSLUTTET("AVSLUTTET", "Behandlingen er avsluttet");
    
    private String kode;
    private String beskrivelse;

    Behandlingsstatus(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Behandlingsstatus> {
        @Override
        protected Behandlingsstatus[] getLovligeVerdier() {
            return Behandlingsstatus.values();
        }
    }

    public boolean erLovligNesteStatusEtterDokumentVurdering() {
        return (this == Behandlingsstatus.UNDER_BEHANDLING)
            || (this == Behandlingsstatus.AVVENT_DOK_PART)
            || (this == Behandlingsstatus.AVVENT_DOK_UTL);
    }

    public static boolean erVenterForDokumentasjon(Behandlingsstatus behandlingsstatus) {
        return (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_PART) || (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_UTL);
    }

}
