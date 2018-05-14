package no.nav.melosys.domain.oppgave;

import no.nav.melosys.domain.Kodeverk;

public enum Oppgavetype implements Kodeverk {
    //FIXME: MELOSYS-1336: Skulle hente term(beskrivlse) fra felles kodeverk
    BEH_SAK("BEH_SAK", "Behandling"),
    JFR("JFR", "Journalføring");

    private final String kode;
    private final String beskrivelse;

    Oppgavetype(String kode, String beskrivelse) {
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
