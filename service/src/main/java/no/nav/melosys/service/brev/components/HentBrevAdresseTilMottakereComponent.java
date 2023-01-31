package no.nav.melosys.service.brev.components;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.dokument.BrevmottakerService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HentBrevAdresseTilMottakereComponent {

    private final BehandlingService behandlingService;
    private final BrevmottakerService brevmottakerService;
    private final TilBrevAdresseComponent tilBrevAdresseComponent;

    public HentBrevAdresseTilMottakereComponent(BehandlingService behandlingService, BrevmottakerService brevmottakerService, TilBrevAdresseComponent tilBrevAdresseComponent) {
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
        this.tilBrevAdresseComponent = tilBrevAdresseComponent;
    }

    @Transactional
    public List<BrevAdresse> hentBrevAdresseTilMottakere(long behandlingId, Aktoersroller aktoersroller) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);

        List<BrevAdresse> brevAdresser = new ArrayList<>();
        var mottakere = brevmottakerService.avklarMottakere(null, Mottaker.av(aktoersroller), behandling, false, false);
        for (Aktoer mottaker : mottakere) {
            BrevAdresse brevAddresse = tilBrevAdresseComponent.tilBrevAdresse(mottaker, behandling);
            brevAdresser.add(brevAddresse);
        }
        return brevAdresser;
    }
}
