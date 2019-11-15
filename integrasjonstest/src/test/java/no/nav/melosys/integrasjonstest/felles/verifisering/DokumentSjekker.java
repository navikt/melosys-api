package no.nav.melosys.integrasjonstest.felles.verifisering;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysSystemService;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;
import static org.mockito.internal.verification.VerificationModeFactory.atMost;

@Component
@Scope("prototype")
public class DokumentSjekker {

    private static final Logger logger = LoggerFactory.getLogger(DokumentSjekker.class);

    @SpyBean // @Mockbean er raskere, men verifiserer ikke mot xsd
    DoksysSystemService doksysService;

    ArgumentCaptor<Dokumentbestilling> dokumentbestillingCaptor = ArgumentCaptor.forClass(Dokumentbestilling.class);

    public void sjekkBrevBestilt(ForventetDokumentBestilling... forventedeDokumentBestillinger) throws FunksjonellException, TekniskException {
        verify(doksysService, atLeast(0)).produserIkkeredigerbartDokument(dokumentbestillingCaptor.capture());
        dokumentbestillingCaptor.getAllValues().stream()
            .map(Dokumentbestilling::getMetadata)
            .forEach(res -> logger.info("Produserte brev: dokumentIypeId {},  aktørrolle {}, mottakerId {}", res.dokumenttypeID, res.mottaker.getRolle(), res.mottakerID));

        Assertions.assertThat(forventedeDokumentBestillinger).allMatch(this::finnesIResultat);
        verify(doksysService, atMost(forventedeDokumentBestillinger.length)).produserIkkeredigerbartDokument(any());
    }

    public void ingenBrevSendt() throws FunksjonellException, TekniskException {
        sjekkBrevBestilt();
    }

    public DoksysSystemService getDoksysService() {
        return doksysService;
    }

    public boolean finnesIResultat(ForventetDokumentBestilling forventet) {
        List<Dokumentbestilling> resultat = dokumentbestillingCaptor.getAllValues();

        return resultat.stream()
            .map(Dokumentbestilling::getMetadata)
            .anyMatch(forventet::erOppfylt);
    }
}