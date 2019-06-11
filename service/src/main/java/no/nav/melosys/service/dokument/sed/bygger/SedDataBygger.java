package no.nav.melosys.service.dokument.sed.bygger;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.UtenlandskIdent;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.dto.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import no.nav.melosys.service.dokument.sed.mapper.VilkaarsresultatTilBegrunnelseMapper;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SedDataBygger extends AbstraktDokumentDataBygger {

    private final AvklarteVirksomheterService avklarteVirksomheterService;

    @Autowired
    public SedDataBygger(KodeverkService kodeverkService,
                         LovvalgsperiodeService lovvalgsperiodeService,
                         AvklartefaktaService avklartefaktaService,
                         AvklarteVirksomheterService avklarteVirksomheterService) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    public SedDataDto lag(Behandling behandling) throws TekniskException, FunksjonellException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        SedDataDto sedDataDto = lagPersonopplysninger(behandling);

        sedDataDto.setLovvalgsperioder(Collections.singletonList(hentLovvalgsperiodeDto()));

        sedDataDto.setTidligereLovvalgsperioder(hentTidligereLovvalgsperioderDto(behandling));

        sedDataDto.setMottakerLand(behandling.getFagsak().hentMyndighetLandkode().getKode());

        return sedDataDto;
    }

    public SedDataDto lagUtkast(Behandling behandling) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        return lagPersonopplysninger(behandling);
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

    private Adresse fraBostedsadresse(Bostedsadresse bostedsadresse) {
        Adresse adresse = new Adresse();
        adresse.setPoststed(bostedsadresse.getPoststed());
        adresse.setPostnr(bostedsadresse.getPostnr());
        adresse.setLand(bostedsadresse.getLand().getKode());
        adresse.setGateadresse(
            bostedsadresse.getGateadresse().getGatenavn() + " " +
                (bostedsadresse.getGateadresse().getHusnummer() == null ? "" : bostedsadresse.getGateadresse().getHusnummer()) +
                (bostedsadresse.getGateadresse().getHusbokstav() == null ? "" : bostedsadresse.getGateadresse().getHusbokstav())
        );
        return adresse;
    }

    private static Ident tilUtenlandskIdentDto(UtenlandskIdent ui) {
        Ident ident = new Ident();
        ident.setIdent(ui.ident);
        ident.setLandkode(ui.landkode);
        return ident;
    }

    private Arbeidssted mapArbeidssted(no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted arb) {
        Arbeidssted arbeidssted = new Arbeidssted();
        arbeidssted.setNavn(arb.navn);
        arbeidssted.setFysisk(arb.erFysisk());
        if (arb.erFysisk()) {
            arbeidssted.setAdresse(fraStrukturertAdresse(arb.adresse));
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

    private Lovvalgsperiode hentLovvalgsperiodeDto(no.nav.melosys.domain.Lovvalgsperiode lovvalgsperiode) {
        Lovvalgsperiode lovvalgsperiodeDto = new Lovvalgsperiode();
        lovvalgsperiodeDto.setFom(lovvalgsperiode.getFom());
        lovvalgsperiodeDto.setTom(lovvalgsperiode.getTom());
        lovvalgsperiodeDto.setLovvalgsland(lovvalgsperiode.getLovvalgsland() != null ? lovvalgsperiode.getLovvalgsland().getKode() : null);
        lovvalgsperiodeDto.setUnntakFraLovvalgsland(lovvalgsperiode.getUnntakFraLovvalgsland() != null ? lovvalgsperiode.getUnntakFraLovvalgsland().getKode() : null);
        lovvalgsperiodeDto.setBestemmelse(LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(lovvalgsperiode.getBestemmelse()));
        lovvalgsperiodeDto.setUnntaksBegrunnelse(hentUnntaksBegrunnelse(lovvalgsperiode));

        if (lovvalgsperiode.getUnntakFraBestemmelse() != null) {
            lovvalgsperiodeDto.setUnntakFraBestemmelse(LovvalgTilBestemmelseDtoMapper
                .mapMelosysLovvalgTilBestemmelseDto(lovvalgsperiode.getUnntakFraBestemmelse()));
        }

        return lovvalgsperiodeDto;
    }

    private Lovvalgsperiode hentLovvalgsperiodeDto() throws FunksjonellException, TekniskException {
        no.nav.melosys.domain.Lovvalgsperiode lovvalgsperiode = hentLovvalgsperiode();
        return hentLovvalgsperiodeDto(lovvalgsperiode);
    }

    private List<Lovvalgsperiode> hentTidligereLovvalgsperioderDto(Behandling behandling) throws TekniskException {
        List<Lovvalgsperiode> tidligereLovvalgsperioderDto = new ArrayList<>();
        Collection<no.nav.melosys.domain.Lovvalgsperiode> tidligereLovvalgsperioder =
            lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);

        tidligereLovvalgsperioder.stream()
            .map(this::hentLovvalgsperiodeDto)
            .forEach(tidligereLovvalgsperioderDto::add);

        return tidligereLovvalgsperioderDto;
    }

    private String hentUnntaksBegrunnelse(no.nav.melosys.domain.Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.getBehandlingsresultat() == null) {
            return null;
        }

        Set<Vilkaarsresultat> vilkaarsresultater = lovvalgsperiode.getBehandlingsresultat().getVilkaarsresultater();

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
