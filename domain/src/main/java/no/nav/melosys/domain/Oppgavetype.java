package no.nav.melosys.domain;

public enum Oppgavetype implements Kodeverk {

    BEH_SAK("BEH_SAK", "Behandling"),
    JFR("JFR", "Journalføring");

    private String kode;
    private String beskrivelse;

    Oppgavetype(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

}
