package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.TilleggsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;

public abstract class AbstraktDokumentDataBygger {
    protected final KodeverkService kodeverkService;
    protected final LovvalgsperiodeService lovvalgsperiodeService;
    protected final AvklartefaktaService avklartefaktaService;

    protected PersonDokument person;
    protected SoeknadDokument søknad;

    protected Behandling behandling;

    protected AbstraktDokumentDataBygger(KodeverkService kodeverkService,
                                         LovvalgsperiodeService lovvalgsperiodeService,
                                         AvklartefaktaService avklartefaktaService) {
        this.kodeverkService = kodeverkService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
    }

    protected Bostedsadresse hentBostedsadresse() {
        Bostedsadresse adresse = person.bostedsadresse;
        adresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnr(), LocalDate.now()));
        return adresse;
    }

    protected List<Arbeidssted> hentArbeidssteder() {
        List<Arbeidssted> arbeidssteder = hentFysiskearbeidssteder();
        arbeidssteder.addAll(hentIkkeFysiskeArbeidssteder());

        if (!arbeidssteder.isEmpty()) {
            return arbeidssteder;
        }

        List<AvklartVirksomhet> utenlandskeVirksomheter = hentUtenlandskeVirksomheter();
        if (utenlandskeVirksomheter.size() != 1) {
            return Collections.emptyList();
        }

        // I Lev1 er det kun én utenlandsk arbeidsgiver.
        // Det er derfor ok å bruke dette navnet på fysisk arbeidssted
        AvklartVirksomhet utenlandskVirksomhet = utenlandskeVirksomheter.get(0);

        // Brevet krever alltid minst et arbeidssted - selv når det ikke er oppgitt i søknad
        return Collections.singletonList(utledArbeidsstedFraVirksomhet(utenlandskVirksomhet));
    }

    protected List<AvklartVirksomhet> hentUtenlandskeVirksomheter() {
        // For nå har alltid kun et utenlandsk foretak.
        // Det er derfor ikke nødvendig med filtrering av avklarte foretak
        return søknad.foretakUtland.stream()
            .map(AvklartVirksomhet::new)
            .collect(Collectors.toList());
    }

    private List<Arbeidssted> hentFysiskearbeidssteder() {
        return søknad.arbeidUtland.stream()
            .map(au -> new Arbeidssted(au.foretakNavn, au.foretakOrgnr, au.adresse))
            .collect(Collectors.toList());
    }

    private List<Arbeidssted> hentIkkeFysiskeArbeidssteder() {
        Set<Avklartefakta> avklartefaktaSet = avklartefaktaService.hentAlleAvklarteFlaggland(behandling.getId());

        return avklartefaktaSet.stream()
            .map(avklartefakta -> new Arbeidssted(avklartefakta.getSubjekt(), avklartefakta.getFakta(), Yrkesgrupper.SOKKEL_ELLER_SKIP))
            .collect(Collectors.toList());
    }

    private Arbeidssted utledArbeidsstedFraVirksomhet(AvklartVirksomhet virksomhet) {
        return new Arbeidssted(virksomhet.navn, virksomhet.orgnr, virksomhet.adresse.landKode);
    }

    public String hentArbeidsland(Lovvalgsperiode periode) throws FunksjonellException {
        // Artikklene 12.1, 12.2, 16.1 bruker oppholdsland
        Landkoder arbeidsland = Landkoder.valueOf(søknad.oppholdUtland.oppholdslandKoder.get(0));

        if (periode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1 &&
            periode.getTilleggsbestemmelse() == TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1) {
                Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
                arbeidsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART12_1 + ART11_4_1: Trenger flaggland"));
        }

        if (periode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A) {
            if (periode.getBestemmelse() == TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1) {
                Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
                arbeidsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART11_3A + ART11_4_1: Trenger bostedsland"));
            } else {
                if (søknad.maritimtArbeid.isEmpty()) {
                    throw new FunksjonellException("ART11_3A: Trenger Maritimt arbeid fra søknad");
                }
                arbeidsland = Landkoder.valueOf(søknad.maritimtArbeid.get(0).territorialfarvann);
            }
        }

        if (periode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2) {
            Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
            arbeidsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART11_4_2: Trenger flaggland"));
        }

        return arbeidsland.getBeskrivelse();
    }

    public String hentTrygdemyndighetsland(Lovvalgsperiode periode) throws FunksjonellException {
        // Artikklene 12.1, 12.2, 16.1 bruker oppholdsland
        Landkoder trygdemyndighetsland = Landkoder.valueOf(søknad.oppholdUtland.oppholdslandKoder.get(0));

        if (periode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1 &&
            periode.getTilleggsbestemmelse() == TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1) {
            Optional<Landkoder> avklarteFlaggland = avklartefaktaService.hentFlaggland(behandling.getId());
            trygdemyndighetsland = avklarteFlaggland.orElseThrow(() -> new FunksjonellException("ART12_1 + ART11_4_1: Trenger bostedsland"));
        }

        if (periode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A ||
            periode.getBestemmelse() == LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2) {
            trygdemyndighetsland = hentBostedsland();
        }

        return trygdemyndighetsland.getBeskrivelse();
    }

    public Landkoder hentBostedsland() {
        Optional<Landkoder> bostedslandOpt = avklartefaktaService.hentBostedland(behandling.getId());
        return bostedslandOpt.orElseGet(() -> Landkoder.valueOf(søknad.bosted.oppgittAdresse.landKode));
    }

    protected Collection<Lovvalgsperiode> hentLovvalgsperioder() throws TekniskException {
        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId());
        if (lovvalgsperioder.isEmpty()) {
            throw new TekniskException("Trenger minst en lovvalgsperiode");
        }

        return lovvalgsperioder;
    }

    protected Lovvalgsperiode hentLovvalgsperiode() throws FunksjonellException, TekniskException {
        Collection<Lovvalgsperiode> lovvalgsperioder = hentLovvalgsperioder();

        if (lovvalgsperioder.size() > 1) {
            throw new FunksjonellException("Forventer kun en lovvalgsperiode!");
        }

        return lovvalgsperioder.iterator().next();
    }
}