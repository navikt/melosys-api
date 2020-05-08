package no.nav.melosys.integrasjon.inntk;

import java.time.YearMonth;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;

public interface InntektFasade {

    Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom) throws IntegrasjonException, FunksjonellException;
}
