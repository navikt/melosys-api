package no.nav.melosys.domain.begrunnelse;

import no.nav.melosys.domain.Kodeverk;

public enum NormaltDriverVirksomhet implements Kodeverk {

    IKKE_FORUTGAAENDE_DRIFT("IKKE_FORUTGAAENDE_DRIFT", "Ikke tilstrekkelig forutgående drift"),
    HAR_IKKE_NØDVENDIG_INFRASTRUKTUR("HAR_IKKE_NØDVENDIG_INFRASTRUKTUR", "Ikke nødvendig utstyr eller fasiliteter"),
    OPPRETTHOLDER_IKKE_LISENSER_AUTORISASJON("OPPRETTHOLDER_IKKE_LISENSER_AUTORISASJON", "Opprettholder ikke nødvendige lisenser eller autorisasjoner");

    private String kode;
    private String beskrivelse;

    NormaltDriverVirksomhet(String kode, String beskrivelse) {
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
