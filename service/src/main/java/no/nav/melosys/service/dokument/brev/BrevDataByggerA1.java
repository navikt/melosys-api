package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.RolleType;
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
import org.springframework.stereotype.Component;

@Component
public class BrevDataByggerA1 implements BrevDataBygger {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagSystemService registerOppslagService;
    private final BehandlingRepository behandlingRepository;
    private final KodeverkService kodeverkService;

    @Autowired
    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService,
                            BehandlingRepository behandlingRepository,
                            RegisterOppslagSystemService registerOppslagService,
                            KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.behandlingRepository = behandlingRepository;
        this.kodeverkService = kodeverkService;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        PersonDokument person = SaksopplysningerUtils.hentPersonDokument(behandling);
        Set<String> avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());

        BrevDataA1 brevData = new BrevDataA1(saksbehandler);

        brevData.mottaker = RolleType.BRUKER;
        brevData.yrkesgruppe = avklartefaktaService.hentYrkesGruppe(behandling.getId());
        brevData.utenlandskeVirksomheter = hentUtenlandskeAvklarteVirksomheter(søknad);
        brevData.norskeVirksomheter = hentAlleNorskeAvklarteVirksomheter(søknad, avklarteOrganisasjoner);
        brevData.selvstendigeForetak = hentAvklarteSelvstendigeForetak(søknad, avklarteOrganisasjoner);
        brevData.bostedsadresse = hentBostedsadresse(person);
        brevData.søknad = søknad;

        return brevData;
    }

    private Bostedsadresse hentBostedsadresse(PersonDokument person) {
        Bostedsadresse adresse = person.bostedsadresse;
        adresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnr(), LocalDate.now()));
        return adresse;
    }

    private Set<String> hentAvklarteSelvstendigeForetak(SoeknadDokument søknad, Set<String> avklarteOrganisasjoner) {
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
                .collect(Collectors.toSet());

        organisasjonsnumre.retainAll(avklarteOrganisasjoner);
        return organisasjonsnumre;
    }

    private List<Virksomhet> hentAlleNorskeAvklarteVirksomheter(SoeknadDokument søknad, Set<String> avklarteOrganisasjoner) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Set<String> organisasjonsnumre = søknad.hentAlleOrganisasjonsnumre();
        organisasjonsnumre.retainAll(avklarteOrganisasjoner);

        return registerOppslagService.hentOrganisasjoner(organisasjonsnumre).stream()
                .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), utfyllManglendeAdressefelter(org)))
                .collect(Collectors.toList());
    }

    private StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
    }

    private List<Virksomhet> hentUtenlandskeAvklarteVirksomheter(SoeknadDokument søknad) {
        return søknad.foretakUtland.stream()
                //TODO: utenlandske foretak har ikke nødvendigvis orgnr!
                //.filter(foretak -> avklarteOrganisasjoner.contains(foretak.orgnr))
                .map(Virksomhet::new)
                .collect(Collectors.toList());
    }
}
