package no.nav.melosys.integrasjon.inntk;

import java.time.YearMonth;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface InntektFasade {

    String FILTER = "MedlemskapA-inntekt";
    String FILTER_URI = "http://nav.no/kodeverk/Kode/A-inntektsfilter/MedlemskapA-inntekt?v=6";

    String FORMAALSKODE = "Medlemskap";
    String FORMAALSKODE_URI = "http://nav.no/kodeverk/Kode/Formaal/Medlemskap?v=5";

    Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom) throws IntegrasjonException, SikkerhetsbegrensningException;
}
