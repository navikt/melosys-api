package no.nav.melosys.service.aktoer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.TRYGDEMYNDIGHET;

@Service
public class UtenlandskMyndighetService {
    private static final Logger log = LoggerFactory.getLogger(UtenlandskMyndighetService.class);

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final LandvelgerService landvelgerService;
    private final FagsakService fagsakService;

    @Autowired
    public UtenlandskMyndighetService(UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                      LandvelgerService landvelgerService, FagsakService fagsakService) {
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.landvelgerService = landvelgerService;
        this.fagsakService = fagsakService;
    }

    public void avklarUtenlandskMyndighetSomAktørOgLagre(Behandling behandling) {
        String saksnummer = behandling.getFagsak().getSaksnummer();
        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        if (!landkoder.isEmpty()) {
            Collection<String> institusjonsIder = konverterLandkodeTilInstitusjonsId(landkoder);
            fagsakService.oppdaterMyndigheter(saksnummer, institusjonsIder);
        }
    }

    public UtenlandskMyndighet hentUtenlandskMyndighet(Landkoder landkode) {
        return utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."));
    }

    public UtenlandskMyndighet hentUtenlandskMyndighetForInstitusjonID(String institusjonID) {
        return hentUtenlandskMyndighet(UtenlandskMyndighet.konverterInstitusjonIdTilLandkode(institusjonID));
    }

    public List<UtenlandskMyndighet> hentAlleUtenlandskMyndigheter() {
        return utenlandskMyndighetRepository.findAll();
    }

    private Collection<String> konverterLandkodeTilInstitusjonsId(Collection<Landkoder> landkoder) {
        List<String> institusjonsider = new ArrayList<>();
        for (Landkoder landkode : landkoder) {
            institusjonsider.add(lagInstitusjonsId(landkode));
        }
        return institusjonsider;
    }

    public Map<UtenlandskMyndighet, Aktoer> lagUtenlandskeMyndigheterFraBehandling(Behandling behandling) {
        Collection<Landkoder> utenlandskeMyndigheterLandkoder = new ArrayList<>();
        try {
            utenlandskeMyndigheterLandkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        } catch (IkkeFunnetException e) {
            log.info("Landvelger fant ingen utenlandske myndigheter for behandling {}", behandling.getId());
        }
        List<UtenlandskMyndighet> utenlandskMyndighetList = utenlandskMyndighetRepository.findByLandkodeIsIn(utenlandskeMyndigheterLandkoder);

        return utenlandskMyndighetList.stream()
            .collect(Collectors.toMap(utenlandskMyndighet -> utenlandskMyndighet, this::lagAktoer));
    }

    private Aktoer lagAktoer(UtenlandskMyndighet utenlandskMyndighet) {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(TRYGDEMYNDIGHET);
        aktoer.setInstitusjonId(lagInstitusjonsId(utenlandskMyndighet));
        return aktoer;
    }

    public String lagInstitusjonsId(Landkoder landkode) {
        UtenlandskMyndighet myndighet = hentUtenlandskMyndighet(landkode);
        return lagInstitusjonsId(myndighet);
    }

    public String lagInstitusjonsId(UtenlandskMyndighet utenlandskMyndighet) {
        return utenlandskMyndighet.landkode
            + (utenlandskMyndighet.institusjonskode == null ? "" : ":" + utenlandskMyndighet.institusjonskode);
    }
}
