package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
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
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.apache.commons.collections4.CollectionUtils;

public class BrevDataByggerA001 extends AbstraktDokumentDataBygger implements BrevDataBygger {
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerA001(AnmodningsperiodeService anmodningsperiodeService,
                              AvklartefaktaService avklartefaktaService,
                              AvklarteVirksomheterService avklarteVirksomheterService,
                              KodeverkService kodeverkService,
                              LovvalgsperiodeService lovvalgsperiodeService,
                              UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                              VilkaarsresultatRepository vilkaarsresultatRepository) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService, avklarteVirksomheterService);
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        Collection<Anmodningsperiode> anmodningsperioder = hentAnmodningsperioder();
        Landkoder landkode = anmodningsperioder.iterator().next().getUnntakFraLovvalgsland();

        BrevDataA001 brevData = new BrevDataA001();
        brevData.personDokument = this.person;
        brevData.utenlandskMyndighet = hentUtenlandsMyndighet(landkode);
        brevData.arbeidsgivendeVirkomsheter = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling, this::utfyllManglendeAdressefelter);
        brevData.selvstendigeVirksomheter = avklarteVirksomheterService.hentNorskeSelvstendigeForetak(behandling, this::utfyllManglendeAdressefelter);

        brevData.bostedsadresse = hentBostedsadresse();
        brevData.arbeidssteder = hentArbeidssteder();

        brevData.vilkårsresultat161 = hentVilkårsresultat();
        brevData.utenlandskIdent = hentUtenlandskIdent(landkode);
        brevData.anmodningsperioder = anmodningsperioder;
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

        Set<String> avklarteOrgnumre = avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
        Stream<Periode> avklarteAnsettelsesPerioder =
            arbeidsforholdDok.hentAnsettelsesperioder(avklarteOrgnumre).stream();

        // Usikkert hva som er formålet med feltet i brevet.
        // Bestemt å bruke den seneste datoen for avklart arbeidsgiver inntil vi vet mer
        return avklarteAnsettelsesPerioder.max(Comparator.comparing(Periode::getFom));
    }
}