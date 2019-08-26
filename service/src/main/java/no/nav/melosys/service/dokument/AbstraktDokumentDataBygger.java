package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
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

    protected List<Arbeidssted> hentArbeidssteder() throws TekniskException {
        List<Arbeidssted> arbeidssteder = hentFysiskearbeidssteder();
        arbeidssteder.addAll(hentMaritimeArbeidssteder());
        return arbeidssteder;
    }

    private List<Arbeidssted> hentFysiskearbeidssteder() throws TekniskException {
        List<Arbeidssted> fysiskeArbeidssteder = søknad.arbeidUtland.stream()
            .map(au -> new FysiskArbeidssted(au.foretakNavn, au.foretakOrgnr, au.adresse))
            .collect(Collectors.toList());

        if (fysiskeArbeidssteder.isEmpty()) {
            hentUtenlandskeVirksomheter().stream()
                .filter(uv -> uv.adresseErOgsåArbeidssted)
                .forEach(uv -> fysiskeArbeidssteder.add(utledArbeidsstedFraVirksomhet(uv)));
        }
        return fysiskeArbeidssteder;
    }

    private List<MaritimtArbeidssted> hentMaritimeArbeidssteder() {
        Map<String, AvklartMaritimtArbeid> avklartMaritimtArbeid =
            avklartefaktaService.hentAlleMaritimeAvklartfakta(behandling.getId());

        // Arbeidssted for maritimt arbeid benytter foretakNavn og foretakOrgnr fra søknad, og arbeidsland fra avklartfakta
        return søknad.maritimtArbeid.stream()
            .map(ma -> lagMaritimtArbeidssted(ma, avklartMaritimtArbeid))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private MaritimtArbeidssted lagMaritimtArbeidssted(MaritimtArbeid maritimtArbeid, Map<String, AvklartMaritimtArbeid> alleAvklarteMaritimeArbeid) {
            AvklartMaritimtArbeid avklartMaritimtArbeid = alleAvklarteMaritimeArbeid.get(maritimtArbeid.enhetNavn);
            if (avklartMaritimtArbeid != null) {
                return new MaritimtArbeidssted(maritimtArbeid, avklartMaritimtArbeid);
            }
            return null;
    }

    private Arbeidssted utledArbeidsstedFraVirksomhet(AvklartVirksomhet virksomhet) {
        return new FysiskArbeidssted(virksomhet.navn, virksomhet.orgnr, (StrukturertAdresse)virksomhet.adresse);
    }

    protected List<AvklartVirksomhet> hentUtenlandskeVirksomheter() throws TekniskException {
        if (utenlandskeVirksomheter == null) {
            utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
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

    protected Collection<AvklartVirksomhet> hentBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Collection<AvklartVirksomhet> bivirksomheter = new ArrayList<>();
        bivirksomheter.addAll(hentAlleNorskeVirksomheterMedAdresse());
        bivirksomheter.addAll(hentUtenlandskeVirksomheter());
        bivirksomheter.remove(hentHovedvirksomhet());
        return bivirksomheter;
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