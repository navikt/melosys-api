package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface TpsFasade {

    String hentAktørIdForIdent(String fnr) throws IkkeFunnetException;

    String hentIdentForAktørId(String aktørID) throws IkkeFunnetException;

    Saksopplysning hentPerson(String ident) throws IkkeFunnetException, SikkerhetsbegrensningException;

    Saksopplysning hentPersonMedAdresse(String ident) throws IkkeFunnetException, SikkerhetsbegrensningException;

    /**
     * @param aktørId tilsvarende til FNR
     * @return Antall personer som bor på samme bostedsadresse inkludert ident det spørres på.
     */
    int hentAntallPersonerSomBorPåBostedsadresse(String aktørId) throws IntegrasjonException;

    String hentStatsborgerskapPåGittDato(String ident, LocalDate dato) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException;
}
