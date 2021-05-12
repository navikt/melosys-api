package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;

public interface PDLConsumer {
    Identliste hentIdenter(String ident);
    Person hentPerson(String ident);
}
