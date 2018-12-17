package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.EnumUtils;

import static no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilLovvalgBestemmelse;

public class BrevDataByggerA001 implements BrevDataBygger {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagSystemService registerOppslagService;
    private final KodeverkService kodeverkService;
    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    private Set<String> avklarteOrganisasjoner;
    private SoeknadDokument søknad;
    private PersonDokument person;
    private Behandling behandling;

    public BrevDataByggerA001(AvklartefaktaService avklartefaktaService,
                              RegisterOppslagSystemService registerOppslagService,
                              KodeverkService kodeverkService,
                              TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository,
                              UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository,
                              VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.kodeverkService = kodeverkService;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
        tidligereMedlemsperiodeRepository.findById_BehandlingId(behandling.getId());

        List<Lovvalgsperiode> lovvalgsperioder = hentLovvalgsperioder();
        Landkoder landkode = lovvalgsperioder.get(0).getUnntakFraLovvalgsland();

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
        brevData.tidligereLovvalgsperioder = hentTidligereLovvalgsperioder();
        brevData.ansettelsesperiode = hentAnsettelsesperiode();

        return brevData;
    }

    public UtenlandskMyndighet hentUtenlandsMyndighet(Landkoder landkode) throws TekniskException {
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

    private Bostedsadresse hentBostedsadresse() {
        Bostedsadresse adresse = person.bostedsadresse;
        adresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnr(), LocalDate.now()));
        return adresse;
    }

    private List<Virksomhet> hentAvklarteSelvstendigeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
                .collect(Collectors.toSet());

        organisasjonsnumre.retainAll(avklarteOrganisasjoner);
        return registerOppslagService.hentOrganisasjoner(organisasjonsnumre).stream()
                .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), org.getOrganisasjonDetaljer().hentUstrukturertForretningsadresse()))
                .collect(Collectors.toList());
    }

    private List<Virksomhet> hentAlleNorskeAvklarteVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        return registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner).stream()
                .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), org.getOrganisasjonDetaljer().hentUstrukturertForretningsadresse()))
                .collect(Collectors.toList());
    }

    private String hentUtenlandskIdent(Landkoder landKode) throws TekniskException {
        Optional<String> ident = søknad.personOpplysninger.utenlandskIdent.stream()
                .filter(utenlandskIdent -> utenlandskIdent.landKode != landKode.getKode())
                .map(utenlandskIdent -> utenlandskIdent.ident)
                .findFirst();

        return ident.orElseThrow(
                () -> new TekniskException("Fant ingen utenlands ident for valgt landKode: " + landKode.getKode()));
    }

    private List<Arbeidssted> hentArbeidssteder() throws TekniskException {
        List<Virksomhet> utenlandskeVirksomheter = hentUtenlandskeVirksomheter();
        if (utenlandskeVirksomheter.size() != 1) {
            throw new TekniskException("Krever utsendelse til én og kun én virksomhet i utlandet");
        }

        // I Lev1 er det kun én utenlandsk arbeidsgiver, og det er derfor ok å bruke dette navnet
        Virksomhet utenlandskVirksomhet = utenlandskeVirksomheter.get(0);

        List<Arbeidssted> arbeidssteder = hentFysiskearbeidssteder(utenlandskVirksomhet);
        arbeidssteder.addAll(hentIkkeFysiskeArbeidssteder());

        // Brevet krever alltid minst et arbeidssted - selv når det ikke er oppgitt i søknad
        if (arbeidssteder.isEmpty()) {
            Arbeidssted arbeidsstedMedForetaketsNavnOgLand = utledArbeidsstedFraUtenlandskForetak(utenlandskVirksomhet);
            arbeidssteder.add(arbeidsstedMedForetaketsNavnOgLand);
        }

        return arbeidssteder;
    }

    private List<Virksomhet> hentUtenlandskeVirksomheter() {
        return søknad.foretakUtland.stream()
                .map(Virksomhet::new)
                .collect(Collectors.toList());
    }

    private List<Arbeidssted> hentFysiskearbeidssteder(Virksomhet utenlandskVirksomhet) {
        return søknad.arbeidUtland.stream()
                .map(au -> new Arbeidssted(utenlandskVirksomhet.navn, au.adresse))
                .collect(Collectors.toList());
    }

    private List<Arbeidssted> hentIkkeFysiskeArbeidssteder() {
        List<Arbeidssted> ikkeFysiskArbeidssteder = new ArrayList<>();
        //TODO: hente ut maritimt arbeid
        return ikkeFysiskArbeidssteder;
    }

    private Arbeidssted utledArbeidsstedFraUtenlandskForetak(Virksomhet utenlandskVirksomhet) {
        return new Arbeidssted(utenlandskVirksomhet.navn,
                               utenlandskVirksomhet.adresse.landKode);
    }

    private List<Lovvalgsperiode> hentLovvalgsperioder() throws TekniskException {
        List<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeRepository.findByBehandlingsresultatId(behandling.getId());
        if (lovvalgsperioder.isEmpty()) {
            throw new TekniskException("Trenger minst en lovvalgsperiode");
        }

        Lovvalgsperiode valgtLovvalgsperiode = lovvalgsperioder.get(0);
        boolean lovvalgsperiodeIkkeGyldig = lovvalgsperioder.stream()
                .anyMatch(periode -> !validerPeriode(periode, valgtLovvalgsperiode));
        if (lovvalgsperiodeIkkeGyldig) {
            throw new TekniskException("A001 kan ha flere  lovvalgsperioder, men ikke med ulike Land eller unntak");
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

    private List<Lovvalgsperiode> hentTidligereLovvalgsperioder() throws TekniskException {
        Set<Long> utvalgtePeriodeIDer = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandling.getId()).stream()
                                            .map(utvalgtPeriode -> utvalgtPeriode.getId().getPeriodeId())
                                            .collect(Collectors.toSet());

        MedlemskapDokument medlemskapdokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        Set<Medlemsperiode> perioder = medlemskapdokument.getMedlemsperiode().stream()
                .filter(periode -> !utvalgtePeriodeIDer.contains(periode.id))
                .collect(Collectors.toSet());

        List<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();
        for (Medlemsperiode periode : perioder) {
            Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
            lovvalgsperiode.setFom(periode.periode.getFom());
            lovvalgsperiode.setTom(periode.periode.getTom());

            if (EnumUtils.isValidEnum(GrunnlagMedl.class, periode.getGrunnlagstype())) {
                GrunnlagMedl grunnlagMedlKode = GrunnlagMedl.valueOf(periode.getGrunnlagstype());
                lovvalgsperiode.setBestemmelse(tilLovvalgBestemmelse(grunnlagMedlKode));
            }
            else {
                lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ANNET);
            }

            tidligereLovvalgsperioder.add(lovvalgsperiode);
        }
        return tidligereLovvalgsperioder;
    }

    private Periode hentAnsettelsesperiode() throws TekniskException {
        ArbeidsforholdDokument arbeidsforholdDok = SaksopplysningerUtils.hentArbeidsforholdDokument(behandling);
        List<Periode> ansettelsesPerioder = arbeidsforholdDok.hentPerioder();
        if (ansettelsesPerioder.isEmpty()) {
            return null;
        }
        return ansettelsesPerioder.get(0);
    }
}
