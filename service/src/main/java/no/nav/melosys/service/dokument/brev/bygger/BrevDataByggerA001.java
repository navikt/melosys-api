package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;
import java.util.stream.Stream;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_2;

public class BrevDataByggerA001 implements BrevDataBygger {
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    private BrevDataGrunnlag dataGrunnlag;
    private Behandling behandling;

    public BrevDataByggerA001(LovvalgsperiodeService lovvalgsperiodeService,
                              AnmodningsperiodeService anmodningsperiodeService,
                              UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                              VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        this.dataGrunnlag = dataGrunnlag;
        this.behandling = dataGrunnlag.getBehandling();

        Collection<Anmodningsperiode> anmodningsperioder = hentAnmodningsperioder();
        Landkoder landkode = anmodningsperioder.iterator().next().getUnntakFraLovvalgsland();

        BrevDataA001 brevData = new BrevDataA001();
        brevData.personDokument = dataGrunnlag.getPerson();
        brevData.utenlandskMyndighet = hentUtenlandsMyndighet(landkode);

        brevData.arbeidsgivendeVirksomheter =
            ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere(),
                            dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeArbeidsgivere());

        brevData.selvstendigeVirksomheter =
            ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendige(),
                            dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeSelvstendige());

        brevData.bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
        brevData.arbeidssteder = dataGrunnlag.getArbeidssteder().hentArbeidssteder();

        brevData.utenlandskIdent = hentUtenlandskIdent(landkode);
        brevData.anmodningsperioder = anmodningsperioder;
        brevData.tidligereLovvalgsperioder = lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);
        brevData.ansettelsesperiode = hentAnsettelsesperiode();

        Vilkaarsresultat art16Vilkaar = hentVilkårsresultat();
        Set<VilkaarBegrunnelse> art16VilkaarBegrunnelser = art16Vilkaar.getBegrunnelser();
        if (harVilkaarForArtikkel12(behandling.getId())) {
            brevData.anmodningBegrunnelser = art16VilkaarBegrunnelser;
            brevData.anmodningUtenArt12Begrunnelser = Collections.emptySet();
        } else {
            brevData.anmodningBegrunnelser = Collections.emptySet();
            brevData.anmodningUtenArt12Begrunnelser = art16VilkaarBegrunnelser;
        }

        if (harSærligGrunn(art16VilkaarBegrunnelser)) {
            brevData.anmodningFritekst = art16Vilkaar.getBegrunnelseFritekst();
        }

        return brevData;
    }

    private boolean harSærligGrunn(Set<VilkaarBegrunnelse> art16VilkaarBegrunnelser) {
        return art16VilkaarBegrunnelser.stream()
            .map(VilkaarBegrunnelse::getKode)
            .anyMatch("SAERLIG_GRUNN"::equals);
    }

    private boolean harVilkaarForArtikkel12(long behandlingID) {
        Optional<Vilkaarsresultat> art121Vilkaar = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandlingID, FO_883_2004_ART12_1);
        Optional<Vilkaarsresultat> art122Vilkaar = vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandlingID, FO_883_2004_ART12_2);
        return art121Vilkaar.isPresent() || art122Vilkaar.isPresent();
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
        return dataGrunnlag.getSøknad().personOpplysninger.utenlandskIdent.stream()
            .filter(utenlandskIdent -> utenlandskIdent.landkode.equals(landkode.getKode()))
            .map(utenlandskIdent -> utenlandskIdent.ident)
            .findFirst();
    }

    private Collection<Anmodningsperiode> hentAnmodningsperioder() throws FunksjonellException {
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.hentAnmodningsperioder(behandling.getId());
        return validerAnmodningsperioder(anmodningsperioder);
    }

    private Collection<Anmodningsperiode> validerAnmodningsperioder(Collection<Anmodningsperiode> anmodningsperioder)
        throws FunksjonellException {
        if (CollectionUtils.isEmpty(anmodningsperioder)) {
            throw new FunksjonellException("Minst en anmodningsperiode trengs for å kunne sende A001.");
        }
        final Anmodningsperiode referansePeriode = anmodningsperioder.iterator().next();
        boolean lovvalgsperiodeIkkeGyldig = anmodningsperioder.stream()
            .anyMatch(periode -> !referansePeriode.gjelderSammeLandOgUnntakSom(periode));
        if (lovvalgsperiodeIkkeGyldig) {
            throw new FunksjonellException("Flere anmodningsperioder støttes, men ikke med ulike land eller unntak.");
        }
        return anmodningsperioder;
    }

    private Optional<Periode> hentAnsettelsesperiode() throws TekniskException {
        ArbeidsforholdDokument arbeidsforholdDok = SaksopplysningerUtils.hentArbeidsforholdDokument(behandling);

        Set<String> avklarteOrgnumre = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivendeOrgnumre();
        Stream<Periode> avklarteAnsettelsesPerioder =
            arbeidsforholdDok.hentAnsettelsesperioder(avklarteOrgnumre).stream();

        // Usikkert hva som er formålet med feltet i brevet.
        // Bestemt å bruke den seneste datoen for avklart arbeidsgiver inntil vi vet mer
        return avklarteAnsettelsesPerioder.max(Comparator.comparing(Periode::getFom));
    }
}