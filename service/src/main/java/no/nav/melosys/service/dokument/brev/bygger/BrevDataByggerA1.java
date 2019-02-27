package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataByggerA1 extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private final RegisterOppslagSystemService registerOppslagService;

    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService,
                            RegisterOppslagSystemService registerOppslagService,
                            KodeverkService kodeverkService) {
        super(kodeverkService, null, avklartefaktaService, registerOppslagService);
        this.registerOppslagService = registerOppslagService;
    }

    protected Function<OrganisasjonDokument, Adresse> adresseformaterer = this::utfyllManglendeAdressefelter;

    private StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
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
        brevData.norskeVirksomheter = hentAlleNorskeAvklarteVirksomheter(adresseformaterer);
        brevData.selvstendigeForetak = hentAvklarteSelvstendigeForetakOrgnumre();
        brevData.bostedsadresse = hentBostedsadresse();
        brevData.arbeidssteder = hentArbeidssteder();
        brevData.person = person;

        if (brevData.norskeVirksomheter.isEmpty()) {
            throw new TekniskException("Trenger minst en valgt norsk virksomhet for ART12.1");
        }

        // Lev1 kun norske virksomheter som hovedvirksomhet (og kun én)
        brevData.hovedvirksomhet = brevData.norskeVirksomheter.get(0);
        avklarSelvstendigForetakVirksomhet(brevData.hovedvirksomhet);
        return brevData;
    }
}
