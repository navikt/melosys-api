package no.nav.melosys.service.aktoer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Preferanse;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;

@Service
public class UtenlandskMyndighetService {
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

    public void avklarUtenlandskMyndighetSomAktørOgLagre(Behandling behandling) throws TekniskException, IkkeFunnetException {
        String saksnummer = behandling.getFagsak().getSaksnummer();
        Collection<Landkoder> landkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        if (landkoder.isEmpty()) {
            throw new TekniskException("Mangler myndighetsland for sak " + saksnummer);
        }

        Collection<String> institusjonsIder = konverterLandkodeTilInstitusjonsId(landkoder);
        fagsakService.oppdaterMyndigheter(saksnummer, institusjonsIder);
    }

    private Collection<String> konverterLandkodeTilInstitusjonsId(Collection<Landkoder> landkoder) throws TekniskException {
        List<String> institusjonsider = new ArrayList<>();
        for (Landkoder landkode : landkoder) {
            institusjonsider.add(lagInstitusjonsId(landkode));
        }
        return institusjonsider;
    }

    /**
     * Brukes til brevutsendelse fordi alle myndigheter lagres som aktører, men ikke alle ønsker brev tilsendt.
     */
    public List<Aktoer> lagUtenlandskMyndighetFraBehandling(Behandling behandling) throws TekniskException {
        Collection<Landkoder> utenlandskeMyndigheterLandkoder = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        List<Aktoer> myndighetsaktører = new ArrayList<>();
        for (Landkoder myndighetensLandkode : utenlandskeMyndigheterLandkoder) {
            if (myndighetØnskerInnvilgelsesbrev(myndighetensLandkode)) {
                Aktoer aktoer = new Aktoer();
                aktoer.setRolle(MYNDIGHET);
                aktoer.setInstitusjonId(lagInstitusjonsId(myndighetensLandkode));
                myndighetsaktører.add(aktoer);
            }
        }
        return myndighetsaktører;
    }

    private boolean myndighetØnskerInnvilgelsesbrev(Landkoder landkode) throws TekniskException {
        return utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."))
            .preferanser.stream().map(Preferanse::getPreferanse)
            .noneMatch(p -> p == Preferanse.PreferanseEnum.RESERVERT_FRA_A1);
    }

    private String lagInstitusjonsId(Landkoder landkode) throws TekniskException {
        UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."));
        return landkode.getKode() + ":" + myndighet.institusjonskode;
    }
}