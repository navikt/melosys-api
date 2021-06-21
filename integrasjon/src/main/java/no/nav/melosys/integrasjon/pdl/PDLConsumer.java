package no.nav.melosys.integrasjon.pdl;

import java.util.Collection;

import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap;

public interface PDLConsumer {
    Identliste hentIdenter(String ident);
    Person hentPerson(String ident);
    Collection<Adressebeskyttelse> hentAdressebeskyttelser(String ident);
    Collection<Navn> hentNavn(String fnr);
    Collection<Statsborgerskap> hentStatsborgerskap(String ident);
}
