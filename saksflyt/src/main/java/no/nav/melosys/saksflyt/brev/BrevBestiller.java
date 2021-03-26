package no.nav.melosys.saksflyt.brev;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevBestiller {
    private static final Logger log = LoggerFactory.getLogger(BrevBestiller.class);

    private final DokumentServiceFasade dokumentServiceFasade;

    @Autowired
    public BrevBestiller(DokumentServiceFasade dokumentServiceFasade) {
        this.dokumentServiceFasade = dokumentServiceFasade;
    }

    public void bestill(Produserbaredokumenter dokumentType, String avsender, Mottaker mottaker, Behandling behandling) throws FunksjonellException, TekniskException {
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(dokumentType)
            .medAvsenderNavn(avsender)
            .medMottakere(mottaker)
            .medBehandling(behandling).build();
        bestill(brevbestilling);
    }

    public void bestill(DoksysBrevbestilling brevbestilling) throws FunksjonellException, TekniskException {
        Produserbaredokumenter dokumentType = brevbestilling.getProduserbartdokument();
        Behandling behandling = brevbestilling.getBehandling();

        for (Mottaker mottaker : brevbestilling.getMottakere()) {
            dokumentServiceFasade.produserDokument(dokumentType, mottaker, behandling.getId(), brevbestilling);
            log.info("Brevet '{}' er bestillt for sak {} og behandling {}", dokumentType, behandling.getFagsak().getSaksnummer(), behandling.getId());
        }
    }
}
