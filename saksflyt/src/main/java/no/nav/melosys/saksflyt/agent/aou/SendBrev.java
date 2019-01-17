package no.nav.melosys.saksflyt.agent.aou;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProduserbartDokument.ORIENTERING_ANMODNING_UNNTAK;
import static no.nav.melosys.domain.ProduserbartDokument.SED_A001;
import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.AOU_SEND_BREV;

/**
 * Sende ulike brev basert på lovvalgsbestemmelse.
 * <p>
 * Transisjoner:
 * ProsessType.ANMODNING_OM_UNNTAK
 *  AOU_SEND_BREV -> AOU_AVSLUTT_BEHANDLING eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakSendBrev")
public class SendBrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendBrev.class);

    private final DokumentSystemService dokumentService;
    private final BrevDataByggerVelger brevDataByggerVelger;
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public SendBrev(DokumentSystemService dokumentService,
                    BrevDataByggerVelger brevDataByggerVelger,
                    BehandlingRepository behandlingRepository) {
        this.dokumentService = dokumentService;
        this.brevDataByggerVelger = brevDataByggerVelger;
        this.behandlingRepository = behandlingRepository;

        log.info("AnmodningOmUnntakSendBrev initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_SEND_BREV;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        // Henter ut behandling på nytt for å få med saksopplysninger
        Behandling behandling = behandlingRepository.findOneWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        if (behandling == null) {
            throw new TekniskException(String.format("Finner ikke behandlingen %s.", prosessinstans.getBehandling().getId()));
        }

        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        sendBrev(behandling, saksbehandler, ORIENTERING_ANMODNING_UNNTAK, RolleType.BRUKER);
        sendBrev(behandling, saksbehandler, SED_A001, RolleType.MYNDIGHET);

        log.info("Sendt alle brev for anmodning om unntak. Prosessinstans {}", prosessinstans.getId());
        prosessinstans.setSteg(null);
    }

    public void sendBrev(Behandling behandling, String saksbehandler, ProduserbartDokument dokumentType, RolleType mottaker) throws TekniskException, FunksjonellException {
        BrevDataBygger brevDataBygger = brevDataByggerVelger.hent(dokumentType);
        BrevData brevData = brevDataBygger.lag(behandling, saksbehandler);
        brevData.mottaker = mottaker;

        dokumentService.produserDokument(behandling.getId(), dokumentType, brevData);
        log.info("Sendt brevet '{}', for behandling {}", dokumentType, behandling.getId());
    }
}
