package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.VilkaarType;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataByggerA001 extends BrevDatabyggerBase implements BrevDataBygger {

    private final RegisterOppslagSystemService registerOppslagService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerA001(AvklartefaktaService avklartefaktaService,
                              RegisterOppslagSystemService registerOppslagService,
                              KodeverkService kodeverkService,
                              LovvalgsperiodeService lovvalgsperiodeService,
                              UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                              VilkaarsresultatRepository vilkaarsresultatRepository) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService);
        this.registerOppslagService = registerOppslagService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());

        Collection<Lovvalgsperiode> lovvalgsperioder = hentLovvalgsperioder();
        Landkoder landkode = lovvalgsperioder.iterator().next().getUnntakFraLovvalgsland();

        BrevDataA001 brevData = new BrevDataA001();
        brevData.personDokument = this.person;
        brevData.utenlandskMyndighet = hentUtenlandsMyndighet(landkode);
        brevData.arbeidsgivendeVirkomsheter = hentAlleNorskeAvklarteVirksomheter();
        brevData.selvstendigeVirksomheter = hentAvklarteSelvstendigeForetak();

        brevData.bostedsadresse = hentBostedsadresse();
        brevData.arbeidssteder = hentArbeidssteder();

        brevData.vilkårsresultat161 = hentVilkårsresultat();
        brevData.utenlandskIdent = hentUtenlandskIdent(landkode);
        brevData.lovvalgsperioder = lovvalgsperioder;
        brevData.tidligereLovvalgsperioder = lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);
        brevData.ansettelsesperiode = hentAnsettelsesperiode();

        return brevData;
    }

    private UtenlandskMyndighet hentUtenlandsMyndighet(Landkoder landkode) throws TekniskException {
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetRepository.findByLandkode(landkode);
        if (utenlandskMyndighet == null) {
            throw new TekniskException("Fant ingen utenlandsk myndighet for landkode: "+ landkode.getKode());
        }
        return utenlandskMyndighet;
    }

    private Vilkaarsresultat hentVilkårsresultat() throws TekniskException {
        List<Vilkaarsresultat> vilkaarresultater = vilkaarsresultatRepository.findByBehandlingsresultatId(behandling.getId());
        Optional<Vilkaarsresultat> vilkårsresultat161 = vilkaarresultater.stream()
                .filter(vilkaarsresultat -> vilkaarsresultat.getVilkaar() == VilkaarType.FO_883_2004_ART16_1)
                .findFirst();

        Vilkaarsresultat resultat = vilkårsresultat161.orElseThrow(() ->
                new TekniskException("Fant ingen vilkårbegrunnelse for FO_883_2004_ART16_1"));

        if (resultat.getBegrunnelser().isEmpty()) {
            throw new TekniskException("Brevet A001 trenger en begrunnelsekode for ART16_1");
        }
        return resultat;
    }

    private List<Virksomhet> hentAvklarteSelvstendigeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Set<String> organisasjonsnumre = hentAvklarteSelvstendigeForetakOrgnumre();
        return registerOppslagService.hentOrganisasjoner(organisasjonsnumre).stream()
                .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), org.getOrganisasjonDetaljer().hentUstrukturertForretningsadresse()))
                .collect(Collectors.toList());
    }

    protected List<Virksomhet> hentAlleNorskeAvklarteVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        return registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner).stream()
                .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), org.getOrganisasjonDetaljer().hentUstrukturertForretningsadresse()))
                .collect(Collectors.toList());
    }

    private Optional<String> hentUtenlandskIdent(Landkoder landKode) {
        return søknad.personOpplysninger.utenlandskIdent.stream()
                .filter(utenlandskIdent -> !utenlandskIdent.landKode.equals(landKode.getKode()))
                .map(utenlandskIdent -> utenlandskIdent.ident)
                .findFirst();
    }

    private Optional<Periode> hentAnsettelsesperiode() throws TekniskException {
        ArbeidsforholdDokument arbeidsforholdDok = SaksopplysningerUtils.hentArbeidsforholdDokument(behandling);

        if (avklarteOrganisasjoner.size() != 1) {
            throw new TekniskException("Kan ikke avgjøre ansettelsesperiode ved flere arbeidsforhold");
        }
        Stream<Periode> avklarteAnsettelsesPerioder =
                arbeidsforholdDok.hentAnsettelsesperioder(avklarteOrganisasjoner).stream();

        // Usikkert hva som er formålet med feltet i brevet.
        // Bestemt å bruke den seneste datoen for avklart arbeidsgiver inntil vi vet mer
        return avklarteAnsettelsesPerioder.max(Comparator.comparing(p -> p.getFom()));
    }
}