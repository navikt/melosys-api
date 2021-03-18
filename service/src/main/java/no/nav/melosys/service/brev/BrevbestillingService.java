package no.nav.melosys.service.brev;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapAdresselinjer;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapLandForAdresse;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapPostnr;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.mapPoststed;

@Service
public class BrevbestillingService {

    private final BehandlingService behandlingService;
    private final DokumentServiceFasade dokumentServiceFasade;
    private final DokgenService dokgenService;
    private final BrevmottakerService brevmottakerService;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;

    @Autowired
    public BrevbestillingService(BehandlingService behandlingService, DokumentServiceFasade dokumentServiceFasade, DokgenService dokgenService, BrevmottakerService brevmottakerService, PersondataFasade persondataFasade, EregFasade eregFasade, KontaktopplysningService kontaktopplysningService) {
        this.behandlingService = behandlingService;
        this.dokumentServiceFasade = dokumentServiceFasade;
        this.dokgenService = dokgenService;
        this.brevmottakerService = brevmottakerService;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
    }

    public List<Produserbaredokumenter> hentBrevMaler(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Produserbaredokumenter> brevmaler = asList(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER);

        return behandling.erAktiv() ? brevmaler : emptyList();
    }

    public List<BrevAdresse> hentBrevAdresseTilMottakere(Produserbaredokumenter produserbaredokumenter, Aktoersroller aktoersroller, Behandling behandling) throws FunksjonellException, TekniskException {
        var mottakere = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(aktoersroller), behandling);
        List<BrevAdresse> brevAdresser = new ArrayList<>();

        for (Aktoer mottaker : mottakere) {
            if (mottaker.getRolle() == Aktoersroller.BRUKER) {
                PersonDokument personDokument = (PersonDokument) persondataFasade.hentPerson(behandling.hentPersonDokument().fnr, Informasjonsbehov.STANDARD).getDokument();
                brevAdresser.add(new BrevAdresse(
                    personDokument.sammensattNavn,
                    null,
                    mapAdresselinjer(null, null, null, personDokument),
                    mapPostnr(null, personDokument),
                    mapPoststed(null, personDokument),
                    mapLandForAdresse(null, personDokument)
                    )
                );
            }
            else if (mottaker.getRolle() == Aktoersroller.ARBEIDSGIVER || mottaker.getRolle() == Aktoersroller.REPRESENTANT) {
                OrganisasjonDokument organisasjonDokument = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottaker.getOrgnr()).getDokument();
                Kontaktopplysning kontaktopplysninger = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker.getOrgnr()).orElse(null);
                brevAdresser.add(new BrevAdresse(
                    organisasjonDokument.getNavn(),
                    organisasjonDokument.getOrgnummer(),
                    mapAdresselinjer(organisasjonDokument, null, kontaktopplysninger, null),
                    mapPostnr(organisasjonDokument, null),
                    mapPoststed(organisasjonDokument, null),
                    mapLandForAdresse(organisasjonDokument, null)
                    )
                );
            }
        }
        return brevAdresser;
    }

    public void produserBrev(Produserbaredokumenter produserbartDokument, long behandlingID, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        //TODO Legge til valg av mal basert på brevbestilling.mottaker (rolle)
        dokgenService.produserOgDistribuerBrev(produserbartDokument, behandlingID, brevbestillingDto);
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartDokument, long behandlingID, BrevbestillingDto brevbestillingDto)
        throws FunksjonellException, TekniskException {
        //TODO Legge til valg av mal basert på brevbestilling.mottaker (rolle)
        return dokumentServiceFasade.produserUtkast(produserbartDokument, behandlingID, brevbestillingDto);
    }
}
