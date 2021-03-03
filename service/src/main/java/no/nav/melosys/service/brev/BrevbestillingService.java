package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;

@Service
public class BrevbestillingService {

    private final BehandlingService behandlingService;
    private final DokumentServiceFasade dokumentServiceFasade;
    private final DokgenService dokgenService;

    @Autowired
    public BrevbestillingService(BehandlingService behandlingService, DokumentServiceFasade dokumentServiceFasade, DokgenService dokgenService) {
        this.behandlingService = behandlingService;
        this.dokumentServiceFasade = dokumentServiceFasade;
        this.dokgenService = dokgenService;
    }

    public List<Produserbaredokumenter> hentBrevMaler(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Produserbaredokumenter> brevmaler = asList(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER);

        return behandling.erAktiv() ? brevmaler : emptyList();
    }

    public void produserBrev(Produserbaredokumenter produserbartDokument, long behandlingID, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        //TODO Legge til valg av mal basert på brevbestilling.mottaker (rolle)
        dokgenService.produserOgDistribuerBrev(produserbartDokument, behandlingID, brevbestillingDto);
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartDokument, long behandlingID, BrevbestillingDto brevbestillingDto)
        throws FunksjonellException, TekniskException {
        //TODO Legge til valg av mal basert på brevbestilling.mottaker (rolle)
        return dokumentServiceFasade.produserUtkast(produserbartDokument, behandlingID, brevbestillingDto);
    }
}
