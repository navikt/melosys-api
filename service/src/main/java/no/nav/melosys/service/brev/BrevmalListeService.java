package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.brev.bestilling.HentBrevAdresseTilMottakereService;
import no.nav.melosys.service.brev.bestilling.HentMuligeProduserbaredokumenterService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrevmalListeService {

    private final HentMuligeProduserbaredokumenterService hentMuligeProduserbaredokumenterService;
    private final HentBrevAdresseTilMottakereService hentBrevAdresseTilMottakereService;

    public BrevmalListeService(HentMuligeProduserbaredokumenterService hentMuligeProduserbaredokumenterService,
                               HentBrevAdresseTilMottakereService hentBrevAdresseTilMottakereService) {
        this.hentMuligeProduserbaredokumenterService = hentMuligeProduserbaredokumenterService;
        this.hentBrevAdresseTilMottakereService = hentBrevAdresseTilMottakereService;
    }

    public List<Produserbaredokumenter> hentMuligeProduserbaredokumenter(long behandlingId, Aktoersroller aktoersroller) {
        return hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, aktoersroller);
    }

    @Transactional
    public List<BrevAdresse> hentBrevAdresseTilMottakere(long behandlingId, Aktoersroller aktoersroller) {
        return hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, aktoersroller);
    }
}
