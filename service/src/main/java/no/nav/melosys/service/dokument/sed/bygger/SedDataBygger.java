package no.nav.melosys.service.dokument.sed.bygger;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.UtenlandskIdent;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.dto.Lovvalgsperiode;
import no.nav.melosys.integrasjon.eessi.dto.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import no.nav.melosys.service.dokument.sed.mapper.VilkaarsresultatTilBegrunnelseMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;

@Service
public class SedDataBygger {
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;

    @Autowired
    public SedDataBygger(LovvalgsperiodeService lovvalgsperiodeService, LandvelgerService landvelgerService) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
    }

    public SedDataDto lag(SedDataGrunnlag dataGrunnlag, Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) throws FunksjonellException, TekniskException {
        if (dataGrunnlag instanceof SedDataGrunnlagMedSoknad) {
            return lag((SedDataGrunnlagMedSoknad) dataGrunnlag, behandlingsresultat, medlemsperiodeType);
        } else if (dataGrunnlag instanceof SedDataGrunnlagUtenSoknad) {
            return lag((SedDataGrunnlagUtenSoknad) dataGrunnlag, behandlingsresultat, medlemsperiodeType);
        }
        throw new IllegalArgumentException("Ukjent datagrunnlag: " + dataGrunnlag.getClass().getSimpleName());
    }

    private SedDataDto lag(SedDataGrunnlagMedSoknad dataGrunnlag, Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) throws TekniskException, FunksjonellException {
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().finnAdresse()
            .orElseThrow(() -> new FunksjonellException("Finner ingen adresse på person i behandling " + behandlingsresultat.getId()));
        SedDataDto sedDataDto = lagPersonopplysninger(dataGrunnlag, bostedsadresse);
        sedDataDto.setLovvalgsperioder(lagLovvalgsperioderDto(behandlingsresultat, medlemsperiodeType));
        sedDataDto.setTidligereLovvalgsperioder(lagTidligereLovvalgsperioderDto(dataGrunnlag.getBehandling()));
        sedDataDto.setMottakerLand(dataGrunnlag.getBehandling().getFagsak().hentMyndighetLandkode().getKode());
        sedDataDto.setSvarAnmodningUnntak(lagSvarAnmodningUnntakDto(behandlingsresultat));
        return sedDataDto;
    }

    private SedDataDto lag(SedDataGrunnlagUtenSoknad dataGrunnlag, Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) throws TekniskException, FunksjonellException {
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().finnAdresse()
            .orElseThrow(() -> new FunksjonellException("Finner ingen adresse på person i behandling " + behandlingsresultat.getId()));
        SedDataDto sedDataDto = lagPersonopplysninger(dataGrunnlag, bostedsadresse);
        sedDataDto.setLovvalgsperioder(lagLovvalgsperioderDto(behandlingsresultat, medlemsperiodeType));
        sedDataDto.setMottakerLand(dataGrunnlag.getBehandling().getFagsak().hentMyndighetLandkode().getKode());
        sedDataDto.setSvarAnmodningUnntak(lagSvarAnmodningUnntakDto(behandlingsresultat));
        return sedDataDto;
    }

    public SedDataDto lagUtkast(SedDataGrunnlag dataGrunnlag, Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) throws FunksjonellException, TekniskException {
        if (dataGrunnlag instanceof SedDataGrunnlagMedSoknad) {
            return lagUtkast((SedDataGrunnlagMedSoknad) dataGrunnlag, behandlingsresultat, medlemsperiodeType);
        } else if (dataGrunnlag instanceof SedDataGrunnlagUtenSoknad) {
            return lagUtkast((SedDataGrunnlagUtenSoknad) dataGrunnlag, behandlingsresultat, medlemsperiodeType);
        }
        throw new IllegalArgumentException("Ukjent datagrunnlag: " + dataGrunnlag.getClass().getSimpleName());
    }

    private SedDataDto lagUtkast(SedDataGrunnlagMedSoknad dataGrunnlag, Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) throws FunksjonellException, TekniskException {
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().finnAdresse().orElse(null);
        SedDataDto sedDataDto = lagPersonopplysninger(dataGrunnlag, bostedsadresse);
        sedDataDto.setLovvalgsperioder(lagLovvalgsperioderDtoHvisFinnes(behandlingsresultat, medlemsperiodeType));
        sedDataDto.setSvarAnmodningUnntak(lagSvarAnmodningUnntakDto(behandlingsresultat));
        return sedDataDto;
    }

    private SedDataDto lagUtkast(SedDataGrunnlagUtenSoknad dataGrunnlag, Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) throws TekniskException {
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().finnAdresse().orElse(null);
        SedDataDto sedDataDto = lagPersonopplysninger(dataGrunnlag, bostedsadresse);
        sedDataDto.setLovvalgsperioder(lagLovvalgsperioderDtoHvisFinnes(behandlingsresultat, medlemsperiodeType));
        sedDataDto.setSvarAnmodningUnntak(lagSvarAnmodningUnntakDto(behandlingsresultat));
        return sedDataDto;
    }

    private static SedDataDto lagPersonopplysninger(SedDataGrunnlagUtenSoknad dataGrunnlag, StrukturertAdresse bostedsadresse) throws TekniskException {
        SedDataDto sedDataDto = new SedDataDto();

        sedDataDto.setBruker(hentBrukerFraPersonDokument(dataGrunnlag.getPerson()));

        sedDataDto.setFamilieMedlem(dataGrunnlag.getPerson().familiemedlemmer.stream()
            .filter(f -> f.familierelasjon == Familierelasjon.FARA || f.familierelasjon == Familierelasjon.MORA)
            .map(SedDataBygger::hentFamilieMedlem).collect(Collectors.toList()));

        sedDataDto.setBostedsadresse(fraBostedsadresse(bostedsadresse));

        return sedDataDto;
    }

    private SedDataDto lagPersonopplysninger(SedDataGrunnlagMedSoknad dataGrunnlag, StrukturertAdresse strukturertAdresse) throws TekniskException, FunksjonellException {
        SedDataDto sedDataDto = new SedDataDto();

        sedDataDto.setArbeidsgivendeVirksomheter(map(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere()));

        sedDataDto.setArbeidssteder(dataGrunnlag.getArbeidssteder().hentArbeidssteder().stream()
            .map(SedDataBygger::mapArbeidssted).collect(Collectors.toList()));

        sedDataDto.setBostedsadresse(fraBostedsadresse(strukturertAdresse));

        sedDataDto.setAvklartBostedsland(
            landvelgerService.hentBostedsland(dataGrunnlag.getBehandling().getId(), dataGrunnlag.getSøknad()).getKode()
        );

        sedDataDto.setBruker(hentBrukerFraPersonDokument(dataGrunnlag.getPerson()));

        sedDataDto.setFamilieMedlem(dataGrunnlag.getPerson().familiemedlemmer.stream()
            .filter(f -> f.familierelasjon == Familierelasjon.FARA || f.familierelasjon == Familierelasjon.MORA)
            .map(SedDataBygger::hentFamilieMedlem).collect(Collectors.toList()));

        sedDataDto.setSelvstendigeVirksomheter(map(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendige()));

        sedDataDto.setUtenlandskeVirksomheter(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeVirksomheter().stream().map(
            SedDataBygger::tilUtenlandsVirksomhetDto).collect(Collectors.toList()));

        sedDataDto.setUtenlandskIdent(dataGrunnlag.getSøknad().personOpplysninger.utenlandskIdent.stream()
            .map(SedDataBygger::tilUtenlandskIdentDto).collect(Collectors.toList()));

        return sedDataDto;
    }

    private static List<Virksomhet> map(List<AvklartVirksomhet> avklarteArbeidsgivere) {
        return avklarteArbeidsgivere.stream()
            .map(aa -> new Virksomhet(aa.navn, aa.orgnr, fraStrukturertAdresse((StrukturertAdresse) aa.adresse)))
            .collect(Collectors.toList());
    }

    private static Adresse fraBostedsadresse(StrukturertAdresse bostedsadresse) throws TekniskException {
        if (bostedsadresse == null) {
            return new Adresse();
        }

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

    private static Arbeidssted mapArbeidssted(no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted arb) {
        Arbeidssted arbeidssted = new Arbeidssted();
        arbeidssted.setFysisk(arb.erFysisk());
        if (arb.erFysisk()) {
            FysiskArbeidssted fysiskArbeidssted = (FysiskArbeidssted) arb;
            arbeidssted.setAdresse(fraStrukturertAdresse(fysiskArbeidssted.getAdresse()));
            arbeidssted.setNavn(arb.getForetakNavn());
        } else {
            MaritimtArbeidssted maritimtArbeidssted = (MaritimtArbeidssted) arb;
            arbeidssted.setNavn(maritimtArbeidssted.getEnhetNavn());

            Adresse adresse = new Adresse();
            adresse.setLand(maritimtArbeidssted.getLandkode());
            adresse.setPoststed("N/A");

            arbeidssted.setAdresse(adresse);
            arbeidssted.setHjemmebase(maritimtArbeidssted.getFlaggLandKode());
        }
        return arbeidssted;
    }

    private static Bruker hentBrukerFraPersonDokument(PersonDokument personDokument) {
        Bruker bruker = new Bruker();
        bruker.setEtternavn(personDokument.etternavn);
        bruker.setFornavn(personDokument.fornavn);
        bruker.setFnr(personDokument.fnr);
        bruker.setFoedseldato(personDokument.fødselsdato);
        bruker.setKjoenn(personDokument.kjønn.getKode());
        bruker.setStatsborgerskap(personDokument.statsborgerskap.getKode());
        bruker.setHarSensitiveOpplysninger(hentHarSensitiveOpplysninger(personDokument.diskresjonskode));

        return bruker;

    }

    private static boolean hentHarSensitiveOpplysninger(Diskresjonskode diskresjonskode) {
        if (diskresjonskode == null) {
            return false;
        }

        return diskresjonskode.erKode6();
    }

    private static Adresse fraStrukturertAdresse(StrukturertAdresse strukturertAdresse) {
        Adresse adresse = new Adresse();
        adresse.setGateadresse(strukturertAdresse.gatenavn + (strukturertAdresse.husnummer == null ? "" : (" " + strukturertAdresse.husnummer + " ")));
        adresse.setLand(strukturertAdresse.landkode);
        adresse.setPostnr(strukturertAdresse.postnummer);
        adresse.setPoststed(strukturertAdresse.poststed);
        adresse.setRegion(strukturertAdresse.region);
        return adresse;
    }

    private static List<Lovvalgsperiode> lagLovvalgsperioderDto(Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) {

        if (medlemsperiodeType == MedlemsperiodeType.LOVVALGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertLovvalgsperiode()));
        } else if (medlemsperiodeType == MedlemsperiodeType.ANMODNINGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertAnmodningsperiode(),
                hentUnntaksBegrunnelse(behandlingsresultat)));
        }

        return Collections.emptyList();
    }

    private static List<Lovvalgsperiode> lagLovvalgsperioderDtoHvisFinnes(Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) {

        if (medlemsperiodeType == MedlemsperiodeType.LOVVALGSPERIODE && !behandlingsresultat.getLovvalgsperioder().isEmpty()) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.getLovvalgsperioder().iterator().next()));
        } else if (medlemsperiodeType == MedlemsperiodeType.ANMODNINGSPERIODE && !behandlingsresultat.getAnmodningsperioder().isEmpty()) {
            return Collections.singletonList(lagLovvalgsperiodeDto(
                behandlingsresultat.getAnmodningsperioder().iterator().next(),
                hentUnntaksBegrunnelse(behandlingsresultat)
            ));
        }

        return Collections.emptyList();
    }


    private static Lovvalgsperiode lagLovvalgsperiodeDto(Anmodningsperiode anmodningsperiode, String unntaksBegrunnelse) {
        Lovvalgsperiode lovvalgsperiodeDto = lagLovvalgsperiodeDto(anmodningsperiode);

        lovvalgsperiodeDto.setUnntakFraLovvalgsland(anmodningsperiode.getUnntakFraLovvalgsland().getKode());
        lovvalgsperiodeDto.setUnntakFraBestemmelse(LovvalgTilBestemmelseDtoMapper
            .mapMelosysLovvalgTilBestemmelseDto(anmodningsperiode.getUnntakFraBestemmelse()));
        lovvalgsperiodeDto.setUnntaksBegrunnelse(unntaksBegrunnelse);

        return lovvalgsperiodeDto;
    }

    private static Lovvalgsperiode lagLovvalgsperiodeDto(Medlemskapsperiode periodeMedBestemmelse) {
        Lovvalgsperiode lovvalgsperiodeDto = new Lovvalgsperiode();
        lovvalgsperiodeDto.setFom(periodeMedBestemmelse.getFom());
        lovvalgsperiodeDto.setTom(periodeMedBestemmelse.getTom());
        lovvalgsperiodeDto.setLovvalgsland(periodeMedBestemmelse.getLovvalgsland() != null ? periodeMedBestemmelse.getLovvalgsland().getKode() : null);
        lovvalgsperiodeDto.setBestemmelse(LovvalgTilBestemmelseDtoMapper.mapMelosysLovvalgTilBestemmelseDto(periodeMedBestemmelse.getBestemmelse()));
        return lovvalgsperiodeDto;
    }

    private List<Lovvalgsperiode> lagTidligereLovvalgsperioderDto(Behandling behandling)
        throws TekniskException {

        Collection<no.nav.melosys.domain.Lovvalgsperiode> tidligereLovvalgsperioder =
            lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);

        return tidligereLovvalgsperioder.stream()
            .map(SedDataBygger::lagLovvalgsperiodeDto)
            .collect(Collectors.toList());
    }

    private static SvarAnmodningUnntakDto lagSvarAnmodningUnntakDto(Behandlingsresultat behandlingsresultat) throws TekniskException {
        Anmodningsperiode anmodningsperiode = null;
        if (behandlingsresultat.getAnmodningsperioder().iterator().hasNext()) {
            anmodningsperiode = behandlingsresultat.getAnmodningsperioder().iterator().next();
        }

        if (anmodningsperiode != null && anmodningsperiode.getAnmodningsperiodeSvar() != null) {
            return SvarAnmodningUnntakDto.av(anmodningsperiode.getAnmodningsperiodeSvar());
        }
        return null;
    }

    private static String hentUnntaksBegrunnelse(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat == null) {
            return null;
        }
        Set<Vilkaarsresultat> vilkaarsresultater = behandlingsresultat.getVilkaarsresultater();

        return vilkaarsresultater == null ? null : vilkaarsresultater.stream()
            .map(VilkaarsresultatTilBegrunnelseMapper::tilEngelskBegrunnelseString)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining("\n\n"));
    }

    private static String[] splitFulltNavn(String navn) {
        if (navn == null || navn.isEmpty()) {
            return new String[2];
        } else if (!navn.contains(" ")) {
            return new String[]{navn, null};
        } else {
            return navn.split(" ", 2);
        }
    }

    private static FamilieMedlem hentFamilieMedlem(Familiemedlem f) {
        FamilieMedlem familieMedlem = new FamilieMedlem();
        String[] navn = splitFulltNavn(f.navn);
        familieMedlem.setFornavn(navn[0]);
        familieMedlem.setEtternavn(navn[1]);
        familieMedlem.setRelasjon(f.familierelasjon == Familierelasjon.FARA ? "FAR" : "MOR");
        return familieMedlem;
    }

    private static Virksomhet tilUtenlandsVirksomhetDto(AvklartVirksomhet uVirksomhet) {
        Virksomhet virksomhet = new Virksomhet();
        virksomhet.setNavn(uVirksomhet.navn);
        virksomhet.setOrgnr(uVirksomhet.orgnr);
        virksomhet.setType("registrering");
        virksomhet.setAdresse(fraStrukturertAdresse((StrukturertAdresse) uVirksomhet.adresse));
        return virksomhet;
    }
}
