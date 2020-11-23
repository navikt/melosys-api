package no.nav.melosys.integrasjon.tps.mapper;

import no.nav.melosys.domain.dokument.person.PersonDokument;

public class PersonMedKilde {
    public PersonDokument dokument;
    public String dokumentXml;

    public PersonMedKilde(PersonDokument dokument, String dokumentXml) {
        this.dokument = dokument;
        this.dokumentXml = dokumentXml;
    }
}
