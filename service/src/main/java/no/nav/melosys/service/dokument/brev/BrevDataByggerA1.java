package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BrevDataByggerA1 {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagSystemService registerOppslagService;
    private final BehandlingRepository behandlingRepository;

    private Set<String> avklarteOrganisasjoner;
    private KodeverkService kodeverkService;
    private SoeknadDokument søknad;
    private PersonDokument person;

    @Autowired
    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService,
                            BehandlingRepository behandlingRepository,
                            RegisterOppslagSystemService registerOppslagService, KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.behandlingRepository = behandlingRepository;
        this.kodeverkService = kodeverkService;
    }

    @Transactional
    public BrevDataDto lag(long behandlingId, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Behandling behandling = behandlingRepository.findOneWithSaksopplysningerById(behandlingId);
        if (behandling == null) {
            throw new TekniskException("Finner ikke behandling");
        }

        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandlingId);

        BrevDataA1Dto brevDataDto = new BrevDataA1Dto();
        brevDataDto.saksbehandler = saksbehandler;

        brevDataDto.yrkesgruppe = avklartefaktaService.hentYrkesGruppe(behandlingId);
        brevDataDto.utenlandskeVirksomheter = hentUtenlandskeAvklarteVirksomheter();
        brevDataDto.norskeVirksomheter = hentAlleNorskeAvklarteVirksomheter();
        brevDataDto.selvstendigeForetak = hentAvklarteSelvstendigeForetak();
        brevDataDto.bostedsadresse = hentBostedsadresse();
        brevDataDto.søknad = søknad;

        return brevDataDto;
    }

    private Bostedsadresse hentBostedsadresse() {
        Bostedsadresse adresse = person.bostedsadresse;
        adresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnr(), LocalDate.now()));
        return adresse;
    }

    private Set<String> hentAvklarteSelvstendigeForetak() {
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
                .collect(Collectors.toSet());

        organisasjonsnumre.retainAll(avklarteOrganisasjoner);
        return organisasjonsnumre;
    }

    private List<Virksomhet> hentAlleNorskeAvklarteVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Set<String> organisasjonsnumre = søknad.hentAlleOrganisasjonsnumre();
        organisasjonsnumre.retainAll(avklarteOrganisasjoner);

        return registerOppslagService.hentOrganisasjoner(organisasjonsnumre).stream()
                .map(org -> new Virksomhet(org.getSammenslåttNavn(), org.getOrgnummer(), utfyllManglendeAdressefelter(org)))
                .collect(Collectors.toList());
    }

    private StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
    }

    private List<Virksomhet> hentUtenlandskeAvklarteVirksomheter() {
        return søknad.foretakUtland.stream()
                //TODO: utenlandske foretak har ikke nødvendigvis orgnr!
                //.filter(foretak -> avklarteOrganisasjoner.contains(foretak.orgnr))
                .map(Virksomhet::new)
                .collect(Collectors.toList());
    }
}
