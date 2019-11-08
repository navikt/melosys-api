package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.*;

public interface TpsFasade {

    String hentAktørIdForIdent(String fnr) throws IkkeFunnetException;

    String hentIdentForAktørId(String aktørID) throws IkkeFunnetException;

    Saksopplysning hentPerson(String ident) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException;

    Saksopplysning hentPersonMedAdresse(String ident) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException;

    Saksopplysning hentPersonhistorikk(String ident, LocalDate dato) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException;

    /**
     */

    String hentSammensattNavn(String fnr) throws FunksjonellException, IntegrasjonException;
}
