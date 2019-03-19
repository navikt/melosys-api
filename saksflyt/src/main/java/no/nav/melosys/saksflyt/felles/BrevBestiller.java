package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevBestiller {

    private static final Logger log = LoggerFactory.getLogger(BrevBestiller.class);

    private final DokumentSystemService dokumentService;
    private final BrevDataByggerVelger brevDataByggerVelger;

    @Autowired
    public BrevBestiller(DokumentSystemService dokumentService, BrevDataByggerVelger brevDataByggerVelger) {
        this.dokumentService = dokumentService;
        this.brevDataByggerVelger = brevDataByggerVelger;
    }

    public void bestill(Behandling behandling,
                        String saksbehandler,
                        Produserbaredokumenter dokumentType,
                        Aktoersroller aktoersroller) throws FunksjonellException, TekniskException {
        bestill(behandling, saksbehandler, dokumentType, aktoersroller, null);
    }

    public void bestill(Behandling behandling,
                        String saksbehandler,
                        Produserbaredokumenter dokumentType,
                        Aktoersroller aktoersroller, Endretperioder endretPeriodeBegrunnelseKode) throws FunksjonellException, TekniskException {
        BrevDataBygger brevDataBygger = brevDataByggerVelger.hent(dokumentType);
        BrevData brevData = brevDataBygger.lag(behandling, saksbehandler);
        brevData.mottaker = aktoersroller;
        if (endretPeriodeBegrunnelseKode != null) {
            brevData.begrunnelseKode = endretPeriodeBegrunnelseKode.getKode();
        }
        dokumentService.produserDokument(behandling.getId(), dokumentType, brevData);
        log.info("Sendt brevet '{}', for behandling {}", dokumentType, behandling.getId());
    }
}
