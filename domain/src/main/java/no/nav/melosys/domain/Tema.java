package no.nav.melosys.domain;

//TODO: Tema bør endres til Fagområde hvis det er det foretrukne fagbegrepet.
public enum Tema implements Kodeverk {
    //FIXME: MELOSYS-1336: Skulle hente term(beskrivlse) fra felles kodeverk
    MED("MED", "Medlemskap"),
    UFM("UFM", "Unntak fra MED");

    private final String kode;
    private final String beskrivelse;

    Tema(String kode, String beskrivelse) {
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
