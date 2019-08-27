package no.nav.melosys.saksflyt.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import no.nav.melosys.service.dokument.brev.ressurser.BrevdataInput;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevBestiller {

    private static final Logger log = LoggerFactory.getLogger(BrevBestiller.class);

    private final DokumentSystemService dokumentService;
    private final BrevDataByggerVelger brevDataByggerVelger;
    private final BrevdataInput brevdataInput;

    @Autowired
    public BrevBestiller(DokumentSystemService dokumentService, BrevDataByggerVelger brevDataByggerVelger, BrevdataInput brevdataInput) {
        this.dokumentService = dokumentService;
        this.brevDataByggerVelger = brevDataByggerVelger;
        this.brevdataInput = brevdataInput;
    }

    public void bestill(Produserbaredokumenter dokumentType, String avsender, Mottaker mottaker, Behandling behandling) throws FunksjonellException, TekniskException {
        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(dokumentType)
            .medAvsender(avsender)
            .medMottaker(mottaker)
            .medBehandling(behandling).build();
        Brevressurser brevdataRessurser = brevdataInput.av(behandling);

        bestill(brevbestilling, brevdataRessurser);
    }

    public void bestill(Brevbestilling brevbestilling) throws FunksjonellException, TekniskException {
        Brevressurser brevdataRessurser = brevdataInput.av(brevbestilling.getBehandling());
        bestill(brevbestilling, brevdataRessurser);
    }

    public void bestill(Brevbestilling brevbestilling, Brevressurser brevdataRessurser) throws FunksjonellException, TekniskException {
        Produserbaredokumenter dokumentType = brevbestilling.getDokumentType();
        Behandling behandling = brevbestilling.getBehandling();

        BrevDataBygger brevDataBygger = brevDataByggerVelger.hent(brevbestilling.getDokumentType(), brevdataRessurser);
        BrevData brevData = brevDataBygger.lag(brevbestilling.getAvsender());
        brevData.begrunnelseKode = brevbestilling.getBegrunnelseKode();
        brevData.fritekst = brevbestilling.getFritekst();
        dokumentService.produserDokument(dokumentType, brevbestilling.getMottaker(), behandling.getId(), brevData);
        log.info("Brevet '{}' er bestillt for sak {} og behandling {}", dokumentType, behandling.getFagsak().getSaksnummer(), behandling.getId());
    }
}
