package no.nav.melosys.domain.begrunnelser;

import no.nav.melosys.domain.Kodeverk;

public enum IkkeSkip implements Kodeverk {

    IKKE_EGET_FREMDRIFT("IKKE_EGET_FREMDRIFT", "Ikke eget fremdrift"),
    IKKE_ORDINAERT_SKIPSFART("IKKE_ORDINAERT_SKIPSFART", "Ikke ordinært skipsfart");

    private String kode;
    private String beskrivelse;

    IkkeSkip(String kode, String beskrivelse) {
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

