package no.nav.melosys.service.brev;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapAdresselinjer;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapLandForAdresse;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapMottakerNavn;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapPostnr;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapPoststed;

@Service
public class BrevbestillingService {

    private final DokumentServiceFasade dokumentServiceFasade;
    private final DokgenService dokgenService;
    private final BrevmottakerService brevmottakerService;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final KodeverkService kodeverkService;

    @Autowired
    public BrevbestillingService(DokumentServiceFasade dokumentServiceFasade, DokgenService dokgenService, BrevmottakerService brevmottakerService, PersondataFasade persondataFasade, EregFasade eregFasade, KontaktopplysningService kontaktopplysningService, KodeverkService kodeverkService) {
        this.dokumentServiceFasade = dokumentServiceFasade;
        this.dokgenService = dokgenService;
        this.brevmottakerService = brevmottakerService;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.kodeverkService = kodeverkService;
    }

    public List<Produserbaredokumenter> hentMuligeProduserbaredokumenter(Behandling behandling) {
        List<Produserbaredokumenter> brevmaler = new ArrayList<>(asList(MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER));

        if (behandling.getType() == Behandlingstyper.SOEKNAD) {
            brevmaler.add(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        }
        if (behandling.erKlage()) {
            brevmaler.add(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE);
        }

        return behandling.erAktiv() ? brevmaler : emptyList();
    }

    public List<BrevAdresse> hentBrevAdresseTilMottakere(Produserbaredokumenter produserbaredokumenter, Aktoersroller aktoersroller, Behandling behandling) throws FunksjonellException, TekniskException {
        var mottakere = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(aktoersroller), behandling, false, false);
        List<BrevAdresse> brevAdresser = new ArrayList<>();

        for (Aktoer mottaker : mottakere) {
            brevAdresser.add(tilBrevAdresse(mottaker, behandling));
        }
        return brevAdresser;
    }

    private BrevAdresse tilBrevAdresse(Aktoer mottaker, Behandling behandling) throws TekniskException, FunksjonellException {
        PersonDokument personDokument = null;
        Kontaktopplysning kontaktopplysning = null;
        OrganisasjonDokument orgDokument = null;

        if (mottaker.getRolle() == Aktoersroller.BRUKER) {
            personDokument = (PersonDokument) persondataFasade.hentPerson(behandling.hentPersonDokument().fnr, Informasjonsbehov.STANDARD).getDokument();
        }
        else if (mottaker.getRolle() == Aktoersroller.ARBEIDSGIVER || mottaker.getRolle() == Aktoersroller.REPRESENTANT) {
            kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker.getOrgnr()).orElse(null);
            String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : mottaker.getOrgnr();
            orgDokument = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
        }

        return new BrevAdresse.Builder()
            .medMottakerNavn(mapMottakerNavn(orgDokument, personDokument))
            .medOrgnr(orgDokument != null ? orgDokument.getOrgnummer() : null)
            .medAdresselinjer(mapAdresselinjer(orgDokument, null, kontaktopplysning, personDokument))
            .medPostnr(mapPostnr(orgDokument, personDokument))
            .medPoststed(orgDokument != null ? mapPoststed(orgDokument) : kodeverkService.dekod(FellesKodeverk.POSTNUMMER, personDokument.gjeldendePostadresse.postnr, LocalDate.now()))
            .medLand(mapLandForAdresse(orgDokument, personDokument))
            .build();
    }

    public void produserBrev(long behandlingID, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {

        dokgenService.produserOgDistribuerBrev(brevbestillingDto.getProduserbardokument(), behandlingID, brevbestillingDto);
    }

    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto)
        throws FunksjonellException, TekniskException {

        return dokumentServiceFasade.produserUtkast(brevbestillingDto.getProduserbardokument(), behandlingID, brevbestillingDto);
    }
}
