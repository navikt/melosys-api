package no.nav.melosys.integrasjon.aareg;

import java.util.List;

import no.nav.melosys.domain.Arbeidsforhold;
import no.nav.melosys.integrasjon.felles.IntegrasjonException;

public interface AaregFasade {

    List<Arbeidsforhold> finnArbeidsforholdPrArbeidstaker(String fnr) throws IntegrasjonException;
}
