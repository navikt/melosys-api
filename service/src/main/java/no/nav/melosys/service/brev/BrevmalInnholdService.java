package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;

@Service
public class BrevmalInnholdService {

    private final BehandlingService behandlingService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    @Autowired
    public BrevmalInnholdService(BehandlingService behandlingService, AvklarteVirksomheterService avklarteVirksomheterService) {
        this.behandlingService = behandlingService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    public List<Produserbaredokumenter> hentBrevMaler(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Produserbaredokumenter> brevmaler = asList(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER);

        return behandling.erAktiv() ? brevmaler : emptyList();
    }

    public List<AvklartVirksomhet> hentArbeidsgivere(long behandlingId) throws IkkeFunnetException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        return avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling);
    }
}
