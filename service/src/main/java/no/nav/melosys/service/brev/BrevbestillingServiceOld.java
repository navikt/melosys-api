package no.nav.melosys.service.brev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.FastMottakerMedOrgnr;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Deprecated(since = "Ta vekk sammens med melosys.MEL-4835.refactor1 toggle, erstattet av BrevbestillingFasade")
@Service
public class BrevbestillingServiceOld {

    @Deprecated(since = "Ta vekk sammens med melosys.MEL-4835.refactor1")
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

    public BrevbestillingServiceOld(BrevmottakerService brevmottakerService,
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

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    @Transactional
    public MuligeBrevmottakereDto hentMuligeMottakere(Produserbaredokumenter produserbaredokumenter, long behandlingId, String orgnrTilValgtArbeidsgiver) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Mottakerliste mottakerliste = brevmottakerService.hentMottakerliste(produserbaredokumenter, behandlingId);
        return new MuligeBrevmottakereDto(
            lagHovedMottakerMuligMottakerDto(produserbaredokumenter, behandling, mottakerliste.getHovedMottaker(), orgnrTilValgtArbeidsgiver),
            lagKopiMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getKopiMottakere(), mottakerliste.getHovedMottaker()),
            lagFasteMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getFasteMottakere()));
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private Brevmottaker lagHovedMottakerMuligMottakerDto(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller hovedmottaker, String orgnrTilValgtArbeidsgiver) {
        return new Brevmottaker.Builder()
            .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, produserbaredokumenter, hovedmottaker))
            .medMottakerNavn(hentMottakerNavn(produserbaredokumenter, behandling, hovedmottaker, orgnrTilValgtArbeidsgiver))
            .medRolle(hovedmottaker)
            .build();
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
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
            case ARBEIDSGIVER, VIRKSOMHET -> {
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
    private List<Brevmottaker> lagKopiMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<Aktoersroller> kopiMottakere, Aktoersroller hovedmottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();
        for (Aktoersroller kopiMottaker : kopiMottakere) {
            switch (kopiMottaker) {
                case BRUKER ->
                    brevmottakere.add(lagKopiMottakerForBruker(produserbaredokumenter, behandling, kopiMottaker, hovedmottaker));
                case ARBEIDSGIVER ->
                    brevmottakere.addAll(lagKopiMottakereForArbeidsgiver(produserbaredokumenter, behandling, kopiMottaker));
                case TRYGDEMYNDIGHET ->
                    brevmottakere.addAll(lagKopiMottakereForMyndighet(produserbaredokumenter, behandling, kopiMottaker));
                default -> throw new IllegalStateException(kopiMottaker + " er ikke en gyldig kopiMottakerrolle");
            }
        }
        return brevmottakere;
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private Brevmottaker lagKopiMottakerForBruker(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker, Aktoersroller hovedmottaker) {
        Aktoer avklartKopi = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling);
        if (avklartKopi.getRolle() == BRUKER || hovedmottaker == kopiMottaker) {
            String aktørID = behandling.getFagsak().hentBrukersAktørID();
            return new Brevmottaker.Builder()
                .medDokumentNavn("Kopi til bruker")
                .medMottakerNavn(persondataFasade.hentSammensattNavn(aktørID))
                .medRolle(BRUKER)
                .medAktørId(aktørID)
                .build();
        } else {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            return new Brevmottaker.Builder()
                .medDokumentNavn("Kopi til brukers fullmektig")
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartKopi.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build();
        }
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private List<Brevmottaker> lagKopiMottakereForArbeidsgiver(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        List<Aktoer> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling, false, true);
        for (Aktoer avklartKopi : avklarteKopier) {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            String fastTekst = avklartKopi.getRolle() == ARBEIDSGIVER ? "Kopi til arbeidsgiver" : "Kopi til arbeidsgivers fullmektig";
            brevmottakere.add(new Brevmottaker.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartKopi, fastTekst))
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartKopi.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build());
        }
        return brevmottakere;
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private List<Brevmottaker> lagKopiMottakereForMyndighet(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        List<Aktoer> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling);
        for (Aktoer avklartKopi : avklarteKopier) {
            String fastTekst = "Kopi til utenlandsk trygdemyndighet";
            brevmottakere.add(new Brevmottaker.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartKopi, fastTekst))
                .medMottakerNavn("Utenlandsk trygdemyndighet")
                .medRolle(avklartKopi.getRolle())
                .medInstitusjonId(avklartKopi.getInstitusjonId())
                .build());
        }
        return brevmottakere;
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private List<Brevmottaker> lagFasteMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<FastMottakerMedOrgnr> fasteMottakere) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        for (FastMottakerMedOrgnr fastMottakerMedOrgnr : fasteMottakere) {
            Aktoer avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, FastMottakerMedOrgnr.av(fastMottakerMedOrgnr), behandling);
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.getOrgnr());

            String fastTekst = "Kopi til " + orgDokument.getNavn();
            brevmottakere.add(new Brevmottaker.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartMottaker, fastTekst))
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartMottaker.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build());
        }
        return brevmottakere;
    }

    @Deprecated(since = "Ta vekk sammens med melosys.MEL-4835.refactor1 toggle")
    @Transactional
    public void produserBrev(long behandlingId, BrevbestillingDto brevbestillingDto) {
        if (!BREV_TILGJENGELIG_FOR_MANUELL_BESTILLING.contains(brevbestillingDto.getProduserbardokument())) {
            throw new FunksjonellException("Manuell bestilling av " + brevbestillingDto.getProduserbardokument() + " er ikke støttet.");
        }
        dokumentServiceFasade.produserDokument(behandlingId, brevbestillingDto);
    }

    @Deprecated(since = "Tas vekk sammen med melosys.MEL-4835.refactor1 toggle")
    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        return dokumentServiceFasade.produserUtkast(behandlingID, brevbestillingDto);
    }

    @Deprecated(since = "Ta vekk sammen med melosys.MEL-4835.refactor1 unleash toggle")
    private OrganisasjonDokument hentRettOrganisasjonsdokument(Behandling behandling, String orgnr) {
        var kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        return (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
    }
}
