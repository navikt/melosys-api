package no.nav.melosys.service.dokument.brev;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataByggerAnmodningUnntak implements BrevDataBygger {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagService registerOppslagService;

    public BrevDataByggerAnmodningUnntak(AvklartefaktaService avklartefaktaService,
                                         RegisterOppslagService registerOppslagService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

        Set<String> avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
        Optional<Virksomhet> hovedvirksomhet = registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner).stream()
            .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), null))
            .findFirst();

        BrevDataAnmodningUnntak brevData = new BrevDataAnmodningUnntak(saksbehandler);
        hovedvirksomhet.ifPresent(hv -> brevData.hovedvirksomhet = hv);

        return brevData;
    }
}
