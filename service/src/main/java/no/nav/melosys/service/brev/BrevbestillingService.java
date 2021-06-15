package no.nav.melosys.service.brev;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.FastMottaker;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.dokument.*;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.*;

@Service
public class BrevbestillingService {

    private final DokumentServiceFasade dokumentServiceFasade;
    private final BrevmottakerService brevmottakerService;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final KodeverkService kodeverkService;

    @Autowired
    public BrevbestillingService(DokumentServiceFasade dokumentServiceFasade,
                                 BrevmottakerService brevmottakerService, PersondataFasade persondataFasade,
                                 EregFasade eregFasade, KontaktopplysningService kontaktopplysningService,
                                 KodeverkService kodeverkService) {
        this.dokumentServiceFasade = dokumentServiceFasade;
        this.brevmottakerService = brevmottakerService;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.kodeverkService = kodeverkService;
    }


    public MuligeMottakereDto hentMuligeMottakere(Produserbaredokumenter produserbaredokumenter, Behandling behandling, String orgnrTilValgtArbeidsgiver) {
        Mottakerliste mottakerliste = brevmottakerService.hentMottakerliste(produserbaredokumenter, behandling);
        return new MuligeMottakereDto(
            lagHovedMottakerMuligMottakerDto(produserbaredokumenter, behandling, mottakerliste.getHovedMottaker(), orgnrTilValgtArbeidsgiver),
            lagKopiMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getKopiMottakere(), mottakerliste.getHovedMottaker()),
            lagFasteMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.getFasteMottakere()));
    }

    private MuligMottakerDto lagHovedMottakerMuligMottakerDto(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller hovedmottaker, String orgnrTilValgtArbeidsgiver) {
        return new MuligMottakerDto.Builder()
            .medDokumentNavn(produserbaredokumenter.getBeskrivelse())
            .medMottakerNavn(hentMottakerNavn(produserbaredokumenter, behandling, hovedmottaker, orgnrTilValgtArbeidsgiver))
            .medRolle(hovedmottaker)
            .build();
    }

    private String hentMottakerNavn(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller hovedmottaker, String orgnrTilValgtArbeidsgiver) {
        if (hovedmottaker == Aktoersroller.BRUKER) {
            Aktoer avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(hovedmottaker), behandling);
            if (avklartMottaker.getRolle() == Aktoersroller.BRUKER) {
                return behandling.hentPersonDokument().getSammensattNavn();
            } else {
                var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.getOrgnr());
                return orgDokument.getNavn();
            }
        }
        if (hovedmottaker == Aktoersroller.ARBEIDSGIVER) {
            var orgDokument = (OrganisasjonDokument) eregFasade.hentOrganisasjon(orgnrTilValgtArbeidsgiver).getDokument();
            return orgDokument.getNavn();
        }
        throw new FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker);
    }

    private List<MuligMottakerDto> lagKopiMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<Aktoersroller> kopiMottakere, Aktoersroller hovedmottaker) {
        List<MuligMottakerDto> muligMottakerDtos = new ArrayList<>();
        for (Aktoersroller kopiMottaker : kopiMottakere) {
            if (kopiMottaker == Aktoersroller.BRUKER) {
                muligMottakerDtos.add(lagKopiMottakerForBruker(produserbaredokumenter, behandling, kopiMottaker, hovedmottaker));
            }
            if (kopiMottaker == Aktoersroller.ARBEIDSGIVER) {
                muligMottakerDtos.addAll(lagKopiMottakereForArbeidsgiver(produserbaredokumenter, behandling, kopiMottaker));
            }
        }
        return muligMottakerDtos;
    }

    private MuligMottakerDto lagKopiMottakerForBruker(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker, Aktoersroller hovedmottaker) {
        Aktoer avklartKopi = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling);
        if (avklartKopi.getRolle() == Aktoersroller.BRUKER || hovedmottaker == kopiMottaker) {
            return new MuligMottakerDto.Builder()
                .medDokumentNavn("Kopi til bruker")
                .medMottakerNavn(behandling.hentPersonDokument().getSammensattNavn())
                .medRolle(BRUKER)
                .medAktørId(behandling.getFagsak().hentBruker().getAktørId())
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

    private List<MuligMottakerDto> lagKopiMottakereForArbeidsgiver(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Aktoersroller kopiMottaker) {
        List<MuligMottakerDto> muligMottakerDtos = new ArrayList<>();

        List<Aktoer> avklarteKopier = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(kopiMottaker), behandling, false, true);
        for (Aktoer avklartKopi : avklarteKopier) {
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.getOrgnr());
            muligMottakerDtos.add(new MuligMottakerDto.Builder()
                .medDokumentNavn(avklartKopi.getRolle() == ARBEIDSGIVER ? "Kopi til arbeidsgiver" :  "Kopi til arbeidsgivers fullmektig")
                .medMottakerNavn(orgDokument.getNavn())
                .medRolle(avklartKopi.getRolle())
                .medOrgnr(orgDokument.getOrgnummer())
                .build());
        }
        return muligMottakerDtos;
    }

    private List<MuligMottakerDto> lagFasteMottakereMuligMottakerDtos(Produserbaredokumenter produserbaredokumenter, Behandling behandling, Collection<FastMottaker> fasteMottakere) {
        List<MuligMottakerDto> muligMottakerDtos = new ArrayList<>();

        for (FastMottaker fastMottaker : fasteMottakere) {
            Aktoer avklartMottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, FastMottaker.av(fastMottaker), behandling);
            var orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.getOrgnr());
            muligMottakerDtos.add(new MuligMottakerDto.Builder()
                .medDokumentNavn("Kopi til " + orgDokument.getNavn())
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

    public List<Produserbaredokumenter> hentMuligeProduserbaredokumenter(Behandling behandling) {
        List<Produserbaredokumenter> brevmaler = new ArrayList<>();

        if (behandling.getType() == Behandlingstyper.SOEKNAD) {
            brevmaler.add(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        } else if (behandling.erKlage()) {
            brevmaler.add(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE);
        }
        brevmaler.addAll(asList(MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER));

        return behandling.erAktiv() ? brevmaler : emptyList();
    }

    public List<BrevAdresse> hentBrevAdresseTilMottakere(Produserbaredokumenter produserbaredokumenter, Aktoersroller aktoersroller, Behandling behandling) {
        var mottakere = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(aktoersroller), behandling, false, false);
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

        if (mottaker.getRolle() == Aktoersroller.BRUKER) {
            persondata = (Persondata) persondataFasade.hentPersonFraTps(
                    behandling.hentPersonDokument().getFnr(), Informasjonsbehov.STANDARD).getDokument();

            } else if (mottaker.getRolle() == Aktoersroller.ARBEIDSGIVER || mottaker.getRolle() == Aktoersroller.REPRESENTANT) {
                kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker.getOrgnr()).orElse(null);
            String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : mottaker.getOrgnr();
            orgDokument = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
        }

        return new BrevAdresse.Builder()
            .medMottakerNavn(mapMottakerNavn(orgDokument, persondata))
            .medOrgnr(orgDokument != null ? orgDokument.getOrgnummer() : null)
            .medAdresselinjer(mapAdresselinjer(orgDokument, null, kontaktopplysning, persondata))
            .medPostnr(mapPostnr(orgDokument, persondata))
            .medPoststed(orgDokument != null ? mapPoststed(orgDokument) : kodeverkService.dekod(FellesKodeverk.POSTNUMMER, persondata.getGjeldendePostadresse().postnr, LocalDate.now()))
            .medLand(mapLandForAdresse(orgDokument, persondata))
            .build();
    }

    @Transactional
    public void produserBrev(long behandlingId, BrevbestillingDto brevbestillingDto) {
        dokumentServiceFasade.produserDokument(behandlingId, brevbestillingDto);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        return dokumentServiceFasade.produserUtkast(behandlingID, brevbestillingDto);
    }
}
