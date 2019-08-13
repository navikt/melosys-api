package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.*;

public interface MedlFasade {

    Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException;

    Long opprettPeriodeEndelig(String fnr, Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException;

    Long opprettPeriodeUnderAvklaring(String fnr, Medlemskapsperiode periodeMedBestemmelse, KildedokumenttypeMedl kildedokumenttypeMedl) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException;

    void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, KildedokumenttypeMedl kildedokumenttypeMedl) throws FunksjonellException, TekniskException;

    void avvisPeriode(Long medlPeriodeID, StatusaarsakMedl årsak) throws SikkerhetsbegrensningException, IkkeFunnetException;
}
