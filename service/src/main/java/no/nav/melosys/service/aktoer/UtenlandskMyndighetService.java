package no.nav.melosys.service.aktoer;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.base.Enums;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.TRYGDEMYNDIGHET;

@Service
public class UtenlandskMyndighetService {
    private static final Logger log = LoggerFactory.getLogger(UtenlandskMyndighetService.class);

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final LandvelgerService landvelgerService;
    private final FagsakService fagsakService;

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
            Sakstyper sakstype = behandling.getFagsak().getType();
            if (sakstype == Sakstyper.TRYGDEAVTALE) {
                fagsakService.oppdaterMyndighetForTrygdeavtale(saksnummer, hentLandkodeForTrygdeavtale(landkoder));
            } else if (sakstype == Sakstyper.EU_EOS) {
                Collection<String> institusjonsIder = landkoder.stream().map(this::hentEøsInstitusjonID).toList();
                fagsakService.oppdaterMyndigheterForEuEos(saksnummer, institusjonsIder);
            } else {
                log.debug("Myndighet lagres ikke for sakstype {}", sakstype);
            }
        }
    }

    private Optional<UtenlandskMyndighet> finnUtenlandskMyndighet(String landkode) {
        Optional<Landkoder> eøsLandkodeOptional = Enums.getIfPresent(Landkoder.class, landkode).toJavaUtil();
        return eøsLandkodeOptional.flatMap(utenlandskMyndighetRepository::findByLandkode);
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
        aktoer.setInstitusjonId(utenlandskMyndighet.hentInstitusjonID());
        return aktoer;
    }

    public Optional<String> finnInstitusjonID(String landkode) {
        return finnUtenlandskMyndighet(landkode).map(UtenlandskMyndighet::hentInstitusjonID);
    }

    private String hentEøsInstitusjonID(Landkoder landkode) {
        UtenlandskMyndighet myndighet = hentUtenlandskMyndighet(landkode);
        return myndighet.hentInstitusjonID();
    }

    private Landkoder hentLandkodeForTrygdeavtale(Collection<Landkoder> landkoder) {
        if (landkoder.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett trygdemyndighetsland for bilaterale trygdeavtaler.");
        }
        return landkoder.stream().findFirst().orElseThrow(
            () -> new FunksjonellException("Fant ingen trygdemyndighetsland for bilaterale trygdeavtaler."));
    }
}
