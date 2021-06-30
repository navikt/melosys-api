package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataUtpekingAnnetLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.utpeking.UtpekingService;

public class BrevDataByggerUtpekingAnnetLand implements BrevDataBygger {
    private final UtpekingService utpekingService;
    private final BrevbestillingRequest brevbestillingRequest;

    public BrevDataByggerUtpekingAnnetLand(UtpekingService utpekingService, BrevbestillingRequest brevbestillingRequest) {
        this.utpekingService = utpekingService;
        this.brevbestillingRequest = brevbestillingRequest;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        BrevDataUtpekingAnnetLand brevDataUtpekingAnnetLand = new BrevDataUtpekingAnnetLand(brevbestillingRequest, saksbehandler);
        long behandlingID = dataGrunnlag.getBehandling().getId();
        brevDataUtpekingAnnetLand.utpekingsperiode = utpekingService.hentUtpekingsperioder(behandlingID)
            .stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Brev om utpeking av annet land for behandling " + behandlingID
                + " kan ikke produseres uten utpekingsperiode."));
        return brevDataUtpekingAnnetLand;
    }
}
