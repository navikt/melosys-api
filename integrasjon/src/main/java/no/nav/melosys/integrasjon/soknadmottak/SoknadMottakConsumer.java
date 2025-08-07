package no.nav.melosys.integrasjon.soknadmottak;

import java.util.Collection;

import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import org.springframework.retry.annotation.Retryable;

@Retryable
public interface SoknadMottakConsumer {
    MedlemskapArbeidEOSM hentSøknad(String søknadID);
    Collection<AltinnDokument> hentDokumenter(String søknadID);
}
