package no.nav.melosys.service.brev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.muligemottakere.MuligMottakerDto;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.MuligeMottakereDto;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.*;

@Service
public class BrevbestillingService {

    private static final List<Produserbaredokumenter> BREV_TILGJENGELIG_FOR_MANUELL_BESTILLING = List.of(
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
        MANGELBREV_BRUKER,
        MANGELBREV_ARBEIDSGIVER,
        GENERELT_FRITEKSTBREV_BRUKER,
        GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
        GENERELT_FRITEKSTBREV_VIRKSOMHET,
        AVSLAG_MANGLENDE_OPPLYSNINGER,
        UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
        FRITEKSTBREV
    );

    private final DokumentServiceFasade dokumentServiceFasade;
    private final BrevmottakerService brevmottakerService;
    private final BehandlingService behandlingService;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final PersondataFasade persondataFasade;
    private final DokumentNavnService dokumentNavnService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public BrevbestillingService(BrevmottakerService brevmottakerService,
                                 DokumentServiceFasade dokumentServiceFasade,
                                 BehandlingService behandlingService,
                                 EregFasade eregFasade,
                                 KontaktopplysningService kontaktopplysningService,
                                 PersondataFasade persondataFasade,
                                 DokumentNavnService dokumentNavnService,
                                 UtenlandskMyndighetService utenlandskMyndighetService
    ) {
        this.brevmottakerService = brevmottakerService;
        this.dokumentServiceFasade = dokumentServiceFasade;
        this.behandlingService = behandlingService;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.persondataFasade = persondataFasade;
        this.dokumentNavnService = dokumentNavnService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Transactional
    public List<MuligMottakerDto> hentMuligeMottakereEtater(Produserbaredokumenter produserbaredokumenter,
                                                            long behandlingId,
                                                            List<String> orgnrEtater) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        return orgnrEtater.stream()
            .map(orgnr -> new MuligMottakerDto.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, produserbaredokumenter, ETAT))
                .medMottakerNavn(hentMottakerNavn(produserbaredokumenter, behandling, ETAT, orgnr))
                .medRolle(ETAT)
                .medOrgnr(orgnr)
                .build())
            .toList();
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Transactional
    public MuligeMottakereDto hentMuligeMottakere(Produserbaredokumenter produserbaredokumenter, long behandlingId, String orgnrTilValgtArbeidsgiver) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Mottakerliste mottakerliste = brevmottakerService.hentMottakerliste(produserbaredokumenter, behandlingId);
        return new MuligeMottakereDto(
            lagHovedMottakerMuligMottakerDto(produserbaredokumenter, behandling, mottakerliste.getHovedMottaker(), orgnrTilValgtArbeidsgiver),
            lagKopiMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getKopiMottakere(), mottakerliste.getHovedMottaker()),
            lagFasteMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getFasteMottakere()));
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private MuligMottakerDto lagHovedMottakerMuligMottakerDto(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller hovedmottaker, String orgnrTilValgtArbeidsgiver) {
        return new MuligMottakerDto.Builder()
            .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, produserbaredokumenter, hovedmottaker))
            .medMottakerNavn(hentMottakerNavn(produserbaredokumenter, behandling, hovedmottaker, orgnrTilValgtArbeidsgiver))
            .medRolle(hovedmottaker)
            .build();
    }

    private String hentMottakerNavn(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller hovedmottaker, String orgnr) {
        switch (hovedmottaker) {
            case BRUKER -> {
                Aktoer avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(hovedmottaker), behandling);
                if (avklartMottaker.getRolle() == BRUKER) {
                    return persondataFasade.hentSammensattNavn(behandling.getFagsak().hentBrukersAktørID());
                } else if (avklartMottaker.erPerson()) {
                    return persondataFasade.hentSammensattNavn(avklartMottaker.getPersonIdent());
                } else {
                    var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.getOrgnr());
                    return orgDokument.getNavn();
                }
            }
            case ARBEIDSGIVER, VIRKSOMHET, ETAT -> {
                var saksopplysning = eregFasade.finnOrganisasjon(orgnr);
                if (saksopplysning.isPresent()) {
                    var orgDokument = (OrganisasjonDokument) saksopplysning.get().getDokument();
                    return orgDokument.getNavn();
                } else {
                    throw new IkkeFunnetException("Kan ikke hente mottakernavn, fant ikke orgnr %s".formatted(orgnr));
                }
            }
            case TRYGDEMYNDIGHET -> {
                if (produserbaredokumenter == UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV) {
                    Aktoer avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(hovedmottaker), behandling);
                    var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(avklartMottaker.hentMyndighetLandkode(), produserbaredokumenter);
                    return utenlandskMyndighet.navn;
                } else {
                    throw new FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker);
                }
            }
            default ->
                throw new FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker);
        }
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private List<MuligMottakerDto> lagKopiMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<Aktoersroller> kopiMottakere, Aktoersroller hovedmottaker) {
        List<MuligMottakerDto> muligMottakerDtos = new ArrayList<>();
        for (Aktoersroller kopiMottaker : kopiMottakere) {
            switch (kopiMottaker) {
                case BRUKER ->
                    muligMottakerDtos.add(lagKopiMottakerForBruker(produserbaredokumenter, behandling, kopiMottaker, hovedmottaker));
                case ARBEIDSGIVER ->
                    muligMottakerDtos.addAll(lagKopiMottakereForArbeidsgiver(produserbaredokumenter, behandling, kopiMottaker));
                case TRYGDEMYNDIGHET ->
                    muligMottakerDtos.addAll(lagKopiMottakereForMyndighet(produserbaredokumenter, behandling, kopiMottaker));
                default -> throw new IllegalStateException(kopiMottaker + " er ikke en gyldig kopiMottakerrolle");
            }
        }
        return muligMottakerDtos;
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private MuligMottakerDto lagKopiMottakerForBruker(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker, Aktoersroller hovedmottaker) {
        Aktoer avklartKopi = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling);
        if (avklartKopi.getRolle() == BRUKER || hovedmottaker == kopiMottaker) {
            String aktørID = behandling.getFagsak().hentBrukersAktørID();
            return new MuligMottakerDto.Builder()
                .medDokumentNavn("Kopi til bruker")
                .medMottakerNavn(persondataFasade.hentSammensattNavn(aktørID))
                .medRolle(BRUKER)
                .medAktørId(aktørID)
                .build();
        } else {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            return new MuligMottakerDto.Builder()
                .medDokumentNavn("Kopi til brukers fullmektig")
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartKopi.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build();
        }
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private List<MuligMottakerDto> lagKopiMottakereForArbeidsgiver(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker) {
        List<MuligMottakerDto> muligMottakerDtos = new ArrayList<>();

        List<Aktoer> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling, false, true);
        for (Aktoer avklartKopi : avklarteKopier) {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            String fastTekst = avklartKopi.getRolle() == ARBEIDSGIVER ? "Kopi til arbeidsgiver" : "Kopi til arbeidsgivers fullmektig";
            muligMottakerDtos.add(new MuligMottakerDto.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartKopi, fastTekst))
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartKopi.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build());
        }
        return muligMottakerDtos;
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private List<MuligMottakerDto> lagKopiMottakereForMyndighet(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker) {
        List<MuligMottakerDto> muligMottakerDtos = new ArrayList<>();

        List<Aktoer> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling);
        for (Aktoer avklartKopi : avklarteKopier) {
            String fastTekst = "Kopi til utenlandsk trygdemyndighet";
            muligMottakerDtos.add(new MuligMottakerDto.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartKopi, fastTekst))
                .medMottakerNavn("Utenlandsk trygdemyndighet")
                .medRolle(avklartKopi.getRolle())
                .medInstitusjonId(avklartKopi.getInstitusjonId())
                .build());
        }
        return muligMottakerDtos;
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private List<MuligMottakerDto> lagFasteMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<FastMottakerMedOrgnr> fasteMottakere) {
        List<MuligMottakerDto> muligMottakerDtos = new ArrayList<>();

        for (FastMottakerMedOrgnr fastMottakerMedOrgnr : fasteMottakere) {
            Aktoer avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, FastMottakerMedOrgnr.av(fastMottakerMedOrgnr), behandling);
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.getOrgnr());

            String fastTekst = "Kopi til " + orgDokument.getNavn();
            muligMottakerDtos.add(new MuligMottakerDto.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartMottaker, fastTekst))
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartMottaker.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build());
        }
        return muligMottakerDtos;
    }

    private OrganisasjonDokument hentRettOrganisasjonsdokument(Behandling behandling, String orgnr) {
        var kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        return (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
    }

    @Transactional
    public List<Produserbaredokumenter> hentMuligeProduserbaredokumenter(long behandlingId, Aktoersroller rolle) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);

        if (behandling.erInaktiv()) {
            return emptyList();
        }

        return switch (rolle) {
            case BRUKER -> {
                List<Produserbaredokumenter> brevmaler = new ArrayList<>();
                if (behandling.getFagsak().getTema() == Sakstemaer.MEDLEMSKAP_LOVVALG && behandling.getType() == Behandlingstyper.FØRSTEGANG) {
                    brevmaler.add(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
                }
                brevmaler.addAll(asList(MANGELBREV_BRUKER, GENERELT_FRITEKSTBREV_BRUKER));
                yield brevmaler;
            }
            case ARBEIDSGIVER -> List.of(MANGELBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
            case VIRKSOMHET -> Collections.singletonList(GENERELT_FRITEKSTBREV_VIRKSOMHET);
            case TRYGDEMYNDIGHET -> Collections.singletonList(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV);
            case ETAT -> Collections.singletonList(FRITEKSTBREV);
            default -> throw new FunksjonellException("Rollen " + rolle + " kan ikke sende brev gjennom brevmenyen");
        };
    }

    @Transactional
    public List<BrevAdresse> hentBrevAdresseTilMottakere(Aktoersroller aktoersroller, long behandlingId) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var mottakere = brevmottakerService.avklarMottakere(null, Mottaker.av(aktoersroller), behandling, false, false);
        List<BrevAdresse> brevAdresser = new ArrayList<>();

        for (Aktoer mottaker : mottakere) {
            brevAdresser.add(tilBrevAdresse(mottaker, behandling));
        }
        return brevAdresser;
    }

    private BrevAdresse tilBrevAdresse(Aktoer mottaker, Behandling behandling) {
        Persondata persondata = null;
        Kontaktopplysning kontaktopplysning = null;
        OrganisasjonDokument orgDokument = null;

        switch (mottaker.getRolle()) {
            case BRUKER: {
                persondata = persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
                break;
            }
            case REPRESENTANT: {
                if (mottaker.erPerson()) {
                    persondata = persondataFasade.hentPerson(mottaker.getPersonIdent());
                    break;
                }
            }
            case VIRKSOMHET, ARBEIDSGIVER: {
                kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(),
                    mottaker.getOrgnr()).orElse(null);
                String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : mottaker.getOrgnr();
                orgDokument = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
                break;
            }
            case TRYGDEMYNDIGHET: {
                var utenlandskMyndighet =
                    utenlandskMyndighetService.hentUtenlandskMyndighet(mottaker.hentMyndighetLandkode(), UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV);
                return new BrevAdresse.Builder()
                    .medMottakerNavn(utenlandskMyndighet.navn)
                    .medAdresselinjer(Stream.of(utenlandskMyndighet.gateadresse1, utenlandskMyndighet.gateadresse2).toList())
                    .medPostnr(utenlandskMyndighet.postnummer)
                    .medPoststed(utenlandskMyndighet.poststed)
                    .medLand(utenlandskMyndighet.land)
                    .build();
            }
            default:
                throw new FunksjonellException("Mottakersrolle støttes ikke: " + mottaker.getRolle());
        }

        if (orgDokument == null && persondata == null) {
            throw new FunksjonellException("Orgdata eller persondata forventes for å sende brev.");
        }

        return new BrevAdresse.Builder()
            .medMottakerNavn(mapNavn(orgDokument, persondata))
            .medOrgnr(orgDokument != null ? orgDokument.getOrgnummer() : null)
            .medAdresselinjer(mapAdresselinjer(orgDokument, null, kontaktopplysning, persondata))
            .medPostnr(mapPostnr(orgDokument, persondata))
            .medPoststed(orgDokument != null ? DokgenAdresseMapper.mapPoststed(orgDokument) : mapPoststed(persondata))
            .medRegion(mapRegionForAdresse(orgDokument, persondata))
            .medLand(mapLandForAdresse(orgDokument, persondata))
            .build();
    }

    private String mapPoststed(Persondata persondata) {
        Postadresse postadresse = persondata.hentGjeldendePostadresse();
        if (postadresse == null) {
            return null;
        }
        return postadresse.poststed();
    }

    @Transactional
    public void produserBrev(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        if (!BREV_TILGJENGELIG_FOR_MANUELL_BESTILLING.contains(brevbestillingRequest.getProduserbardokument())) {
            throw new FunksjonellException("Manuell bestilling av " + brevbestillingRequest.getProduserbardokument() + " er ikke støttet.");
        }
        dokumentServiceFasade.produserDokument(behandlingId, brevbestillingRequest);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingRequest brevbestillingRequest) {
        return dokumentServiceFasade.produserUtkast(behandlingID, brevbestillingRequest);
    }

    public List<Etat> hentTilgjengeligeEtater() {
        return List.of(Etat.SKATTEETATEN_ORGNR, Etat.SKATTINNKREVER_UTLAND_ORGNR, Etat.HELFO_ORGNR);
    }
}
