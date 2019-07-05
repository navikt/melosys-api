package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BrevDataByggerA001 extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerA001(AvklartefaktaService avklartefaktaService,
                              AvklarteVirksomheterService avklarteVirksomheterService,
                              KodeverkService kodeverkService,
                              LovvalgsperiodeService lovvalgsperiodeService,
                              UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                              VilkaarsresultatRepository vilkaarsresultatRepository) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        Collection<Lovvalgsperiode> lovvalgsperioder = hentLovvalgsperioder();
        Landkoder landkode = lovvalgsperioder.iterator().next().getUnntakFraLovvalgsland();

        BrevDataA001 brevData = new BrevDataA001();
        brevData.personDokument = this.person;
        brevData.utenlandskMyndighet = hentUtenlandsMyndighet(landkode);
        brevData.arbeidsgivendeVirkomsheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, this::utfyllManglendeAdressefelter);
        brevData.selvstendigeVirksomheter = avklarteVirksomheterService.hentSelvstendigeForetak(behandling, this::utfyllManglendeAdressefelter);

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
        return  utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Fant ingen utenlandsk myndighet for landkode: " + landkode.getKode()));
    }

    private Vilkaarsresultat hentVilkårsresultat() throws TekniskException {
        Optional<Vilkaarsresultat> vilkårsresultat = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandling.getId(), Vilkaar.FO_883_2004_ART16_1);
        Vilkaarsresultat resultat = vilkårsresultat.orElseThrow(() ->
            new TekniskException("Fant ingen vilkårbegrunnelse for FO_883_2004_ART16_1"));

        if (resultat.getBegrunnelser().isEmpty()) {
            throw new TekniskException("Brevet A001 trenger en begrunnelsekode for ART16_1");
        }
        return resultat;
    }

    private Optional<String> hentUtenlandskIdent(Landkoder landkode) {
        return søknad.personOpplysninger.utenlandskIdent.stream()
            .filter(utenlandskIdent -> utenlandskIdent.landkode.equals(landkode.getKode()))
            .map(utenlandskIdent -> utenlandskIdent.ident)
            .findFirst();
    }

    @Override
    protected Collection<Lovvalgsperiode> hentLovvalgsperioder() throws TekniskException {
        Collection<Lovvalgsperiode> lovvalgsperioder = super.hentLovvalgsperioder();

        Lovvalgsperiode valgtLovvalgsperiode = lovvalgsperioder.iterator().next();
        boolean lovvalgsperiodeIkkeGyldig = lovvalgsperioder.stream()
            .anyMatch(periode -> !validerPeriode(periode, valgtLovvalgsperiode));
        if (lovvalgsperiodeIkkeGyldig) {
            throw new TekniskException("Flere lovvalgsperioder støttes, men ikke med ulike Land eller unntak");
        }

        return lovvalgsperioder;
    }

    private boolean validerPeriode(Lovvalgsperiode p1, Lovvalgsperiode p2) {
        return p1.getLovvalgsland() == p2.getLovvalgsland() &&
            p1.getUnntakFraBestemmelse() != null &&
            p1.getUnntakFraBestemmelse() == p2.getUnntakFraBestemmelse() &&
            p1.getUnntakFraLovvalgsland() != null &&
            p1.getUnntakFraLovvalgsland() == p2.getUnntakFraLovvalgsland();
    }

    private Optional<Periode> hentAnsettelsesperiode() throws TekniskException {
        ArbeidsforholdDokument arbeidsforholdDok = SaksopplysningerUtils.hentArbeidsforholdDokument(behandling);

        Set<String> avklarteOrgnumre = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
        if (avklarteOrgnumre.size() != 1) {
            throw new TekniskException("Kan ikke avgjøre ansettelsesperiode ved flere arbeidsforhold");
        }

        Stream<Periode> avklarteAnsettelsesPerioder =
            arbeidsforholdDok.hentAnsettelsesperioder(avklarteOrgnumre).stream();

        // Usikkert hva som er formålet med feltet i brevet.
        // Bestemt å bruke den seneste datoen for avklart arbeidsgiver inntil vi vet mer
        return avklarteAnsettelsesPerioder.max(Comparator.comparing(Periode::getFom));
    }
}