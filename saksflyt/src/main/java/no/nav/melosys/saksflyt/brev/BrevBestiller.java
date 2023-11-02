package no.nav.melosys.saksflyt.brev;

import java.time.LocalDate;
import java.util.Collection;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.ftrl.Betalingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BrevBestiller {
    private static final Logger log = LoggerFactory.getLogger(BrevBestiller.class);

    private final DokumentServiceFasade dokumentServiceFasade;

    public BrevBestiller(DokumentServiceFasade dokumentServiceFasade) {
        this.dokumentServiceFasade = dokumentServiceFasade;
    }

    public void bestill(Produserbaredokumenter dokumentType, Collection<Mottaker> mottakere, String fritekst,
                        String saksbehandler, String begrunnelseKode, Behandling behandling) {
        for (Mottaker mottaker : mottakere) {
            dokumentServiceFasade.produserOgDistribuerBrev(dokumentType, mottaker, fritekst, begrunnelseKode,
                saksbehandler, behandling.getId());
            log.info("Brevet '{}' er bestillt for sak {} og behandling {}", dokumentType,
                behandling.getFagsak().getSaksnummer(), behandling.getId());
        }
    }

    public void bestillVarselbrevManglendeInnbetaling(Collection<Mottaker> mottakere, LocalDate fakturanummer, Betalingsstatus betalingsstatus, String saksnummer, Long behandlingID){
        var dokumentType = Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING;
        for (Mottaker mottaker : mottakere) {
            dokumentServiceFasade.produserOgDistribuerVarselbrevManglendeInnbetaling(mottaker, fakturanummer, betalingsstatus, behandlingID);
            log.info("Brevet '{}' er bestillt for sak {} og behandling {}", dokumentType, saksnummer, behandlingID);
        }
    }


    public void bestill(DoksysBrevbestilling brevbestilling) {
        Produserbaredokumenter dokumentType = brevbestilling.getProduserbartdokument();
        Behandling behandling = brevbestilling.getBehandling();

        for (Mottaker mottaker : brevbestilling.getMottakere()) {
            dokumentServiceFasade.produserDokument(dokumentType, mottaker, behandling.getId(), brevbestilling);
            log.info("Brevet '{}' er bestillt for sak {} og behandling {}", dokumentType, behandling.getFagsak().getSaksnummer(), behandling.getId());
        }
    }
}
