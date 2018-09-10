package no.nav.melosys.domain;

public enum Finansiering implements Kodeverk {

    LAANEKASSEN("LAANEKASSEN", "Lånekassen"),
    UTENLANDSK_INSTITUSJON("UTENLANDSK_INSTITUSJON", "Utenlandsk institusjon"),
    EGNE_MIDLER("EGNE_MIDLER", "Egne midler / arv / etc");

    private String kode;
    private String beskrivelse;

    Finansiering(String kode, String beskrivelse) {
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
}
