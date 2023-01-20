package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;
import java.util.stream.Stream;

import kotlin.Pair;
import no.nav.dok.melosysbrev._000115.BostedsadresseTypeKode;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

public class BrevDataByggerA001 implements BrevDataBygger {
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final VilkaarsresultatService vilkaarsresultatService;

    private BrevDataGrunnlag dataGrunnlag;
    private Behandling behandling;

    public BrevDataByggerA001(LovvalgsperiodeService lovvalgsperiodeService,
                              AnmodningsperiodeService anmodningsperiodeService,
                              UtenlandskMyndighetService utenlandskMyndighetRepository,
                              VilkaarsresultatService vilkaarsresultatService) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.utenlandskMyndighetService = utenlandskMyndighetRepository;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        this.dataGrunnlag = dataGrunnlag;
        this.behandling = dataGrunnlag.getBehandling();

        Collection<Anmodningsperiode> anmodningsperioder = hentAnmodningsperioder();
        Land_iso2 landkode = Land_iso2.valueOf(anmodningsperioder.iterator().next().getUnntakFraLovvalgsland().getKode());

        BrevDataA001 brevData = new BrevDataA001();
        brevData.persondata = dataGrunnlag.getPerson();
        brevData.utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);

        brevData.arbeidsgivendeVirksomheter =
            ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere(),
                dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeArbeidsgivere());

        brevData.selvstendigeVirksomheter =
            ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendige(),
                dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeSelvstendige());

        var adresseOgType = hentBostedsadresseOgTypeKode();
        brevData.bostedsadresse = adresseOgType.getFirst();
        brevData.bostedsadresseTypeKode = adresseOgType.getSecond();

        brevData.arbeidssteder = dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder();

        brevData.utenlandskIdent = hentUtenlandskIdent(landkode);
        brevData.anmodningsperioder = anmodningsperioder;
        brevData.tidligereLovvalgsperioder = lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);
        brevData.ansettelsesperiode = hentAnsettelsesperiode();

        Vilkaarsresultat art16Vilkaar = hentVilkårsresultat();
        Set<VilkaarBegrunnelse> art16VilkaarBegrunnelser = art16Vilkaar.getBegrunnelser();
        if (vilkaarsresultatService.harVilkaarForArtikkel12(behandling.getId())) {
            brevData.anmodningBegrunnelser = art16VilkaarBegrunnelser;
            brevData.anmodningUtenArt12Begrunnelser = Collections.emptySet();
        } else {
            brevData.anmodningBegrunnelser = Collections.emptySet();
            brevData.anmodningUtenArt12Begrunnelser = art16VilkaarBegrunnelser;
        }

        if (harSærligGrunn(art16VilkaarBegrunnelser)) {
            brevData.anmodningFritekstBegrunnelse = art16Vilkaar.getBegrunnelseFritekstEessi();
        }
        brevData.ytterligereInformasjon = dataGrunnlag.getBrevbestilling().getYtterligereInformasjon();
        return brevData;
    }

    private boolean harSærligGrunn(Set<VilkaarBegrunnelse> art16VilkaarBegrunnelser) {
        return art16VilkaarBegrunnelser.stream()
            .map(VilkaarBegrunnelse::getKode)
            .anyMatch("SAERLIG_GRUNN"::equals);
    }

    private Vilkaarsresultat hentVilkårsresultat() {
        Optional<Vilkaarsresultat> vilkårsresultat = vilkaarsresultatService.finnVilkaarsresultat(behandling.getId(), Vilkaar.FO_883_2004_ART16_1);
        Vilkaarsresultat resultat = vilkårsresultat.orElseThrow(() ->
            new TekniskException("Fant ingen vilkårbegrunnelse for FO_883_2004_ART16_1"));

        if (resultat.getBegrunnelser().isEmpty()) {
            throw new TekniskException("Brevet A001 trenger en begrunnelsekode for ART16_1");
        }
        return resultat;
    }

    private Optional<String> hentUtenlandskIdent(Land_iso2 landkode) {
        return dataGrunnlag.getMottatteOpplysningerData().personOpplysninger.utenlandskIdent.stream()
            .filter(utenlandskIdent -> utenlandskIdent.landkode.equals(landkode.getKode()))
            .map(utenlandskIdent -> utenlandskIdent.ident)
            .findFirst();
    }

    private Pair<StrukturertAdresse, BostedsadresseTypeKode> hentBostedsadresseOgTypeKode() {
        var bostedsadresse = dataGrunnlag.getBostedGrunnlag().finnBostedsadresse();
        if (bostedsadresse.isPresent()) {
            return new Pair<>(bostedsadresse.get(), BostedsadresseTypeKode.BOSTEDSLAND);
        }

        var kontaktadresse = dataGrunnlag.getBostedGrunnlag().finnKontaktadresse();
        if (kontaktadresse.isPresent()) {
            return new Pair<>(kontaktadresse.get(), BostedsadresseTypeKode.KONTAKTADRESSE);
        }

        throw new FunksjonellException("Finner verken bostedsadresse eller kontaktadresse");
    }

    private Collection<Anmodningsperiode> hentAnmodningsperioder() {
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.hentAnmodningsperioder(behandling.getId());
        return validerAnmodningsperioder(anmodningsperioder);
    }

    private Collection<Anmodningsperiode> validerAnmodningsperioder(Collection<Anmodningsperiode> anmodningsperioder) {
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

    private Optional<Periode> hentAnsettelsesperiode() {
        ArbeidsforholdDokument arbeidsforholdDok = behandling.hentArbeidsforholdDokument();

        Set<String> avklarteOrgnumre = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivendeOrgnumre();
        Stream<Periode> avklarteAnsettelsesPerioder =
            arbeidsforholdDok.hentAnsettelsesperioder(avklarteOrgnumre).stream();

        // Usikkert hva som er formålet med feltet i brevet.
        // Bestemt å bruke den seneste datoen for avklart arbeidsgiver inntil vi vet mer
        return avklarteAnsettelsesPerioder.max(Comparator.comparing(Periode::getFom));
    }
}
