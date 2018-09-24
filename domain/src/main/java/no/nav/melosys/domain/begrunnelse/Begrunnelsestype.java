package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

// FIXME Ikke definert som kodeverk
public enum Begrunnelsestype implements Kodeverk {

    IKKE_SKIP("IKKE_SKIP", "Ikke skip"),
    OPPHOLD("IKKE_ORDINAERT_SKIPSFART", "Ikke ordinært skipsfart");

    private String kode;
    private String beskrivelse;

    Begrunnelsestype(String kode, String beskrivelse) {
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
