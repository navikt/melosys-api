package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.domain.util.SoeknadUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

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

    protected StrukturertAdresse hentBostedsadresse() throws TekniskException {
        StrukturertAdresse bostedsadresse = SoeknadUtils.hentBostedsadresse(søknad);
        if (bostedsadresse == null) {
            bostedsadresse = hentBostedsadresseFraRegister();
        }
        return bostedsadresse;
    }

    private StrukturertAdresse hentBostedsadresseFraRegister() throws TekniskException {
        Bostedsadresse bostedsadresse = person.bostedsadresse;
        if (StringUtils.isEmpty(bostedsadresse.getLand().getKode())) {
            throw new TekniskException("Bostedsadressen finnes ikke eller mangler landkode");
        }
        bostedsadresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, bostedsadresse.getPostnr(), LocalDate.now()));
        return StrukturertAdresse.av(bostedsadresse);
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

    protected StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        if (StringUtils.isEmpty(adresse.gatenavn) || StringUtils.isEmpty(adresse.postnummer)) {
            adresse = org.getOrganisasjonDetaljer().hentStrukturertPostadresse();
        }
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
    }

    private List<Arbeidssted> hentFysiskearbeidssteder() {
        return søknad.arbeidUtland.stream()
            .map(au -> new Arbeidssted(au.foretakNavn, au.foretakOrgnr, au.adresse))
            .collect(Collectors.toList());
    }

    private List<Arbeidssted> hentIkkeFysiskeArbeidssteder() {
        Set<Avklartefakta> avklartefaktaSet = avklartefaktaService.hentAlleAvklarteArbeidsland(behandling.getId());

        return avklartefaktaSet.stream()
            .map(avklartefakta -> new Arbeidssted(avklartefakta.getSubjekt(), avklartefakta.getFakta(), Yrkesgrupper.SOKKEL_ELLER_SKIP))
            .collect(Collectors.toList());
    }

    private Arbeidssted utledArbeidsstedFraVirksomhet(AvklartVirksomhet virksomhet) {
        return new Arbeidssted(virksomhet.navn, virksomhet.orgnr, virksomhet.adresse.landkode);
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