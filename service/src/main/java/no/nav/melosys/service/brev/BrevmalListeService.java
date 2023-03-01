package no.nav.melosys.service.brev;

import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.brev.bestilling.HentBrevAdresseTilMottakereService;
import no.nav.melosys.service.brev.bestilling.HentMuligeProduserbaredokumenterService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrevmalListeService {

    private final HentMuligeProduserbaredokumenterService hentMuligeProduserbaredokumenterService;
    private final HentBrevAdresseTilMottakereService hentBrevAdresseTilMottakereService;

    public BrevmalListeService(HentMuligeProduserbaredokumenterService hentMuligeProduserbaredokumenterService,
                               HentBrevAdresseTilMottakereService hentBrevAdresseTilMottakereService) {
        this.hentMuligeProduserbaredokumenterService = hentMuligeProduserbaredokumenterService;
        this.hentBrevAdresseTilMottakereService = hentBrevAdresseTilMottakereService;
    }

    public List<Produserbaredokumenter> hentMuligeProduserbaredokumenter(long behandlingId, Mottakerroller rolle) {
        return hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, rolle);
    }

    @Transactional
    public List<BrevAdresse> hentBrevAdresseTilMottakere(long behandlingId, Mottakerroller rolle) {
        return hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, rolle);
    }
}
