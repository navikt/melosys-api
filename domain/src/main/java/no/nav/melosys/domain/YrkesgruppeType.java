package no.nav.melosys.domain;

public enum YrkesgruppeType implements Kodeverk {

    ORDINAER("ORDINAER", "Ordinær arbeid som ikke gjelder flyende personell eller på sokkel eller skip."),
    FLYENDE_PERSONELL("FLYENDE_PERSONELL", "Arbeider som flyende personell."),
    SOKKEL_ELLER_SKIP("SOKKEL_ELLER_SKIP", "Arbeider på sokkel eller skip.");

    private String kode;
    private String beskrivelse;

    YrkesgruppeType(String kode, String beskrivelse) {
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
