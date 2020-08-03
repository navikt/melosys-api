package no.nav.melosys.integrasjon.altinn;

import no.nav.melosys.altinn.MedlemskapArbeidEOSM;

public interface SoknadMottakConsumer {
    MedlemskapArbeidEOSM hentSøknad(String søknadID);
}
