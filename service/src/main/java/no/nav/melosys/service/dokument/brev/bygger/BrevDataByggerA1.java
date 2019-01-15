package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataByggerA1 extends BrevDatabyggerBase implements BrevDataBygger {

    private final RegisterOppslagSystemService registerOppslagService;

    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService,
                            RegisterOppslagSystemService registerOppslagService,
                            KodeverkService kodeverkService) {
        super(kodeverkService, null, avklartefaktaService);
        this.registerOppslagService = registerOppslagService;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());

        BrevDataA1 brevData = new BrevDataA1();
        brevData.yrkesgruppe = avklartefaktaService.hentYrkesGruppe(behandling.getId());
        brevData.utenlandskeVirksomheter = hentUtenlandskeVirksomheter();
        brevData.norskeVirksomheter = hentAlleNorskeAvklarteVirksomheter();
        brevData.selvstendigeForetak = hentAvklarteSelvstendigeForetakOrgnumre();
        brevData.bostedsadresse = hentBostedsadresse();
        brevData.arbeidssteder = hentArbeidssteder();
        brevData.person = person;

        return brevData;
    }

    protected List<Virksomhet> hentAlleNorskeAvklarteVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        return registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner).stream()
                .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), utfyllManglendeAdressefelter(org)))
                .collect(Collectors.toList());
    }

    private StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
    }
}
