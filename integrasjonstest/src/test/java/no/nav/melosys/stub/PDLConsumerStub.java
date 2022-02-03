package no.nav.melosys.stub;

import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap;

import java.util.Collection;

public class PDLConsumerStub implements PDLConsumer {

    @Override
    public Identliste hentIdenter(String ident) {
        return null;
    }

    @Override
    public Person hentBarn(String ident) {
        return null;
    }

    @Override
    public Person hentBarnMedHistorikk(String ident) {
        return null;
    }

    @Override
    public Person hentForelder(String ident) {
        return null;
    }

    @Override
    public Person hentForelderMedHistorikk(String ident) {
        return null;
    }

    @Override
    public Person hentFamilierelasjoner(String ident) {
        return null;
    }

    @Override
    public Person hentPerson(String ident) {
        return null;
    }

    @Override
    public Person hentPersonMedHistorikk(String ident) {
        return null;
    }

    @Override
    public Person hentRelatertVedSivilstand(String ident) {
        return null;
    }

    @Override
    public Person hentRelatertVedSivilstandMedHistorikk(String ident) {
        return null;
    }

    @Override
    public Collection<Adressebeskyttelse> hentAdressebeskyttelser(String ident) {
        return null;
    }

    @Override
    public Collection<Navn> hentNavn(String fnr) {
        return null;
    }

    @Override
    public Collection<Statsborgerskap> hentStatsborgerskap(String ident) {
        return null;
    }
}
