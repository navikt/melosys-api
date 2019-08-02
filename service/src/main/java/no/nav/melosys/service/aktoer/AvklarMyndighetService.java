package no.nav.melosys.service.aktoer;

import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;

@Service
public class AvklarMyndighetService {
    private static final Logger logger = LoggerFactory.getLogger(AvklarMyndighetService.class);

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final LandvelgerService landvelgerService;
    private final FagsakService fagsakService;

    public AvklarMyndighetService(UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                  LandvelgerService landvelgerService, FagsakService fagsakService) {
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.landvelgerService = landvelgerService;
        this.fagsakService = fagsakService;
    }

    public void avklarUtenlandskMyndighetOgLagre(Behandling behandling) throws TekniskException, FunksjonellException {
        String saksnummer = behandling.getFagsak().getSaksnummer();
        Landkoder landkode = landvelgerService.hentTrygdemyndighetsland(behandling);
        if (Landkoder.NO.equals(landkode)) {
            logger.info("Myndighetsland for sak {} er Norge.", saksnummer);
        } else {
            String institusjonsID = lagInstitusjonsId(landkode);
            fagsakService.leggTilAktør(saksnummer, Aktoersroller.MYNDIGHET, institusjonsID);
            logger.info("Avklart myndighet {} for sak {}.", institusjonsID, saksnummer);
        }
    }

    /**
     * Brukes til forhåndsvisning fordi myndigheter lagres ikke på behandlingen før saksflyt kalles.
     */
    public Optional<Aktoer> lagUtenlandskMyndighetFraBehandling(Behandling behandling) throws TekniskException, FunksjonellException {
        Landkoder landkode = landvelgerService.hentTrygdemyndighetsland(behandling);
        if (Landkoder.NO.equals(landkode)) {
            return Optional.empty();
        } else {
            Aktoer aktoer = new Aktoer();
            aktoer.setRolle(MYNDIGHET);
            aktoer.setInstitusjonId(lagInstitusjonsId(landkode));
            return Optional.of(aktoer);
        }
    }

    private String lagInstitusjonsId(Landkoder landkode) throws TekniskException {
        UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."));
        return landkode.getKode() + ":" + myndighet.institusjonskode;
    }
}