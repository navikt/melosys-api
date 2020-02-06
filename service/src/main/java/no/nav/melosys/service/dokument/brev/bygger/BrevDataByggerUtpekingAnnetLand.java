package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataUtpekingAnnetLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.utpeking.UtpekingService;

public class BrevDataByggerUtpekingAnnetLand implements BrevDataBygger {
    private final UtpekingService utpekingService;
    private final BrevbestillingDto brevbestillingDto;

    public BrevDataByggerUtpekingAnnetLand(UtpekingService utpekingService, BrevbestillingDto brevbestillingDto) {
        this.utpekingService = utpekingService;
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException {
        BrevDataUtpekingAnnetLand brevDataUtpekingAnnetLand = new BrevDataUtpekingAnnetLand(brevbestillingDto, saksbehandler);
        long behandlingID = dataGrunnlag.getBehandling().getId();
        brevDataUtpekingAnnetLand.utpekingsperiode = utpekingService.hentUtpekingsperioder(behandlingID)
            .stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Brev om utpeking av annet land for behandling " + behandlingID
                + " kan ikke produseres uten utpekingsperiode."));
        return brevDataUtpekingAnnetLand;
    }
}
