package no.nav.melosys.service.dokument.sed.bygger;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.UtenlandskIdent;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.dto.Lovvalgsperiode;
import no.nav.melosys.integrasjon.eessi.dto.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import no.nav.melosys.service.dokument.sed.mapper.VilkaarsresultatTilBegrunnelseMapper;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;

@Service
public class SedDataBygger extends AbstraktDokumentDataBygger {
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    @Autowired
    public SedDataBygger(KodeverkService kodeverkService,
                         LovvalgsperiodeService lovvalgsperiodeService,
                         AvklartefaktaService avklartefaktaService,
                         @Qualifier("system") AvklarteVirksomheterService avklarteVirksomheterService) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    public SedDataDto lag(Behandling behandling, Behandlingsresultat behandlingsresultat) throws TekniskException, FunksjonellException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        SedDataDto sedDataDto = lagPersonopplysninger(behandling);

        sedDataDto.setLovvalgsperioder(Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat)));

        sedDataDto.setTidligereLovvalgsperioder(lagTidligereLovvalgsperioderDto(behandling));

        sedDataDto.setMottakerLand(behandling.getFagsak().hentMyndighetLandkode().getKode());

        return sedDataDto;
    }

    public SedDataDto lagUtkast(Behandling behandling) throws TekniskException, FunksjonellException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        SedDataDto sedDataDto = lagPersonopplysninger(behandling);
        if (!lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId()).isEmpty()) {
            sedDataDto.setLovvalgsperioder(Collections.singletonList(lagLovvalgsperiodeDto(hentLovvalgsperiode())));
        } else {
            sedDataDto.setLovvalgsperioder(Collections.emptyList());
        }
        return sedDataDto;
    }

    private SedDataDto lagPersonopplysninger(Behandling behandling) throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException {
        SedDataDto sedDataDto = new SedDataDto();

        sedDataDto.setArbeidsgivendeVirksomheter(map(avklarteVirksomheterService.hentArbeidsgivere(behandling, this::utfyllManglendeAdressefelter)));

        sedDataDto.setArbeidssteder(hentArbeidssteder().stream()
            .map(this::mapArbeidssted).collect(Collectors.toList()));

        sedDataDto.setBostedsadresse(fraBostedsadresse(hentBostedsadresse()));

        sedDataDto.setBruker(hentBrukerFraPersonDokument(this.person));

        sedDataDto.setFamilieMedlem(this.person.familiemedlemmer.stream()
            .filter(f -> f.familierelasjon.equals(Familierelasjon.FARA) || f.familierelasjon.equals(Familierelasjon.MORA))
            .map(this::hentFamilieMedlem).collect(Collectors.toList()));

        sedDataDto.setSelvstendigeVirksomheter(map(avklarteVirksomheterService.hentSelvstendigeForetak(behandling, this::utfyllManglendeAdressefelter)));

        sedDataDto.setUtenlandskeVirksomheter(hentUtenlandskeVirksomheter().stream().map(
            this::tilUtenlandsVirksomhetDto).collect(Collectors.toList()));

        sedDataDto.setUtenlandskIdent(this.søknad.personOpplysninger.utenlandskIdent.stream()
            .map(SedDataBygger::tilUtenlandskIdentDto).collect(Collectors.toList()));

        return sedDataDto;
    }

    private List<Virksomhet> map(List<AvklartVirksomhet> avklarteArbeidsgivere) {
        return avklarteArbeidsgivere.stream()
            .map(aa -> new Virksomhet(aa.navn, aa.orgnr, fraStrukturertAdresse((StrukturertAdresse) aa.adresse)))
            .collect(Collectors.toList());
    }

    private Adresse fraBostedsadresse(StrukturertAdresse bostedsadresse) throws TekniskException {
        Adresse adresse = new Adresse();
        adresse.setPoststed(bostedsadresse.poststed);
        adresse.setPostnr(bostedsadresse.postnummer);
        adresse.setLand(tilIso3(bostedsadresse.landkode));
        adresse.setGateadresse(
            bostedsadresse.gatenavn + " " + Objects.toString(bostedsadresse.husnummer, "")
        );
        return adresse;
    }

    private static Ident tilUtenlandskIdentDto(UtenlandskIdent ui) {
        Ident ident = new Ident();
        ident.setIdent(ui.ident);
        ident.setLandkode(ui.landkode);
        return ident;
    }

    private Arbeidssted mapArbeidssted(no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted arb) {
        Arbeidssted arbeidssted = new Arbeidssted();
        arbeidssted.setNavn(arb.getNavn());
        arbeidssted.setFysisk(arb.erFysisk());
        if (arb.erFysisk()) {
            FysiskArbeidssted fysiskArbeidssted = (FysiskArbeidssted)arb;
            arbeidssted.setAdresse(fraStrukturertAdresse(fysiskArbeidssted.getAdresse()));
        } else {
            arbeidssted.setHjemmebase(null); //TODO ved ikke fysiske
        }
        return arbeidssted;
    }

    private Bruker hentBrukerFraPersonDokument(PersonDokument personDokument) {
        Bruker bruker = new Bruker();
        bruker.setEtternavn(personDokument.etternavn);
        bruker.setFornavn(personDokument.fornavn);
        bruker.setFnr(personDokument.fnr);
        bruker.setFoedseldato(personDokument.fødselsdato);
        bruker.setKjoenn(personDokument.kjønn.getKode());
        bruker.setStatsborgerskap(personDokument.statsborgerskap.getKode());

        return bruker;

    }

    private Adresse fraStrukturertAdresse(StrukturertAdresse strukturertAdresse) {
        Adresse adresse = new Adresse();
        adresse.setGateadresse(strukturertAdresse.gatenavn + (strukturertAdresse.husnummer == null ? "" : " " + strukturertAdresse.husnummer + " "));
        adresse.setLand(strukturertAdresse.landkode);
        adresse.setPostnr(strukturertAdresse.postnummer);
        adresse.setPoststed(strukturertAdresse.poststed);
        adresse.setRegion(strukturertAdresse.region);
        return adresse;
    }

    private Lovvalgsperiode lagLovvalgsperiodeDto(Behandlingsresultat behandlingsresultat) {

        if (!behandlingsresultat.getLovvalgsperioder().isEmpty()) {
            return lagLovvalgsperiodeDto(behandlingsresultat.hentValidertLovvalgsperiode());
        }

        Anmodningsperiode anmodningsperiode = behandlingsresultat.hentValidertAnmodningsperiode();
        return lagLovvalgsperiodeDto(anmodningsperiode, hentUnntaksBegrunnelse(behandlingsresultat));
    }

    private Lovvalgsperiode lagLovvalgsperiodeDto(Medlemskapsperiode periodeMedBestemmelse) {
        Lovvalgsperiode lovvalgsperiodeDto = new Lovvalgsperiode();
        lovvalgsperiodeDto.setFom(periodeMedBestemmelse.getFom());
        lovvalgsperiodeDto.setTom(periodeMedBestemmelse.getTom());
        lovvalgsperiodeDto.setLovvalgsland(periodeMedBestemmelse.getLovvalgsland() != null ? periodeMedBestemmelse.getLovvalgsland().getKode() : null);
        lovvalgsperiodeDto.setBestemmelse(LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(periodeMedBestemmelse.getBestemmelse()));
        return lovvalgsperiodeDto;
    }

    private Lovvalgsperiode lagLovvalgsperiodeDto(Anmodningsperiode anmodningsperiode, String unntaksBegrunnelse) {
        Lovvalgsperiode lovvalgsperiodeDto = lagLovvalgsperiodeDto(anmodningsperiode);

        lovvalgsperiodeDto.setUnntakFraLovvalgsland(anmodningsperiode.getUnntakFraLovvalgsland().getKode());
        lovvalgsperiodeDto.setUnntakFraBestemmelse(LovvalgTilBestemmelseDtoMapper
            .mapMelosysLovvalgTilBestemmelseDto(anmodningsperiode.getUnntakFraBestemmelse()));
        lovvalgsperiodeDto.setUnntaksBegrunnelse(unntaksBegrunnelse);

        return lovvalgsperiodeDto;
    }

    private List<Lovvalgsperiode> lagTidligereLovvalgsperioderDto(Behandling behandling)
        throws TekniskException {
        List<Lovvalgsperiode> tidligereLovvalgsperioderDto = new ArrayList<>();
        Collection<no.nav.melosys.domain.Lovvalgsperiode> tidligereLovvalgsperioder =
            lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);

        tidligereLovvalgsperioder.stream()
            .map(this::lagLovvalgsperiodeDto)
            .forEach(tidligereLovvalgsperioderDto::add);

        return tidligereLovvalgsperioderDto;
    }

    private String hentUnntaksBegrunnelse(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat == null) {
            return null;
        }
        Set<Vilkaarsresultat> vilkaarsresultater = behandlingsresultat.getVilkaarsresultater();

        return vilkaarsresultater == null ? null : vilkaarsresultater.stream()
            .map(VilkaarsresultatTilBegrunnelseMapper::tilEngelskBegrunnelseString)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining("\n\n"));
    }

    private String[] splitFulltNavn(String navn) {
        if (navn == null || navn.isEmpty()) return new String[2];
        else if (!navn.contains(" ")) return new String[]{navn, null};
        else return navn.split(" ", 2);
    }

    private FamilieMedlem hentFamilieMedlem(Familiemedlem f) {
        FamilieMedlem familieMedlem = new FamilieMedlem();
        String[] navn = splitFulltNavn(f.navn);
        familieMedlem.setFornavn(navn[0]);
        familieMedlem.setEtternavn(navn[1]);
        familieMedlem.setRelasjon(f.familierelasjon.equals(Familierelasjon.FARA) ? "FAR" : "MOR");
        return familieMedlem;
    }

    private Virksomhet tilUtenlandsVirksomhetDto(AvklartVirksomhet uVirksomhet) {
        Virksomhet virksomhet = new Virksomhet();
        virksomhet.setNavn(uVirksomhet.navn);
        virksomhet.setOrgnr(uVirksomhet.orgnr);
        virksomhet.setType("registrering"); //TODO - riktig?
        virksomhet.setAdresse(fraStrukturertAdresse((StrukturertAdresse) uVirksomhet.adresse));
        return virksomhet;
    }
}
