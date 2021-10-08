package no.nav.melosys.service.dokument.sed.bygger;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.data.UtenlandskIdent;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;
import no.nav.melosys.domain.eessi.sed.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
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
    private final BehandlingsresultatService behandlingsresultatService;
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public SedDataBygger(BehandlingsresultatService behandlingsresultatService, LandvelgerService landvelgerService,
                         LovvalgsperiodeService lovvalgsperiodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    public SedDataDto lag(SedDataGrunnlag dataGrunnlag,
                          Behandlingsresultat behandlingsresultat,
                          PeriodeType periodeType) {
        return lagSedDataDto(dataGrunnlag, behandlingsresultat, periodeType, false);
    }

    public SedDataDto lagUtkast(SedDataGrunnlag dataGrunnlag,
                                Behandlingsresultat behandlingsresultat,
                                PeriodeType periodeType) {
        return lagSedDataDto(dataGrunnlag, behandlingsresultat, periodeType, true);
    }

    private SedDataDto lagSedDataDto(SedDataGrunnlag dataGrunnlag,
                                     Behandlingsresultat behandlingsresultat,
                                     PeriodeType periodeType,
                                     boolean erUtkast) {
        validerAdresser(dataGrunnlag);
        SedDataDto sedDataDto = lagDataDtoMedPersonopplysninger(dataGrunnlag);
        validerArbeidsstederOgVirksomheter(sedDataDto);
        if (erUtkast) {
            sedDataDto.setLovvalgsperioder(lagLovvalgsperioderHvisFinnes(behandlingsresultat, periodeType));
        } else {
            sedDataDto.setLovvalgsperioder(lagLovvalgsperioder(behandlingsresultat, periodeType));
        }
        sedDataDto.setBostedsadresse(lagBostedsadresse(dataGrunnlag.getBostedGrunnlag()));
        sedDataDto.setKontaktadresse(lagKontaktadresse(dataGrunnlag.getPersondata()));
        sedDataDto.setOppholdsadresse(lagOppholdsadresse(dataGrunnlag.getPersondata()));
        sedDataDto.setSvarAnmodningUnntak(lagSvarAnmodningUnntak(behandlingsresultat));
        sedDataDto.setTidligereLovvalgsperioder(lagTidligereLovvalgsperioder(dataGrunnlag.getBehandling()));
        sedDataDto.setVedtakDto(lagVedtakDto(behandlingsresultat));
        return sedDataDto;
    }

    private static void validerAdresser(SedDataGrunnlag dataGrunnlag) {
        if (dataGrunnlag.getBostedGrunnlag().finnBostedsadresse().isEmpty()
            && dataGrunnlag.getPersondata().finnKontaktadresse().isEmpty()
            && dataGrunnlag.getPersondata().finnOppholdsadresse().isEmpty()) {
            throw new FunksjonellException(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse());
        }
    }

    private SedDataDto lagDataDtoMedPersonopplysninger(SedDataGrunnlag dataGrunnlag) {
        if (dataGrunnlag instanceof SedDataGrunnlagMedSoknad grunnlagMedSoknad) {
            return lagDataDtoMedPersonopplysninger(grunnlagMedSoknad);
        } else if (dataGrunnlag instanceof SedDataGrunnlagUtenSoknad grunnlagUtenSoknad) {
            return lagDataDtoMedPersonopplysninger(grunnlagUtenSoknad);
        }
        throw new IllegalArgumentException("Ukjent datagrunnlag: " + dataGrunnlag.getClass().getSimpleName());
    }

    private static SedDataDto lagDataDtoMedPersonopplysninger(SedDataGrunnlagUtenSoknad grunnlagUtenSøknad) {
        SedDataDto sedDataDto = new SedDataDto();
        sedDataDto.setBruker(lagBrukerFraPersondata(grunnlagUtenSøknad.getPersondata()));
        sedDataDto.setFamilieMedlem(grunnlagUtenSøknad.getPersondata().hentFamiliemedlemmer().stream()
            .filter(Familiemedlem::erForelder)
            .map(SedDataBygger::lagForelder).toList());
        return sedDataDto;
    }

    private SedDataDto lagDataDtoMedPersonopplysninger(SedDataGrunnlagMedSoknad grunnlagMedSøknad) {
        SedDataDto sedDataDto = new SedDataDto();
        sedDataDto.setArbeidsgivendeVirksomheter(lagArbeidsgivendeVirksomheter(grunnlagMedSøknad));
        sedDataDto.setSelvstendigeVirksomheter(lagSelvstendigeVirksomheter(grunnlagMedSøknad));
        sedDataDto.setArbeidssteder(hentArbeidssteder(grunnlagMedSøknad));
        sedDataDto.setAvklartBostedsland(
            landvelgerService.hentBostedsland(grunnlagMedSøknad.getBehandling().getId(), grunnlagMedSøknad.getBehandlingsgrunnlagData()).landkode()
        );
        sedDataDto.setBruker(lagBrukerFraPersondata(grunnlagMedSøknad.getPersondata()));
        sedDataDto.setFamilieMedlem(grunnlagMedSøknad.getPersondata().hentFamiliemedlemmer().stream()
            .filter(Familiemedlem::erForelder)
            .map(SedDataBygger::lagForelder).toList());
        sedDataDto.setUtenlandskIdent(grunnlagMedSøknad.getBehandlingsgrunnlagData().personOpplysninger.utenlandskIdent.stream()
            .map(SedDataBygger::tilUtenlandskIdentDto).toList());

        if (grunnlagMedSøknad.getBehandling().erBehandlingAvSøknad()) {
            sedDataDto.setSøknadsperiode(new Periode(
                grunnlagMedSøknad.getBehandlingsgrunnlagData().periode.getFom(),
                grunnlagMedSøknad.getBehandlingsgrunnlagData().periode.getTom()
            ));
        }

        return sedDataDto;
    }

    private List<Arbeidssted> hentArbeidssteder(SedDataGrunnlagMedSoknad dataGrunnlag) {
        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder().stream()
            .map(SedDataBygger::mapArbeidssted).collect(Collectors.toList()); //NOSONAR mutable list

        Set<String> arbeidsland = arbeidssteder.stream().map(Arbeidssted::getAdresse).map(Adresse::getLand).collect(Collectors.toSet());

        landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(dataGrunnlag.getBehandling().getId()).stream()
            .map(Landkoder::getKode)
            .distinct()
            .filter(not(arbeidsland::contains))
            .map(Arbeidssted::lagIkkeFastArbeidssted)
            .forEach(arbeidssteder::add);

        return arbeidssteder;
    }

    private List<Virksomhet> lagArbeidsgivendeVirksomheter(SedDataGrunnlagMedSoknad dataGrunnlag) {
        Collection<AvklartVirksomhet> avklarteVirksomheter = new ArrayList<>();
        avklarteVirksomheter.addAll(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeArbeidsgivere());
        avklarteVirksomheter.addAll(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeArbeidsgivere());

        return avklarteVirksomheter.stream()
            .map(SedDataBygger::lagVirksomhet)
            .toList();
    }

    private static List<Virksomhet> lagSelvstendigeVirksomheter(SedDataGrunnlagMedSoknad dataGrunnlagMedSoknad) {
        Collection<AvklartVirksomhet> avklarteSelvstendigeVirksomheter = new ArrayList<>();
        avklarteSelvstendigeVirksomheter.addAll(dataGrunnlagMedSoknad.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendige());
        avklarteSelvstendigeVirksomheter.addAll(dataGrunnlagMedSoknad.getAvklarteVirksomheterGrunnlag().hentUtenlandskeSelvstendige());

        return avklarteSelvstendigeVirksomheter.stream()
            .map(SedDataBygger::lagVirksomhet)
            .toList();
    }

    private static Virksomhet lagVirksomhet(AvklartVirksomhet avklartVirksomhet) {
        return new Virksomhet(avklartVirksomhet.navn, avklartVirksomhet.orgnr,
            fraStrukturertAdresse((StrukturertAdresse) avklartVirksomhet.adresse));
    }

    private static Adresse lagBostedsadresse(BostedGrunnlag bostedGrunnlag) {
        return bostedGrunnlag.finnBostedsadresse().map(b -> lagAdresse(Adressetype.BOSTEDSADRESSE, b))
            .orElse(null);
    }

    private static Adresse lagKontaktadresse(Persondata persondata) {
        return persondata.finnKontaktadresse().map(Adresse::lagKontaktadresse)
            .orElse(null);
    }

    private static Adresse lagOppholdsadresse(Persondata persondata) {
        return persondata.finnOppholdsadresse().map(Adresse::lagOppholdsadresse)
            .orElse(null);
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
        } else if (arb instanceof FlyvendeArbeidssted flyvendeArbeidssted) {
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

    private static Bruker lagBrukerFraPersondata(Persondata persondata) {
        Bruker bruker = new Bruker();
        bruker.setEtternavn(persondata.getEtternavn());
        bruker.setFornavn(persondata.getFornavn());
        bruker.setFnr(persondata.hentFolkeregisterident());
        bruker.setFoedseldato(persondata.getFødselsdato());
        bruker.setKjoenn(persondata.hentKjønnType().getKode());
        bruker.setStatsborgerskap(
            persondata.hentAlleStatsborgerskap().stream().map(Land::getKode).toList());
        bruker.setHarSensitiveOpplysninger(persondata.harStrengtAdressebeskyttelse());
        return bruker;
    }

    private static List<no.nav.melosys.domain.eessi.sed.Lovvalgsperiode> lagLovvalgsperioder(Behandlingsresultat behandlingsresultat, PeriodeType periodeType) {

        if (periodeType == PeriodeType.LOVVALGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertLovvalgsperiode()));
        } else if (periodeType == PeriodeType.ANMODNINGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertAnmodningsperiode(),
                hentUnntaksBegrunnelse(behandlingsresultat)));
        } else if (periodeType == PeriodeType.UTPEKINGSPERIODE) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertUtpekingsperiode()));
        }

        return Collections.emptyList();
    }

    private static List<no.nav.melosys.domain.eessi.sed.Lovvalgsperiode> lagLovvalgsperioderHvisFinnes(Behandlingsresultat behandlingsresultat, PeriodeType periodeType) {

        if (periodeType == PeriodeType.LOVVALGSPERIODE && behandlingsresultat.finnValidertLovvalgsperiode().isPresent()) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertLovvalgsperiode()));
        } else if (periodeType == PeriodeType.ANMODNINGSPERIODE && behandlingsresultat.finnValidertAnmodningsperiode().isPresent()) {
            return Collections.singletonList(lagLovvalgsperiodeDto(behandlingsresultat.hentValidertAnmodningsperiode(), hentUnntaksBegrunnelse(behandlingsresultat)));
        } else if (periodeType == PeriodeType.UTPEKINGSPERIODE && behandlingsresultat.finnValidertUtpekingsperiode().isPresent()) {
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

    private static no.nav.melosys.domain.eessi.sed.Lovvalgsperiode lagLovvalgsperiodeDto(PeriodeOmLovvalg periodeOmLovvalg) {
        no.nav.melosys.domain.eessi.sed.Lovvalgsperiode lovvalgsperiodeDto = new no.nav.melosys.domain.eessi.sed.Lovvalgsperiode();
        lovvalgsperiodeDto.setFom(periodeOmLovvalg.getFom());
        lovvalgsperiodeDto.setTom(periodeOmLovvalg.getTom());
        lovvalgsperiodeDto.setLovvalgsland(periodeOmLovvalg.getLovvalgsland() != null ? periodeOmLovvalg.getLovvalgsland().getKode() : null);
        lovvalgsperiodeDto.setBestemmelse(Bestemmelse.fraMelosysBestemmelse(periodeOmLovvalg.getBestemmelse()));
        lovvalgsperiodeDto.setTilleggsBestemmelse(periodeOmLovvalg.getTilleggsbestemmelse() != null
            ? Bestemmelse.fraMelosysBestemmelse(periodeOmLovvalg.getTilleggsbestemmelse()) : null);
        return lovvalgsperiodeDto;
    }

    private List<no.nav.melosys.domain.eessi.sed.Lovvalgsperiode> lagTidligereLovvalgsperioder(Behandling behandling) {

        Collection<no.nav.melosys.domain.Lovvalgsperiode> tidligereLovvalgsperioder =
            lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);

        return tidligereLovvalgsperioder.stream()
            .map(SedDataBygger::lagLovvalgsperiodeDto)
            .toList();
    }

    private VedtakDto lagVedtakDto(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getBehandling().getFagsak().getBehandlinger()
            .stream()
            .filter(behandling -> behandling.harStatus(Behandlingsstatus.AVSLUTTET) && !behandling.getId().equals(behandlingsresultat.getId()))
            .map(behandling -> behandlingsresultatService.hentBehandlingsresultat(behandling.getId()))
            .filter(Behandlingsresultat::harVedtak)
            .max(Comparator.comparing(b -> b.getVedtakMetadata().getVedtaksdato()))
            .map(b -> new VedtakDto(false, b.getVedtakMetadata().getVedtaksdato().atZone(ZoneId.systemDefault()).toLocalDate()))
            .orElse(new VedtakDto(true, null));
    }

    private static SvarAnmodningUnntak lagSvarAnmodningUnntak(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getAnmodningsperioder().stream()
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar)
            .map(SvarAnmodningUnntak::av)
            .orElse(null);
    }

    private static String hentUnntaksBegrunnelse(Behandlingsresultat behandlingsresultat) {
        Set<Vilkaarsresultat> vilkaarsresultater = behandlingsresultat.getVilkaarsresultater();

        return vilkaarsresultater == null ? null : vilkaarsresultater.stream()
            .map(VilkaarsresultatTilBegrunnelseMapper::tilEngelskBegrunnelseString)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining("\n\n"));
    }

    private static FamilieMedlem lagForelder(Familiemedlem forelder) {
        FamilieMedlem familieMedlem = new FamilieMedlem();
        familieMedlem.setFornavn(forelder.navn().fornavn());
        familieMedlem.setEtternavn(forelder.navn().etternavn());
        familieMedlem.setRelasjon(forelder.familierelasjon() == Familierelasjon.FAR ? "FAR" : "MOR");
        return familieMedlem;
    }

    private static void validerArbeidsstederOgVirksomheter(SedDataDto dataGrunnlag) {
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
