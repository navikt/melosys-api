package no.nav.melosys.domain;

public enum Dokumenttype implements Kodeverk {

    SOEKNAD_AVKL_LOV("SOEKNAD_AVKL_LOV", "Melding om at en søknad er mottatt"),
    SOEKNAD_OVERS_UTLMYNDH("SOEKNAD_OVERS_UTLMYNDH", "Melding om at en søknad er oversendt fra utenlandske myndigheter"),
    UTPEK_NO_UTLMYNDH("UTPEK_NO_UTLMYNDH", "Melding om at utenlandske myndigheter peker Norge ut som lovvalgsland"),
    PAASTAND_UTLMYNDH("PAASTAND_UTLMYNDH", "Melding om påstand fra utenlandske myndigheter"),
    MELDING_UTLMYNDH("MELDING_UTLMYNDH", "Melding om posting fra utenlandske myndigheter"),
    MELDING_AKTOER("MELDING_AKTOER", "Melding mottatt fra en aktør i saken"),
    KLAGE("KLAGE", "Klage"),
    ANKE("ANKE", "Anke"),
    POSTING_UTLMYNDH("POSTING_UTLMYNDH", "Melding om posting mottatt fra utenlandske myndigheter"),
    KLAGEVEDTAK("KLAGEVEDTAK", "Klagevedtak"),
    KJENNELSE_TRYGDERETTEN("KJENNELSE_TRYGDERETTEN", "Trygderettskjennelse");

    private String kode;
    private String beskrivelse;

    Dokumenttype(String kode, String beskrivelse) {
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
