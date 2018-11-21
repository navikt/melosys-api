package no.nav.melosys.service.dokument.brev;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

@Component(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BrevDataByggerA1 {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagService registerOppslagService;

    private SoeknadDokument søknad;
    private String saksbehandler;
    private Set<String> avklarteOrganisasjoner;

    @Autowired
    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService,
                            RegisterOppslagService registerOppslagService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
    }

    public BrevDataDto lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.saksbehandler = saksbehandler;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());

        BrevDataDto brevDataDto = new BrevDataDto();
        brevDataDto.saksbehandler = saksbehandler;

        brevDataDto.yrkesgruppe = avklartefaktaService.hentYrkesGruppe(behandling.getId());
        brevDataDto.utenlandskeVirksomheter = hentUtenlandskeAvklarteforetak();
        brevDataDto.norskeVirksomheter = hentNorskeAvklarteForetak();
        brevDataDto.selvstendigeForetak = hentAvklarteSelvstendigeForetak();
        brevDataDto.søknad = søknad;

        return brevDataDto;
    }

    private Set<String> hentAvklarteSelvstendigeForetak() {
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
                .collect(Collectors.toSet());

        organisasjonsnumre.retainAll(avklarteOrganisasjoner);
        return organisasjonsnumre;
    }

    private Set<OrganisasjonDokument> hentNorskeAvklarteForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Set<String> organisasjonsnumre = søknad.hentAlleOrganisasjonsnumre();
        organisasjonsnumre.retainAll(avklarteOrganisasjoner);

        return registerOppslagService.hentOrganisasjoner(organisasjonsnumre);
    }

    private List<ForetakUtland> hentUtenlandskeAvklarteforetak() {
        return søknad.foretakUtland.stream()
                .filter(foretak -> !avklarteOrganisasjoner.contains(foretak.orgnr))
                .collect(Collectors.toList());
    }
}
