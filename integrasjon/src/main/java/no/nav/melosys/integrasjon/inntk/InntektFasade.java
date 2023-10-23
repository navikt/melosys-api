package no.nav.melosys.integrasjon.inntk;

import java.time.YearMonth;

import no.nav.melosys.domain.Saksopplysning;

public interface InntektFasade {
    Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom);
}
