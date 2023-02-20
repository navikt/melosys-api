package no.nav.melosys.service.brev.bestilling;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.dokument.BrevmottakerService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HentBrevAdresseTilMottakereService {

    private final BehandlingService behandlingService;
    private final BrevmottakerService brevmottakerService;
    private final TilBrevAdresseService tilBrevAdresseService;

    public HentBrevAdresseTilMottakereService(BehandlingService behandlingService, BrevmottakerService brevmottakerService, TilBrevAdresseService tilBrevAdresseService) {
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
        this.tilBrevAdresseService = tilBrevAdresseService;
    }

    @Transactional
    public List<BrevAdresse> hentBrevAdresseTilMottakere(long behandlingId, Mottakerroller rolle) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);

        List<BrevAdresse> brevAdresser = new ArrayList<>();
        var mottakere = brevmottakerService.avklarMottakere(null, Mottaker.medRolle(rolle), behandling, false, false);
        for (Mottaker mottaker : mottakere) {
            BrevAdresse brevAddresse = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);
            brevAdresser.add(brevAddresse);
        }
        return brevAdresser;
    }
}
