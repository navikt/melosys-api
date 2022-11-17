package no.nav.melosys.domain.mottatteopplysninger.data;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

public class Bosted {
    public Boolean intensjonOmRetur;
    public int antallMaanederINorge;
    public StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
}
