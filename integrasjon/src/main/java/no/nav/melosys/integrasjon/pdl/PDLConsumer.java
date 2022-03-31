package no.nav.melosys.integrasjon.pdl;

import java.util.Collection;

import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap;
import org.springframework.retry.annotation.Retryable;

@Retryable
public interface PDLConsumer {
    Identliste hentIdenter(String ident);
    Person hentBarn(String ident);
    Person hentBarnMedHistorikk(String ident);
    Person hentForelder(String ident);
    Person hentForelderMedHistorikk(String ident);
    Person hentFamilierelasjoner(String ident);
    Person hentPerson(String ident);
    Person hentPersonMedHistorikk(String ident);
    Person hentRelatertVedSivilstand(String ident);
    Person hentRelatertVedSivilstandMedHistorikk(String ident);
    Collection<Adressebeskyttelse> hentAdressebeskyttelser(String ident);
    Collection<Navn> hentNavn(String fnr);
    Collection<Statsborgerskap> hentStatsborgerskap(String ident);
}
