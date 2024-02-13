package no.nav.melosys.service.dokument.brev.bygger;

import java.util.*;
import java.util.stream.Stream;

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
import org.apache.commons.collections4.map.SingletonMap;

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
        brevData.setPersondata(dataGrunnlag.getPerson());
        brevData.setUtenlandskMyndighet(utenlandskMyndighetService.hentUtenlandskMyndighet(landkode));

        brevData.setArbeidsgivendeVirksomheter(ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere(),
            dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeArbeidsgivere()));

        brevData.setSelvstendigeVirksomheter(ListUtils.union(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendige(),
            dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeSelvstendige()));

        var adresseOgType = hentBostedsadresseOgTypeKode();
        brevData.setBostedsadresse(adresseOgType.getValue());
        brevData.setBostedsadresseTypeKode(adresseOgType.getKey());

        brevData.setArbeidssteder(dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder());

        brevData.setUtenlandskIdent(hentUtenlandskIdent(landkode).get());
        brevData.setAnmodningsperioder(anmodningsperioder);
        brevData.setTidligereLovvalgsperioder(lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling));
        brevData.setAnsettelsesperiode(hentAnsettelsesperiode().get());

        Vilkaarsresultat art16Vilkaar = hentVilkårsresultat();
        Set<VilkaarBegrunnelse> art16VilkaarBegrunnelser = art16Vilkaar.getBegrunnelser();
        if (vilkaarsresultatService.harVilkaarForArtikkel12(behandling.getId())) {
            brevData.setAnmodningBegrunnelser(art16VilkaarBegrunnelser);
            brevData.setAnmodningUtenArt12Begrunnelser(Collections.emptySet());
        } else {
            brevData.setAnmodningBegrunnelser(Collections.emptySet());
            brevData.setAnmodningUtenArt12Begrunnelser(art16VilkaarBegrunnelser);
        }

        if (harSærligGrunn(art16VilkaarBegrunnelser)) {
            brevData.setAnmodningFritekstBegrunnelse(art16Vilkaar.getBegrunnelseFritekstEessi());
        }
        brevData.setYtterligereInformasjon(dataGrunnlag.getBrevbestilling().getYtterligereInformasjon());
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
        return dataGrunnlag.getMottatteOpplysningerData().personOpplysninger.getUtenlandskIdent().stream()
            .filter(utenlandskIdent -> utenlandskIdent.getLandkode().equals(landkode.getKode()))
            .map(utenlandskIdent -> utenlandskIdent.getIdent())
            .findFirst();
    }

    private SingletonMap<BostedsadresseTypeKode, StrukturertAdresse> hentBostedsadresseOgTypeKode() {
        var bostedsadresse = dataGrunnlag.getBostedGrunnlag().finnBostedsadresse();
        if (bostedsadresse.isPresent()) {
            return new SingletonMap<>(BostedsadresseTypeKode.BOSTEDSLAND, bostedsadresse.get());
        }

        var kontaktadresse = dataGrunnlag.getBostedGrunnlag().finnKontaktadresse();
        if (kontaktadresse.isPresent()) {
            return new SingletonMap<>(BostedsadresseTypeKode.KONTAKTADRESSE, kontaktadresse.get());
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
        ArbeidsforholdDokument arbeidsforholdDok = behandling.finnArbeidsforholdDokument()
            .orElseThrow(() -> new TekniskException("Finner ikke arbeidsforhold"));

        Set<String> avklarteOrgnumre = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivendeOrgnumre();
        Stream<Periode> avklarteAnsettelsesPerioder =
            arbeidsforholdDok.hentAnsettelsesperioder(avklarteOrgnumre).stream();

        // Usikkert hva som er formålet med feltet i brevet.
        // Bestemt å bruke den seneste datoen for avklart arbeidsgiver inntil vi vet mer
        return avklarteAnsettelsesPerioder.max(Comparator.comparing(Periode::getFom));
    }
}
