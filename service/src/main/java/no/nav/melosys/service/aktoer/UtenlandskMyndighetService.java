package no.nav.melosys.service.aktoer;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.base.Enums;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UtenlandskMyndighetService {
    private static final Logger log = LoggerFactory.getLogger(UtenlandskMyndighetService.class);

    private static final UtenlandskMyndighet utenlandskMyndighetUnntakUSA;

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final LandvelgerService landvelgerService;
    private final FagsakService fagsakService;

    static {
        var utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.navn = "Social Security Administration";
        utenlandskMyndighet.gateadresse1 = "Division of Training and Program Support";
        utenlandskMyndighet.gateadresse2 = "International Support Branch, NT 03-A-09 6100 Wabash Avenue";
        utenlandskMyndighet.poststed = "Baltimore MD 21215";
        utenlandskMyndighet.land = "USA";
        utenlandskMyndighet.landkode = Land_iso2.US;
        utenlandskMyndighetUnntakUSA = utenlandskMyndighet;
    }

    public UtenlandskMyndighetService(UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                      LandvelgerService landvelgerService, FagsakService fagsakService) {
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.landvelgerService = landvelgerService;
        this.fagsakService = fagsakService;
    }

    public void avklarUtenlandskMyndighetSomAktørOgLagre(Behandling behandling) {
        String saksnummer = behandling.getFagsak().getSaksnummer();
        Collection<Land_iso2> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
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
        Optional<Land_iso2> eøsLandkodeOptional = Enums.getIfPresent(Land_iso2.class, landkode).toJavaUtil();
        return eøsLandkodeOptional.flatMap(utenlandskMyndighetRepository::findByLandkode);
    }

    public UtenlandskMyndighet hentUtenlandskMyndighet(Land_iso2 landkode) {
        return hentUtenlandskMyndighet(landkode, null);
    }

    public UtenlandskMyndighet hentUtenlandskMyndighet(Land_iso2 landkode, Produserbaredokumenter produserbaredokumenter) {
        if (landkode == Land_iso2.US && produserbaredokumenter == Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV) {
            return hentUtenlandskMyndighetUnntakUSA();
        }
        return utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."));
    }

    public UtenlandskMyndighet hentUtenlandskMyndighetForInstitusjonID(String institusjonID) {
        return hentUtenlandskMyndighet(UtenlandskMyndighet.konverterInstitusjonIdTilLandkode(institusjonID));
    }

    public List<UtenlandskMyndighet> hentAlleUtenlandskMyndigheter() {
        return utenlandskMyndighetRepository.findAll();
    }

    public Map<UtenlandskMyndighet, Mottaker> lagUtenlandskeMyndigheterFraBehandling(Behandling behandling) {
        Collection<Land_iso2> utenlandskeMyndigheterLandkoder = new ArrayList<>();
        try {
            utenlandskeMyndigheterLandkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId());
        } catch (IkkeFunnetException e) {
            log.info("Landvelger fant ingen utenlandske myndigheter for behandling {}", behandling.getId());
        }
        List<UtenlandskMyndighet> utenlandskMyndighetList = utenlandskMyndighetRepository.findByLandkodeIsIn(utenlandskeMyndigheterLandkoder);

        return utenlandskMyndighetList.stream()
            .collect(Collectors.toMap(utenlandskMyndighet -> utenlandskMyndighet, this::lagMottaker));
    }

    private Mottaker lagMottaker(UtenlandskMyndighet utenlandskMyndighet) {
        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);
        mottaker.setInstitusjonID(utenlandskMyndighet.hentInstitusjonID());
        return mottaker;
    }

    public Optional<String> finnInstitusjonID(String landkode) {
        return finnUtenlandskMyndighet(landkode).map(UtenlandskMyndighet::hentInstitusjonID);
    }

    private String hentEøsInstitusjonID(Land_iso2 landkode) {
        UtenlandskMyndighet myndighet = hentUtenlandskMyndighet(landkode, null);
        return myndighet.hentInstitusjonID();
    }

    private Land_iso2 hentLandkodeForTrygdeavtale(Collection<Land_iso2> landkoder) {
        if (landkoder.size() != 1) {
            throw new FunksjonellException("Fant ingen eller flere enn ett trygdemyndighetsland for bilaterale trygdeavtaler.");
        }
        return landkoder.stream().findFirst().orElseThrow(
            () -> new FunksjonellException("Fant ingen trygdemyndighetsland for bilaterale trygdeavtaler."));
    }

    private UtenlandskMyndighet hentUtenlandskMyndighetUnntakUSA() {
        return utenlandskMyndighetUnntakUSA;
    }
}
