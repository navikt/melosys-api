package no.nav.melosys.saksflyt.agent.aou;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.agent.AbstraktAvklarMyndighet;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.AOU_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.ProsessSteg.AOU_SEND_BREV;

/**
 * Avklarer hvilken utenlandsk myndighet er part i saken.
 *
 * Transisjoner:
 *  AOU_AVKLAR_MYNDIGHET -> AOU_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvklarMyndighet extends AbstraktAvklarMyndighet {

    private static final Logger log = LoggerFactory.getLogger(AvklarMyndighet.class);

    @Autowired
    public AvklarMyndighet(BehandlingRepository behandlingRepository,
                           BehandlingsresultatRepository behandlingsresultatRepository,
                           FagsakService fagsakService,
                           LandvelgerService landvelgerService,
                           UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        super(behandlingRepository, behandlingsresultatRepository, fagsakService,
            landvelgerService, utenlandskMyndighetRepository);
        log.info("AvklarMyndighet initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_AVKLAR_MYNDIGHET;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        super.utfør(prosessinstans);
        prosessinstans.setSteg(AOU_SEND_BREV);
    }
}