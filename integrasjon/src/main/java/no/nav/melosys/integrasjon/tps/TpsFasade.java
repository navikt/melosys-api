package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.*;

public interface TpsFasade {
    String hentAktørIdForIdent(String fnr) throws IkkeFunnetException;

    String hentIdentForAktørId(String aktørID) throws IkkeFunnetException;

    Saksopplysning hentPerson(String ident, Informasjonsbehov behov) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException;

    /**
     * Henter all historikk fram til angitt dato (start av søknadsperioden).
     */
    Saksopplysning hentPersonhistorikk(String ident, LocalDate dato)
        throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException;

    String hentSammensattNavn(String fnr) throws FunksjonellException, IntegrasjonException;
}
