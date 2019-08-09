package no.nav.melosys.service.aktoer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.IkkeFunnetException;
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

    public void avklarUtenlandskMyndighetOgLagre(Behandling behandling) throws TekniskException, IkkeFunnetException {
        String saksnummer = behandling.getFagsak().getSaksnummer();
        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        if (landkoder.isEmpty()) {
            throw new TekniskException("Mangler myndighetsland for sak " + saksnummer);
        }

        Collection<String> institusjonsIder = konverterLandkodeTilInstitusjonsId(landkoder);
        fagsakService.leggTilFjernAktørerForMyndighet(saksnummer, institusjonsIder);
    }

    private Collection<String> konverterLandkodeTilInstitusjonsId(Collection<Landkoder> landkoder) throws TekniskException {
        List<String> institusjonsider = new ArrayList<>();
        for (Landkoder landkode : landkoder) {
            institusjonsider.add(lagInstitusjonsId(landkode));
        }
        return institusjonsider;
    }

    /**
     * Brukes til forhåndsvisning fordi myndigheter lagres ikke på behandlingen før saksflyt kalles.
     */
    public List<Aktoer> lagUtenlandskMyndighetFraBehandling(Behandling behandling) throws TekniskException {
        Collection<Landkoder> utenlandskeMyndigheterLandkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        List<Aktoer> myndighetsaktører = new ArrayList<>();
        for (Landkoder landkode : utenlandskeMyndigheterLandkoder) {
            Aktoer aktoer = new Aktoer();
            aktoer.setRolle(MYNDIGHET);
            aktoer.setInstitusjonId(lagInstitusjonsId(landkode));
            myndighetsaktører.add(aktoer);
        }
        return myndighetsaktører;
    }

    private String lagInstitusjonsId(Landkoder landkode) throws TekniskException {
        UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."));
        return landkode.getKode() + ":" + myndighet.institusjonskode;
    }
}