package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SoeknadUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstraktDokumentDataBygger {
    protected final KodeverkService kodeverkService;
    protected final LovvalgsperiodeService lovvalgsperiodeService;
    protected final AvklartefaktaService avklartefaktaService;
    protected final AvklarteVirksomheterService avklarteVirksomheterService;

    protected PersonDokument person;
    protected SoeknadDokument søknad;

    protected Behandling behandling;

    private List<AvklartVirksomhet> norskeVirksomheter;
    private List<AvklartVirksomhet> utenlandskeVirksomheter;

    protected AbstraktDokumentDataBygger(KodeverkService kodeverkService,
                                         LovvalgsperiodeService lovvalgsperiodeService,
                                         AvklartefaktaService avklartefaktaService,
                                         AvklarteVirksomheterService avklarteVirksomheterService) {
        this.kodeverkService = kodeverkService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    protected Collection<AvklartVirksomhet> hentBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Collection<AvklartVirksomhet> bivirksomheter = new ArrayList<>();
        bivirksomheter.addAll(hentAlleNorskeVirksomheterMedAdresse());
        bivirksomheter.addAll(hentUtenlandskeVirksomheter());
        bivirksomheter.remove(hentHovedvirksomhet());
        return bivirksomheter;
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
        return arbeidssteder;
    }

    private List<Arbeidssted> hentFysiskearbeidssteder() {
        List<Arbeidssted> fysiskeArbeidssteder = søknad.arbeidUtland.stream()
            .map(au -> new FysiskArbeidssted(au.foretakNavn, au.foretakOrgnr, au.adresse))
            .collect(Collectors.toList());

        if (fysiskeArbeidssteder.isEmpty()) {
            hentUtenlandskeVirksomheter().stream()
                .filter(uv -> Boolean.TRUE.equals(uv.adresseErOgsåArbeidssted))
                .forEach(uv -> fysiskeArbeidssteder.add(utledArbeidsstedFraVirksomhet(uv)));
        }
        return fysiskeArbeidssteder;
    }

    private List<Arbeidssted> hentIkkeFysiskeArbeidssteder() {
        Collection<AvklartMaritimtArbeid> avklartMaritimtArbeid =
            avklartefaktaService.hentMaritimeAvklartfakta(behandling.getId());

        return avklartMaritimtArbeid.stream()
            .map(MaritimtArbeidssted::new)
            .collect(Collectors.toList());
    }

    private Arbeidssted utledArbeidsstedFraVirksomhet(AvklartVirksomhet virksomhet) {
        return new FysiskArbeidssted(virksomhet.navn, virksomhet.orgnr, (StrukturertAdresse)virksomhet.adresse);
    }

    protected List<AvklartVirksomhet> hentUtenlandskeVirksomheter() {
        if (utenlandskeVirksomheter == null) {
            // For nå har alltid kun et utenlandsk foretak.
            // Det er derfor ikke nødvendig med filtrering av avklarte foretak
            utenlandskeVirksomheter = søknad.foretakUtland.stream()
                .map(AvklartVirksomhet::new)
                .collect(Collectors.toList());
        }
        return utenlandskeVirksomheter;
    }

    protected List<AvklartVirksomhet> hentAlleNorskeVirksomheterMedAdresse() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        if (norskeVirksomheter == null) {
            norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, this::utfyllManglendeAdressefelter);
        }
        return norskeVirksomheter;
    }

    protected StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        if (StringUtils.isEmpty(adresse.gatenavn) || StringUtils.isEmpty(adresse.postnummer)) {
            adresse = org.getOrganisasjonDetaljer().hentStrukturertPostadresse();
        }
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
    }

    protected AvklartVirksomhet hentHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        if (!hentAlleNorskeVirksomheterMedAdresse().isEmpty()) {
            return hentAlleNorskeVirksomheterMedAdresse().iterator().next();
        } else {
            return hentUtenlandskeVirksomheter().iterator().next();
        }
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