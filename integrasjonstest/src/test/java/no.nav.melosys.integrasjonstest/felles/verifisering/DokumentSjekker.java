package no.nav.melosys.integrasjonstest.felles.verifisering;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysSystemService;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@Component
@Scope("prototype")
public class DokumentSjekker {

    @SpyBean // @Mockbean er raskere, men verifiserer ikke mot xsd
    DoksysSystemService doksysService;

    ArgumentCaptor<Dokumentbestilling> dokumentbestillingCaptor = ArgumentCaptor.forClass(Dokumentbestilling.class);

    public void erBrevBestilt(ForventetDokumentBestilling... forventedeDokumentBestillinger) throws FunksjonellException, TekniskException {
        verify(doksysService, times(forventedeDokumentBestillinger.length)).produserIkkeredigerbartDokument(dokumentbestillingCaptor.capture());
        Assertions.assertThat(forventedeDokumentBestillinger).allMatch(this::finnesIResultat);
    }

    public void ingenBrevSendt() throws FunksjonellException, TekniskException {
        erBrevBestilt();
    }

    public DoksysSystemService getDoksysService() {
        return doksysService;
    }

    public boolean finnesIResultat(ForventetDokumentBestilling forventet) {
        for (Dokumentbestilling res : dokumentbestillingCaptor.getAllValues()) {
            if (forventet.erOppfylt(res.getMetadata())) {
                return true;
            }
        }
        return false;
    }
}