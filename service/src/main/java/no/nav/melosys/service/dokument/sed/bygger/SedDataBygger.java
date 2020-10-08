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
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;
import no.nav.melosys.domain.eessi.sed.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.BostedGrunnlag;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FlyvendeArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.dokument.sed.mapper.VilkaarsresultatTilBegrunnelseMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.function.Predicate.not;
import static no.nav.melosys.domain.eessi.sed.Adresse.fraStrukturertAdresse;
import static no.nav.melosys.domain.eessi.sed.Adresse.lagAdresse;

@Service
public class SedDataBygger {
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;

    @Autowired
    public SedDataBygger(LovvalgsperiodeService lovvalgsperiodeService, LandvelgerService landvelgerService) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
    }

    public SedDataDto lag(SedDataGrunnlag dataGrunnlag,
                          Behandlingsresultat behandlingsresultat,
                          MedlemsperiodeType medlemsperiodeType) throws TekniskException, FunksjonellException {
        return lagSedDataDto(dataGrunnlag, behandlingsresultat, medlemsperiodeType, false);
    }

    public SedDataDto lagUtkast(SedDataGrunnlag dataGrunnlag,
                                Behandlingsresultat behandlingsresultat,
                                MedlemsperiodeType medlemsperiodeType) throws FunksjonellException, TekniskException {
        return lagSedDataDto(dataGrunnlag, behandlingsresultat, medlemsperiodeType, true);
    }

    private SedDataDto lagSedDataDto(SedDataGrunnlag dataGrunnlag,
                                     Behandlingsresultat behandlingsresultat,
                                     MedlemsperiodeType medlemsperiodeType,
                                     boolean erUtkast) throws FunksjonellException, TekniskException {
        SedDataDto sedDataDto = lagPersonopplysninger(dataGrunnlag);
        validerArbeidsstederOgVirksomheter(sedDataDto);
        if (erUtkast) {
            sedDataDto.setBostedsadresse(finnAdresse(dataGrunnlag.getBostedGrunnlag()).orElse(null));
            sedDataDto.setLovvalgsperioder(lagLovvalgsperioderDtoHvisFinnes(behandlingsresultat, medlemsperiodeType));
        } else {
            sedDataDto.setBostedsadresse(finnAdresse(dataGrunnlag.getBostedGrunnlag())
                .orElseThrow(() -> new FunksjonellException("Finner ingen bostedsadresse på person i behandling " + behandlingsresultat.getId())));
            sedDataDto.setLovvalgsperioder(lagLovvalgsperioderDto(behandlingsresultat, medlemsperiodeType));
        }
        sedDataDto.setTidligereLovvalgsperioder(lagTidligereLovvalgsperioderDto(dataGrunnlag.getBehandling()));
        sedDataDto.setSvarAnmodningUnntak(lagSvarAnmodningUnntakDto(behandlingsresultat));
        return sedDataDto;
    }

    private SedDataDto lagPersonopplysninger(SedDataGrunnlag dataGrunnlag) throws FunksjonellException, TekniskException {
        if (dataGrunnlag instanceof SedDataGrunnlagMedSoknad) {
            return lagPersonopplysninger((SedDataGrunnlagMedSoknad) dataGrunnlag);
        } else if (dataGrunnlag instanceof SedDataGrunnlagUtenSoknad) {
            return lagPersonopplysninger((SedDataGrunnlagUtenSoknad) dataGrunnlag);
        }
        throw new IllegalArgumentException("Ukjent datagrunnlag: " + dataGrunnlag.getClass().getSimpleName());
    }

    private static SedDataDto lagPersonopplysninger(SedDataGrunnlagUtenSoknad dataGrunnlag) {
        SedDataDto sedDataDto = new SedDataDto();

        sedDataDto.setBruker(hentBrukerFraPersonDokument(dataGrunnlag.getPerson()));

        sedDataDto.setFamilieMedlem(dataGrunnlag.getPerson().familiemedlemmer.stream()
            .filter(f -> f.familierelasjon == Familierelasjon.FARA || f.familierelasjon == Familierelasjon.MORA)
            .map(SedDataBygger::hentFamilieMedlem).collect(Collectors.toList()));

        return sedDataDto;
    }

    private SedDataDto lagPersonopplysninger(SedDataGrunnlagMedSoknad dataGrunnlag) throws TekniskException, FunksjonellException {
        SedDataDto sedDataDto = new SedDataDto();

        sedDataDto.setArbeidsgivendeVirksomheter(lagArbeidsgivendeVirksomheter(dataGrunnlag));
        sedDataDto.setSelvstendigeVirksomheter(lagSelvstendigeVirksomheter(dataGrunnlag));

        sedDataDto.setArbeidssteder(hentArbeidssteder(dataGrunnlag));

        sedDataDto.setAvklartBostedsland(
            landvelgerService.hentBostedsland(dataGrunnlag.getBehandling().getId(), dataGrunnlag.getBehandlingsgrunnlagData()).getKode()
        );

        sedDataDto.setBruker(hentBrukerFraPersonDokument(dataGrunnlag.getPerson()));

        sedDataDto.setFamilieMedlem(dataGrunnlag.getPerson().familiemedlemmer.stream()
            .filter(f -> f.familierelasjon == Familierelasjon.FARA || f.familierelasjon == Familierelasjon.MORA)
            .map(SedDataBygger::hentFamilieMedlem).collect(Collectors.toList()));

        sedDataDto.setUtenlandskIdent(dataGrunnlag.getBehandlingsgrunnlagData().personOpplysninger.utenlandskIdent.stream()
            .map(SedDataBygger::tilUtenlandskIdentDto).collect(Collectors.toList()));

        return sedDataDto;
    }

    private List<Arbeidssted> hentArbeidssteder(SedDataGrunnlagMedSoknad dataGrunnlag) throws IkkeFunnetException {
        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder().stream()
            .map(SedDataBygger::mapArbeidssted).collect(Collectors.toList());

        Set<String> arbeidsland = arbeidssteder.stream().map(Arbeidssted::getAdresse).map(Adresse::getLand).collect(Collectors.toSet());

        landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(dataGrunnlag.getBehandling().getId()).stream()
            .map(Landkoder::getKode)
            .distinct()
            .filter(not(arbeidsland::contains))
            .map(Arbeidssted::lagIkkeFastArbeidssted)
            .forEach(arbeidssteder::add);

        return arbeidssteder;
    }

    private List<Virksomhet> lagArbeidsgivendeVirksomheter(SedDataGrunnlagMedSoknad dataGrunnlag)
        throws IkkeFunnetException, TekniskException {
        Collection<AvklartVirksomhet> avklarteVirksomheter = new ArrayList<>();
        avklarteVirksomheter.addAll(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere());
        avklarteVirksomheter.addAll(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeArbeidsgivere());

        return avklarteVirksomheter.stream()
            .map(SedDataBygger::lagVirksomhet)
            .collect(Collectors.toList());
    }

    private static List<Virksomhet> lagSelvstendigeVirksomheter(SedDataGrunnlagMedSoknad dataGrunnlagMedSoknad)
        throws IkkeFunnetException, TekniskException {
        Collection<AvklartVirksomhet> avklarteSelvstendigeVirksomheter = new ArrayList<>();
        avklarteSelvstendigeVirksomheter.addAll(dataGrunnlagMedSoknad.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendige());
        avklarteSelvstendigeVirksomheter.addAll(dataGrunnlagMedSoknad.getAvklarteVirksomheterGrunnlag().hentUtenlandskeSelvstendige());

        return avklarteSelvstendigeVirksomheter.stream()
            .map(SedDataBygger::lagVirksomhet)
            .collect(Collectors.toList());
    }

    private static Virksomhet lagVirksomhet(AvklartVirksomhet avklartVirksomhet) {
        return new Virksomhet(avklartVirksomhet.navn, avklartVirksomhet.orgnr,
            fraStrukturertAdresse((StrukturertAdresse) avklartVirksomhet.adresse));
    }

    private static Optional<Adresse> finnAdresse(BostedGrunnlag bostedGrunnlag) {
        return bostedGrunnlag.finnBostedsadresse().map(b -> lagAdresse(Adressetype.BOSTEDSADRESSE, b));
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
        } else if (arb instanceof FlyvendeArbeidssted) {
            FlyvendeArbeidssted flyvendeArbeidssted = (FlyvendeArbeidssted) arb;
            arbeidssted.setNavn(flyvendeArbeidssted.getEnhetNavn());
            arbeidssted.setAdresse(Adresse.lagAdresseMedBareLandkode(flyvendeArbeidssted.getLandkode()));
            arbeidssted.setHjemmebase(flyvendeArbeidssted.getLandkode());
        } else {
            MaritimtArbeidssted maritimtArbeidssted = (MaritimtArbeidssted) arb;
            arbeidssted.setNavn(maritimtArbeidssted.getEnhetNavn() + (maritimtArbeidssted.erSokkel() ? " offshore" : ""));
            arbeidssted.setAdresse(Adresse.lagAdresseMedBareLandkode(maritimtArbeidssted.getLandkode()));
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

    private static List<no.nav.melosys.domain.eessi.sed.Lovvalgsperiode> lagLovvalgsperioderDto(Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) {

        if (medlemsperiodeType == MedlemsperiodeType.LOVVALGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertLovvalgsperiode()));
        } else if (medlemsperiodeType == MedlemsperiodeType.ANMODNINGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertAnmodningsperiode(),
                hentUnntaksBegrunnelse(behandlingsresultat)));
        } else if (medlemsperiodeType == MedlemsperiodeType.UTPEKINGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertUtpekingsperiode()));
        }

        return Collections.emptyList();
    }

    private static List<no.nav.melosys.domain.eessi.sed.Lovvalgsperiode> lagLovvalgsperioderDtoHvisFinnes(Behandlingsresultat behandlingsresultat, MedlemsperiodeType medlemsperiodeType) {

        if (medlemsperiodeType == MedlemsperiodeType.LOVVALGSPERIODE && behandlingsresultat.finnValidertLovvalgsperiode().isPresent()) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertLovvalgsperiode()));
        } else if (medlemsperiodeType == MedlemsperiodeType.ANMODNINGSPERIODE && behandlingsresultat.finnValidertAnmodningsperiode().isPresent()) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertAnmodningsperiode(), hentUnntaksBegrunnelse(behandlingsresultat)));
        } else if (medlemsperiodeType == MedlemsperiodeType.UTPEKINGSPERIODE && behandlingsresultat.finnValidertUtpekingsperiode().isPresent()) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertUtpekingsperiode()));
        }

        return Collections.emptyList();
    }


    private static no.nav.melosys.domain.eessi.sed.Lovvalgsperiode lagLovvalgsperiodeDto(Anmodningsperiode anmodningsperiode, String unntaksBegrunnelse) {
        no.nav.melosys.domain.eessi.sed.Lovvalgsperiode lovvalgsperiodeDto = lagLovvalgsperiodeDto(anmodningsperiode);

        lovvalgsperiodeDto.setUnntakFraLovvalgsland(anmodningsperiode.getUnntakFraLovvalgsland().getKode());
        lovvalgsperiodeDto.setUnntakFraBestemmelse(Bestemmelse.fraMelosysBestemmelse(anmodningsperiode.getUnntakFraBestemmelse()));
        lovvalgsperiodeDto.setUnntaksBegrunnelse(unntaksBegrunnelse);

        return lovvalgsperiodeDto;
    }

    private static no.nav.melosys.domain.eessi.sed.Lovvalgsperiode lagLovvalgsperiodeDto(Medlemskapsperiode medlemskapsperiode) {
        no.nav.melosys.domain.eessi.sed.Lovvalgsperiode lovvalgsperiodeDto = new no.nav.melosys.domain.eessi.sed.Lovvalgsperiode();
        lovvalgsperiodeDto.setFom(medlemskapsperiode.getFom());
        lovvalgsperiodeDto.setTom(medlemskapsperiode.getTom());
        lovvalgsperiodeDto.setLovvalgsland(medlemskapsperiode.getLovvalgsland() != null ? medlemskapsperiode.getLovvalgsland().getKode() : null);
        lovvalgsperiodeDto.setBestemmelse(Bestemmelse.fraMelosysBestemmelse(medlemskapsperiode.getBestemmelse()));
        lovvalgsperiodeDto.setTilleggsBestemmelse(medlemskapsperiode.getTilleggsbestemmelse() != null
            ? Bestemmelse.fraMelosysBestemmelse(medlemskapsperiode.getTilleggsbestemmelse()) : null);
        return lovvalgsperiodeDto;
    }

    private List<no.nav.melosys.domain.eessi.sed.Lovvalgsperiode> lagTidligereLovvalgsperioderDto(Behandling behandling)
        throws TekniskException {

        Collection<no.nav.melosys.domain.Lovvalgsperiode> tidligereLovvalgsperioder =
            lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);

        return tidligereLovvalgsperioder.stream()
            .map(SedDataBygger::lagLovvalgsperiodeDto)
            .collect(Collectors.toList());
    }

    private static SvarAnmodningUnntak lagSvarAnmodningUnntakDto(Behandlingsresultat behandlingsresultat) throws TekniskException {
        Anmodningsperiode anmodningsperiode = null;
        if (behandlingsresultat.getAnmodningsperioder().iterator().hasNext()) {
            anmodningsperiode = behandlingsresultat.getAnmodningsperioder().iterator().next();
        }

        if (anmodningsperiode != null && anmodningsperiode.getAnmodningsperiodeSvar() != null) {
            return SvarAnmodningUnntak.av(anmodningsperiode.getAnmodningsperiodeSvar());
        }
        return null;
    }

    private static String hentUnntaksBegrunnelse(Behandlingsresultat behandlingsresultat) {
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

    private static void validerArbeidsstederOgVirksomheter(SedDataDto dataGrunnlag) throws FunksjonellException {
        for (Arbeidssted arbeidssted : dataGrunnlag.getArbeidssteder()) {
            if (StringUtils.isEmpty(arbeidssted.getAdresse().getLand())) {
                throw new FunksjonellException("Feltet land er ikke utfylt for arbeidssted " + arbeidssted.getNavn());
            }
        }
        for (Virksomhet virksomhet : dataGrunnlag.getSelvstendigeVirksomheter()) {
            if (StringUtils.isEmpty(virksomhet.getAdresse().getLand())) {
                throw new FunksjonellException("Feltet land er ikke utfylt for selvstendig virksomhet " + virksomhet.getNavn());
            }
        }
        for (Virksomhet virksomhet : dataGrunnlag.getArbeidsgivendeVirksomheter()) {
            if (StringUtils.isEmpty(virksomhet.getAdresse().getLand())) {
                throw new FunksjonellException("Feltet land er ikke utfylt for virksomhet " + virksomhet.getNavn());
            }
        }
    }
}
