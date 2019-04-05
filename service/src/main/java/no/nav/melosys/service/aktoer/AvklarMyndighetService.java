package no.nav.melosys.service.aktoer;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;

@Service
public class AvklarMyndighetService {

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    private final LandvelgerService landvelgerService;

    private final FagsakService fagsakService;

    public AvklarMyndighetService(UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                  LandvelgerService landvelgerService, FagsakService fagsakService) {
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.landvelgerService = landvelgerService;
        this.fagsakService = fagsakService;
    }

    public void avklarMyndighetOgLagre(Behandling behandling) throws TekniskException {
        fagsakService.leggTilAktør(
            behandling.getFagsak().getSaksnummer(), Aktoersroller.MYNDIGHET, lagInstitusjonsId(behandling)
        );
    }

    public Aktoer avklarMyndighet(Behandling behandling) throws TekniskException {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(MYNDIGHET);
        aktoer.setInstitusjonId(lagInstitusjonsId(behandling));
        return aktoer;
    }

    private String lagInstitusjonsId(Behandling behandling) throws TekniskException {
        Landkoder landkode = landvelgerService.hentTrygdemyndighetsland(behandling);
        UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode);
        return landkode.getKode() + ":" + myndighet.institusjonskode;
    }
}
