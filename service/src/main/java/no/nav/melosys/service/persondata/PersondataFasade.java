package no.nav.melosys.service.persondata;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.*;

public interface PersondataFasade {
    String hentAktørIdForIdent(String ident) throws IkkeFunnetException;

    String hentFolkeregisterIdent(String ident) throws IkkeFunnetException;

    Saksopplysning hentPerson(String ident, Informasjonsbehov behov) throws IkkeFunnetException,
        IntegrasjonException, SikkerhetsbegrensningException;

    Saksopplysning hentPersonhistorikk(String ident, LocalDate dato) throws IkkeFunnetException,
        SikkerhetsbegrensningException, TekniskException;

    String hentSammensattNavn(String fnr) throws FunksjonellException, IntegrasjonException;

    boolean harStrengtFortroligAdresse(String fnr) throws IkkeFunnetException, IntegrasjonException,
        SikkerhetsbegrensningException;
}
