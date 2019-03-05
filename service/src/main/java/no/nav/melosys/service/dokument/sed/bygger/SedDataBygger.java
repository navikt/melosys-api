package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.UtenlandskIdent;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.dto.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import no.nav.melosys.service.dokument.sed.mapper.VilkaarsresultatTilBegrunnelseMapper;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataBygger extends AbstraktDokumentDataBygger {

    private final AvklarteVirksomheterService avklarteVirksomheterService;

    public SedDataBygger(KodeverkService kodeverkService,
                         LovvalgsperiodeService lovvalgsperiodeService,
                         AvklartefaktaService avklartefaktaService,
                         AvklarteVirksomheterService avklarteVirksomheterService) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    Function<OrganisasjonDokument, no.nav.melosys.domain.dokument.felles.Adresse> adresseformaterer = this::utfyllManglendeAdressefelter;
    private StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
    }

    public SedDataDto lag(Behandling behandling) throws TekniskException, FunksjonellException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        SedDataDto sedDataDto = new SedDataDto();

        sedDataDto.setArbeidsgivendeVirksomheter(map(avklarteVirksomheterService.hentArbeidsgivere(behandling, adresseformaterer)));

        sedDataDto.setArbeidssteder(hentArbeidssteder().stream()
            .map(this::mapArbeidssted).collect(Collectors.toList()));

        sedDataDto.setBostedsadresse(fraBostedsadresse(hentBostedsadresse()));

        sedDataDto.setBruker(hentBrukerFraPersonDokument(this.person));

        sedDataDto.setEgenAnsatt(this.person.erEgenAnsatt);

        sedDataDto.setFamilieMedlem(this.person.familiemedlemmer.stream()
            .filter(f -> f.familierelasjon.equals(Familierelasjon.FARA) || f.familierelasjon.equals(Familierelasjon.MORA))
            .map(this::hentFamilieMedlem).collect(Collectors.toList()));

        sedDataDto.setLovvalgsperioder(Collections.singletonList(hentLovvalgsperiodeDto()));

        sedDataDto.setSelvstendigeVirksomheter(map(avklarteVirksomheterService.hentSelvstendigeForetak(behandling, adresseformaterer)));

        sedDataDto.setUtenlandskeVirksomheter(hentUtenlandskeVirksomheter().stream().map(
            this::tilUtenlandsVirksomhetDto).collect(Collectors.toList()));//TODO - riktig?

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
                (bostedsadresse.getGateadresse().getGatenummer() == null ? "" : bostedsadresse.getGateadresse().getGatenummer()) +
                (bostedsadresse.getGateadresse().getHusbokstav() == null ? "" : bostedsadresse.getGateadresse().getHusbokstav())
        );
        return adresse;
    }

    private static Ident tilUtenlandskIdentDto(UtenlandskIdent ui) {
        Ident ident = new Ident();
        ident.setIdent(ui.ident);
        ident.setLandkode(ui.landKode);
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
        adresse.setLand(strukturertAdresse.landKode);
        adresse.setPostnr(strukturertAdresse.postnummer);
        adresse.setPoststed(strukturertAdresse.poststed);
        return adresse;
    }

    private Lovvalgsperiode hentLovvalgsperiodeDto() throws FunksjonellException {
        no.nav.melosys.domain.Lovvalgsperiode lovvalgsperiode = hentLovvalgsperiode();
        Lovvalgsperiode lovvalgsperiodeDto = new Lovvalgsperiode();
        lovvalgsperiodeDto.setFom(lovvalgsperiode.getFom());
        lovvalgsperiodeDto.setTom(lovvalgsperiode.getTom());
        lovvalgsperiodeDto.setLandkode(lovvalgsperiode.getLovvalgsland().getKode());
        lovvalgsperiodeDto.setBestemmelse(LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(lovvalgsperiode.getBestemmelse()));
        lovvalgsperiodeDto.setBeskrivelse(hentBeskrivelse(lovvalgsperiode));

        if (lovvalgsperiode.getUnntakFraBestemmelse() != null) {
            lovvalgsperiodeDto.setUnntakFraBestemmelse(LovvalgTilBestemmelseDtoMapper
                .mapMelosysLovvalgTilBestemmelseDto(lovvalgsperiode.getUnntakFraBestemmelse()));
        }

        return lovvalgsperiodeDto;
    }

    private String hentBeskrivelse(no.nav.melosys.domain.Lovvalgsperiode lovvalgsperiode) {
        Set<Vilkaarsresultat> vilkaarsresultater = lovvalgsperiode.getBehandlingsresultat().getVilkaarsresultater();

        return vilkaarsresultater == null ? null : vilkaarsresultater.stream()
            .map(VilkaarsresultatTilBegrunnelseMapper::mapVilkaarsresultatTilBegrunnelseString)
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
