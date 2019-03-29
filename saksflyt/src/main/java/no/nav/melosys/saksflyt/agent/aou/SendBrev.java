package no.nav.melosys.saksflyt.agent.aou;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.BrevBestiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.AOU_SEND_BREV;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.ANMODNING_UNNTAK;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK;

/**
 * Sende ulike brev basert på lovvalgsbestemmelse.
 * <p>
 * Transisjoner:
 * ProsessType.ANMODNING_OM_UNNTAK
 *  AOU_SEND_BREV -> AOU_SEND_SED eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakSendBrev")
public class SendBrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendBrev.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public SendBrev(BrevBestiller brevBestiller,
                    BehandlingRepository behandlingRepository) {
        this.brevBestiller = brevBestiller;
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

        // Henter ut behandling for å få med saksopplysninger
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        if (behandling == null) {
            throw new TekniskException(String.format("Finner ikke behandlingen %s.", prosessinstans.getBehandling().getId()));
        }

        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        brevBestiller.bestill(ORIENTERING_ANMODNING_UNNTAK, saksbehandler, Aktoersroller.BRUKER, behandling);
        brevBestiller.bestill(ANMODNING_UNNTAK, saksbehandler, Aktoersroller.MYNDIGHET, behandling);

        log.info("Sendt alle brev for anmodning om unntak. Prosessinstans {}", prosessinstans.getId());
        prosessinstans.setSteg(ProsessSteg.AOU_SEND_SED);
    }

}
