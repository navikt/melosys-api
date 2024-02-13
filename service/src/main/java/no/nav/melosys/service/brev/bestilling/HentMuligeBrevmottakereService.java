package no.nav.melosys.service.brev.bestilling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
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

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV;

@Component
public class HentMuligeBrevmottakereService {
    private final BehandlingService behandlingService;
    private final BrevmottakerService brevmottakerService;
    private final DokumentNavnService dokumentNavnService;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public HentMuligeBrevmottakereService(BehandlingService behandlingService,
                                          BrevmottakerService brevmottakerService,
                                          DokumentNavnService dokumentNavnService,
                                          PersondataFasade persondataFasade,
                                          EregFasade eregFasade,
                                          KontaktopplysningService kontaktopplysningService,
                                          UtenlandskMyndighetService utenlandskMyndighetService) {
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
        this.dokumentNavnService = dokumentNavnService;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Transactional
    public ResponseDto hentMuligeBrevmottakere(RequestDto requestDto) {

        Produserbaredokumenter produserbaredokumenter = requestDto.produserbartdokument();
        Mottakerliste mottakerliste = brevmottakerService.hentMottakerliste(produserbaredokumenter, requestDto.behandlingID());

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(requestDto.behandlingID());
        Brevmottaker hovedMottaker = lagHovedMottakerMuligMottakerDto(produserbaredokumenter, behandling, mottakerliste.getHovedMottaker(),
            requestDto.orgnr(), requestDto.institusjonID());
        List<Brevmottaker> kopiMottakere = lagKopiMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getKopiMottakere(), mottakerliste.getHovedMottaker());
        List<Brevmottaker> fasteMottakere = lagFasteMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getFasteMottakere());

        return new ResponseDto(hovedMottaker, kopiMottakere, fasteMottakere);
    }

    private Brevmottaker lagHovedMottakerMuligMottakerDto(Produserbaredokumenter produserbaredokumenter, Behandling behandling,
                                                          Mottakerroller hovedmottaker, String orgnrTilValgtArbeidsgiver,
                                                          String institusjonID) {
        return new Brevmottaker.Builder()
            .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottakerrolle(behandling, produserbaredokumenter, hovedmottaker))
            .medMottakerNavn(hentMottakerNavn(produserbaredokumenter, behandling, hovedmottaker, orgnrTilValgtArbeidsgiver, institusjonID))
            .medRolle(hovedmottaker)
            .build();
    }

    private List<Brevmottaker> lagFasteMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<NorskMyndighet> fasteMottakere) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        for (NorskMyndighet fastMottaker : fasteMottakere) {
            Mottaker avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(fastMottaker), behandling);
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.getOrgnr());

            String fastTekst = "Kopi til " + orgDokument.getNavn();
            brevmottakere.add(new Brevmottaker.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottaker(behandling, produserbaredokumenter, avklartMottaker, fastTekst))
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartMottaker.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build());
        }
        return brevmottakere;
    }

    private Brevmottaker lagKopiMottakerForBruker(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Mottakerroller kopiMottaker, Mottakerroller hovedmottaker) {
        Mottaker avklartKopi = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.medRolle(kopiMottaker), behandling);
        if (avklartKopi.getRolle() == Mottakerroller.BRUKER || hovedmottaker == kopiMottaker) {
            String aktørID = behandling.getFagsak().hentBrukersAktørID();
            return new Brevmottaker.Builder()
                .medDokumentNavn("Kopi til bruker")
                .medMottakerNavn(persondataFasade.hentSammensattNavn(aktørID))
                .medRolle(Mottakerroller.BRUKER)
                .medAktørId(aktørID)
                .build();
        } else if (avklartKopi.getPersonIdent() != null) {
            return new Brevmottaker.Builder()
                .medDokumentNavn("Kopi til brukers fullmektig")
                .medMottakerNavn(persondataFasade.hentSammensattNavn(avklartKopi.getPersonIdent()))
                .medRolle(avklartKopi.getRolle())
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

    private String hentMottakerNavn(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Mottakerroller hovedmottaker,
                                    String orgnr, String institusjonID) {
        switch (hovedmottaker) {
            case BRUKER -> {
                Mottaker avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.medRolle(Mottakerroller.BRUKER), behandling);
                if (avklartMottaker.getRolle() == Mottakerroller.BRUKER) {
                    return persondataFasade.hentSammensattNavn(behandling.getFagsak().hentBrukersAktørID());
                } else if (avklartMottaker.getPersonIdent() != null) {
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
            case UTENLANDSK_TRYGDEMYNDIGHET -> {
                if (produserbaredokumenter == UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV) {
                    if (institusjonID != null) {
                        var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID(institusjonID);
                        if (!utenlandskMyndighet.getAdresse().erGyldig()) {
                            throw new FunksjonellException("Du kan ikke sende brev til trygdemyndigheten i %s, fordi korrekt adresse er ukjent.".formatted(utenlandskMyndighet.getLandkode().getBeskrivelse()));
                        }
                        return utenlandskMyndighet.getNavn();
                    } else {
                        Mottaker avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET), behandling);
                        var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(avklartMottaker.hentMyndighetLandkode(), produserbaredokumenter);
                        return utenlandskMyndighet.getNavn();
                    }
                } else {
                    throw new FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker);
                }
            }
            case NORSK_MYNDIGHET -> throw new FunksjonellException("Hent mottakere for norske mynigheter burde gå gjennom endepunktet mulige-mottakere-norske-myndigheter og støttes derfor ikke her");
            default -> throw new FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker);
        }
    }

    private OrganisasjonDokument hentRettOrganisasjonsdokument(Behandling behandling, String orgnr) {
        var kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        return (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
    }


    private List<Brevmottaker> lagKopiMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<Mottakerroller> kopiMottakere, Mottakerroller hovedmottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();
        for (Mottakerroller kopiMottaker : kopiMottakere) {
            switch (kopiMottaker) {
                case BRUKER -> brevmottakere.add(lagKopiMottakerForBruker(produserbaredokumenter, behandling, kopiMottaker, hovedmottaker));
                case ARBEIDSGIVER -> brevmottakere.addAll(lagKopiMottakereForArbeidsgiver(produserbaredokumenter, behandling, kopiMottaker));
                case UTENLANDSK_TRYGDEMYNDIGHET -> brevmottakere.addAll(lagKopiMottakereForUtenlandskTrygdemyndighet(produserbaredokumenter, behandling, kopiMottaker));
                default -> throw new IllegalStateException(kopiMottaker + " er ikke en gyldig kopiMottakerrolle");
            }
        }
        return brevmottakere;
    }

    private List<Brevmottaker> lagKopiMottakereForArbeidsgiver(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Mottakerroller kopiMottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        List<Mottaker> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.medRolle(kopiMottaker), behandling, false, true);
        for (Mottaker avklartKopi : avklarteKopier) {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            String fastTekst = avklartKopi.getRolle() == Mottakerroller.ARBEIDSGIVER ? "Kopi til arbeidsgiver" : "Kopi til arbeidsgivers fullmektig";
            brevmottakere.add(new Brevmottaker.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottaker(behandling, produserbaredokumenter, avklartKopi, fastTekst))
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartKopi.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build());
        }
        return brevmottakere;
    }

    private List<Brevmottaker> lagKopiMottakereForUtenlandskTrygdemyndighet(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Mottakerroller kopiMottaker) {
        List<Brevmottaker> brevmottakere = new ArrayList<>();

        List<Mottaker> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.medRolle(kopiMottaker), behandling);
        for (Mottaker avklartKopi : avklarteKopier) {
            String fastTekst = "Kopi til utenlandsk trygdemyndighet";
            brevmottakere.add(new Brevmottaker.Builder()
                .medDokumentNavn(dokumentNavnService.utledDokumentNavnForProduserbaredokumenterOgMottaker(behandling, produserbaredokumenter, avklartKopi, fastTekst))
                .medMottakerNavn("Utenlandsk trygdemyndighet")
                .medRolle(avklartKopi.getRolle())
                .medInstitusjonID(avklartKopi.getInstitusjonID())
                .build());
        }
        return brevmottakere;
    }


    public record RequestDto(Produserbaredokumenter produserbartdokument,
                             long behandlingID,
                             String orgnr,
                             String institusjonID) {
    }

    public record ResponseDto(Brevmottaker hovedMottaker,
                              List<Brevmottaker> kopiMottakere,
                              List<Brevmottaker> fasteMottakere) {
    }

}
