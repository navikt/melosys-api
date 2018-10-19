package no.nav.melosys.domain.dokument.medlemskap;

public enum DekningMedltype {
    //FIXME: MELOSYS-1336: Skulle hente term(beskrivlse) fra felles kodeverk
    Untatt("Untatt"),
    Full("Full");

    private String kode;

    DekningMedltype(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

}
