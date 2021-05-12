package no.nav.melosys.service.dokument;

import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevdataGrunnlagFactory;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

@Service
@Primary
public class DokumentService {
    private static final Logger log = LoggerFactory.getLogger(DokumentService.class);

    private final BehandlingService behandlingService;
    private final BrevDataService brevDataService;
    private final DoksysFasade dokSysFasade;
    private final BrevmottakerService brevmottakerService;
    private final BrevDataByggerVelger brevDataByggerVelger;
    private final BrevdataGrunnlagFactory brevdataGrunnlagFactory;

    @Autowired
    public DokumentService(BehandlingService behandlingService,
                           BrevDataService brevDataService,
                           DoksysFasade dokSysFasade,
                           BrevmottakerService brevmottakerService,
                           BrevDataByggerVelger brevDataByggerVelger,
                           BrevdataGrunnlagFactory brevdataGrunnlagFactory) {
        this.behandlingService = behandlingService;
        this.brevDataService = brevDataService;
        this.dokSysFasade = dokSysFasade;
        this.brevmottakerService = brevmottakerService;
        this.brevDataByggerVelger = brevDataByggerVelger;
        this.brevdataGrunnlagFactory = brevdataGrunnlagFactory;
    }

    /**
     * Kaller Doksys for å produsere et dokumentutkast
     */
    // Bruker Transactional for å støtte lazy loading gjennom Hibernate,
    // selv om dataene som hentes ut egentlig er read-only. Det ser ut til å
    // være påkrevd for Hibernate å finne en sesjon via Spring-transaksjonen
    // for å kunne laste lazy collections i objektgrafen.
    @Transactional(readOnly = true)
    public byte[] produserUtkast(long behandlingID, BrevbestillingDto brevbestillingDto) {
        Produserbaredokumenter produserbartDokument = brevbestillingDto.getProduserbardokument();
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Aktoersroller mottakerRolle = brevbestillingDto.getMottaker() == null ?
            brevmottakerService.avklarMottakerRolleFraDokument(produserbartDokument) : brevbestillingDto.getMottaker();
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(produserbartDokument)
            .medAvsenderNavn(SubjectHandler.getInstance().getUserID())
            .medMottakerRolle(mottakerRolle)
            .medBehandling(behandling)
            .medBegrunnelseKode(brevbestillingDto.getBegrunnelseKode())
            .medFritekst(brevbestillingDto.getFritekst())
            .medYtterligereInformasjon(brevbestillingDto.getYtterligereInformasjon())
            .build();
        BrevData brevData = lagBrevData(brevbestilling);

        List<Aktoer> avklarteMottakere =
            brevmottakerService.avklarMottakere(produserbartDokument, Mottaker.av(mottakerRolle), behandling, true);

        if (avklarteMottakere.isEmpty()) {
            final var saksnummer = behandling.getFagsak().getSaksnummer();
            log.info("Ingen mottaker funnet for {}, {}", saksnummer, produserbartDokument);
            if (mottakerRolle == Aktoersroller.ARBEIDSGIVER) {
                throw new FunksjonellException("Melosys sender ikke brev til utenlandske arbeidsgivere uten orgnr."
                    + System.lineSeparator() + "Ingen orgn funnet for sak " + saksnummer);
            }
            throw new FunksjonellException("Ingen mottaker funnet for sak " + saksnummer);
        } else {
            return dokSysFasade.produserDokumentutkast(lagDokumentbestilling(produserbartDokument, avklarteMottakere.get(0), behandling, brevData));
        }
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(Produserbaredokumenter produserbartDokument,
                                 Mottaker mottaker,
                                 long behandlingID,
                                 DoksysBrevbestilling brevbestilling) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        // FIXME: Behandling i brevbestilling byttes ut med behandlingId for å samle ansvar for henting av saksopplysninger
        DoksysBrevbestilling nyBrevbestilling = new DoksysBrevbestilling.Builder()
            .medProduserbartDokument(brevbestilling.getProduserbartdokument())
            .medAvsenderNavn(brevbestilling.getAvsenderNavn())
            .medMottakerRolle(brevbestilling.getMottakerRolle())
            .medMottakere(brevbestilling.getMottakere())
            .medBehandling(behandling)
            .medBegrunnelseKode(brevbestilling.getBegrunnelseKode())
            .medFritekst(brevbestilling.getFritekst())
            .medYtterligereInformasjon(brevbestilling.getYtterligereInformasjon())
            .build();
        BrevData brevData = lagBrevData(nyBrevbestilling);

        List<Aktoer> mottakere = brevmottakerService.avklarMottakere(produserbartDokument, mottaker, behandling);
        for (Aktoer aktør : mottakere) {
            produserIkkeredigerbartDokument(produserbartDokument, aktør, behandling, brevData);
        }
    }

    private BrevData lagBrevData(DoksysBrevbestilling brevbestilling) {
        final var dokumentType = brevbestilling.getProduserbartdokument();
        Assert.notNull(dokumentType, "Ingen gyldig dokumentType.");

        BrevDataBygger brevDataBygger = brevDataByggerVelger.hent(dokumentType, lagBrevbestillingDto(brevbestilling));
        BrevDataGrunnlag brevDataGrunnlag = brevdataGrunnlagFactory.av(brevbestilling);

        return brevDataBygger.lag(brevDataGrunnlag, brevbestilling.getAvsenderNavn());
    }

    private static BrevbestillingDto lagBrevbestillingDto(DoksysBrevbestilling brevbestilling) {
        return new BrevbestillingDto.Builder()
            .medMottaker(brevbestilling.getMottakerRolle())
            .medBegrunnelseKode(brevbestilling.getBegrunnelseKode())
            .medFritekst(brevbestilling.getFritekst())
            .build();
    }

    private void produserIkkeredigerbartDokument(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData) {
        dokSysFasade.produserIkkeredigerbartDokument(lagDokumentbestilling(produserbartDokument, mottaker, behandling, brevData));
    }

    private Dokumentbestilling lagDokumentbestilling(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData) {
        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker);
        DokumentbestillingMetadata metadata = brevDataService.lagBestillingMetadata(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        Element brevinnhold = brevDataService.lagBrevXML(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        return new Dokumentbestilling(metadata, brevinnhold);
    }
}
