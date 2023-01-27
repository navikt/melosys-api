package no.nav.melosys.service.brev.hentmuligemottakere;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
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
import no.nav.melosys.service.brev.DokumentNavnService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV;

@Component
public class HentMuligeBrevmottakere {
    private final BehandlingService behandlingService;
    private final BrevmottakerService brevmottakerService;
    private final DokumentNavnService dokumentNavnService;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public HentMuligeBrevmottakere(BehandlingService behandlingService, BrevmottakerService brevmottakerService, DokumentNavnService dokumentNavnService, PersondataFasade persondataFasade, EregFasade eregFasade, KontaktopplysningService kontaktopplysningService, UtenlandskMyndighetService utenlandskMyndighetService) {
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
        this.dokumentNavnService = dokumentNavnService;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Transactional
    public HentMuligeBrevmottakereResponseDto hentMuligeBrevmottakere(HentMuligeBrevmottakereRequestDto hentMuligeBrevmottakereRequestDto) {

        Produserbaredokumenter produserbaredokumenter = hentMuligeBrevmottakereRequestDto.produserbartdokument();
        Mottakerliste mottakerliste = brevmottakerService.hentMottakerliste(produserbaredokumenter, hentMuligeBrevmottakereRequestDto.behandlingID());

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(hentMuligeBrevmottakereRequestDto.behandlingID());
        Brevmottaker hovedMottaker = lagHovedMottakerMuligMottakerDto(produserbaredokumenter, behandling, mottakerliste.getHovedMottaker(), hentMuligeBrevmottakereRequestDto.orgnr());
        List<Brevmottaker> kopiMottakere = lagKopiMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getKopiMottakere(), mottakerliste.getHovedMottaker());
        List<Brevmottaker> fasteMottakere = lagFasteMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getFasteMottakere());
        return new HentMuligeBrevmottakereResponseDto(hovedMottaker, kopiMottakere, fasteMottakere);
    }

    private Brevmottaker lagHovedMottakerMuligMottakerDto(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller hovedmottaker, String orgnrTilValgtArbeidsgiver) {
        return new Brevmottaker.Builder().medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(behandling, produserbaredokumenter, hovedmottaker)).medMottakerNavn(hentMottakerNavn(produserbaredokumenter, behandling, hovedmottaker, orgnrTilValgtArbeidsgiver)).medRolle(hovedmottaker).build();
    }

    private List<Brevmottaker> lagFasteMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<FastMottakerMedOrgnr> fasteMottakere) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        for (FastMottakerMedOrgnr fastMottakerMedOrgnr : fasteMottakere) {
            Aktoer avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, FastMottakerMedOrgnr.av(fastMottakerMedOrgnr), behandling);
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.getOrgnr());

            String fastTekst = "Kopi til " + orgDokument.getNavn();
            brevmottakere.add(new Brevmottaker.Builder().medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartMottaker, fastTekst)).medMottakerNavn(orgDokument.getNavn()).medRolle(avklartMottaker.getRolle()).medOrgnr(orgDokument.getOrgnummer()).build());
        }
        return brevmottakere;
    }

    private Brevmottaker lagKopiMottakerForBruker(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker, Aktoersroller hovedmottaker) {
        Aktoer avklartKopi = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling);
        if (avklartKopi.getRolle() == BRUKER || hovedmottaker == kopiMottaker) {
            String aktørID = behandling.getFagsak().hentBrukersAktørID();
            return new Brevmottaker.Builder().medDokumentNavn("Kopi til bruker").medMottakerNavn(persondataFasade.hentSammensattNavn(aktørID)).medRolle(BRUKER).medAktørId(aktørID).build();
        } else {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            return new Brevmottaker.Builder().medDokumentNavn("Kopi til brukers fullmektig").medMottakerNavn(orgDokument.getNavn()).medRolle(avklartKopi.getRolle()).medOrgnr(orgDokument.getOrgnummer()).build();
        }
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
                    UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(avklartMottaker.hentMyndighetLandkode());
                    return utenlandskMyndighet.navn;
                } else {
                    throw new FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker);
                }
            }
            default ->
                throw new FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker);
        }
    }

    private OrganisasjonDokument hentRettOrganisasjonsdokument(Behandling behandling, String orgnr) {
        var kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        return (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
    }


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

    private List<Brevmottaker> lagKopiMottakereForArbeidsgiver(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        List<Aktoer> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling, false, true);
        for (Aktoer avklartKopi : avklarteKopier) {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            String fastTekst = avklartKopi.getRolle() == ARBEIDSGIVER ? "Kopi til arbeidsgiver" : "Kopi til arbeidsgivers fullmektig";
            brevmottakere.add(new Brevmottaker.Builder().medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartKopi, fastTekst)).medMottakerNavn(orgDokument.getNavn()).medRolle(avklartKopi.getRolle()).medOrgnr(orgDokument.getOrgnummer()).build());
        }
        return brevmottakere;
    }

    private List<Brevmottaker> lagKopiMottakereForMyndighet(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        List<Aktoer> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling);
        for (Aktoer avklartKopi : avklarteKopier) {
            String fastTekst = "Kopi til utenlandsk trygdemyndighet";
            brevmottakere.add(new Brevmottaker.Builder().medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, avklartKopi, fastTekst)).medMottakerNavn("Utenlandsk trygdemyndighet").medRolle(avklartKopi.getRolle()).medInstitusjonId(avklartKopi.getInstitusjonId()).build());
        }
        return brevmottakere;
    }

}
