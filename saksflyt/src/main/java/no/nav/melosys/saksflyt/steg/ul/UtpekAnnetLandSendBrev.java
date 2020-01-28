package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ORIENTERING_UTPEKING_UTLAND;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

@Component
public class UtpekAnnetLandSendBrev extends AbstraktStegBehandler {

    private final BrevBestiller brevBestiller;

    public UtpekAnnetLandSendBrev(BrevBestiller brevBestiller) {
        this.brevBestiller = brevBestiller;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.UL_SEND_BREV;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);

        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(ORIENTERING_UTPEKING_UTLAND)
            .medAvsender(saksbehandler)
            .medBehandling(behandling)
            .medMottakere(Mottaker.av(BRUKER))
            .build();
        brevBestiller.bestill(brevbestilling);

        prosessinstans.setSteg(ProsessSteg.UL_SEND_UTLAND);
    }
}
