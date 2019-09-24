package no.nav.melosys.service.dokument;

import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlagFactory;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER;

@Service
@Primary
public class DokumentService {
    private static final Logger log = LoggerFactory.getLogger(DokumentService.class);

    private final BehandlingService behandlingService;
    private final BrevDataService brevDataService;
    private final DoksysFasade dokSysFasade;
    private final ProsessinstansService prosessinstansService;
    private final BrevmottakerService brevmottakerService;
    private final BrevDataByggerVelger brevDataByggerVelger;
    private final DokumentdataGrunnlagFactory dokumentdataGrunnlagFactory;

    @Autowired
    public DokumentService(BehandlingService behandlingService,
                           BrevDataService brevDataService, DoksysFasade dokSysFasade,
                           ProsessinstansService prosessinstansService,
                           BrevmottakerService brevmottakerService,
                           BrevDataByggerVelger brevDataByggerVelger,
                           DokumentdataGrunnlagFactory dokumentdataGrunnlagFactory) {
        this.behandlingService = behandlingService;
        this.brevDataService = brevDataService;
        this.dokSysFasade = dokSysFasade;
        this.prosessinstansService = prosessinstansService;
        this.brevmottakerService = brevmottakerService;
        this.brevDataByggerVelger = brevDataByggerVelger;
        this.dokumentdataGrunnlagFactory = dokumentdataGrunnlagFactory;
    }

    /**
     * Kaller Doksys for å produsere et dokumentutkast
     */
    // Bruker Transactional for å støtte lazy loading gjennom Hibernate,
    // selv om dataene som hentes ut egentlig er read-only. Det ser ut til å
    // være påkrevd for Hibernate å finne en sesjon via Spring-transaksjonen
    // for å kunne laste lazy collections i objektgrafen.
    @Transactional(readOnly = true)
    public byte[] produserUtkast(long behandlingID, Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto)
        throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        DokumentdataGrunnlag brevdataRessurser = dokumentdataGrunnlagFactory.av(behandling);
        BrevDataBygger bygger = brevDataByggerVelger.hent(produserbartDokument, brevbestillingDto);
        BrevData brevData = bygger.lag(brevdataRessurser, SubjectHandler.getInstance().getUserID());

        Aktoersroller mottakerRolle = brevbestillingDto.mottaker == null ? brevmottakerService.avklarMottakerRolleFraDokument(produserbartDokument) : brevbestillingDto.mottaker;
        List<Aktoer> mottakere = brevmottakerService.avklarMottakere(produserbartDokument, Mottaker.av(mottakerRolle), behandling, true);

        if (mottakere.isEmpty()) {
            log.info("Ingen mottaker funnet for {}, {}", produserbartDokument, brevbestillingDto);
            throw new FunksjonellException("Ingen mottaker funnet for sak " + behandling.getFagsak().getSaksnummer());
        } else {
            return dokSysFasade.produserDokumentutkast(lagDokumentbestilling(produserbartDokument, mottakere.get(0), behandling, brevData));
        }
    }

    /**
     * Produserer et dokument i Doksys
     */
    public void produserDokument(Produserbaredokumenter produserbartDokument, Mottaker mottaker, long behandlingID, BrevData brevData)
        throws TekniskException, FunksjonellException {
        Assert.notNull(produserbartDokument, "Ingen gyldig produserbartDokument.");
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        List<Aktoer> mottakere = brevmottakerService.avklarMottakere(produserbartDokument, mottaker, behandling);
        for (Aktoer aktør : mottakere) {
            produserIkkeredigerbartDokument(produserbartDokument, aktør, behandling, brevData);
        }
    }

    private void produserIkkeredigerbartDokument(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData)
        throws FunksjonellException, TekniskException {
        dokSysFasade.produserIkkeredigerbartDokument(lagDokumentbestilling(produserbartDokument, mottaker, behandling, brevData));
    }

    private Dokumentbestilling lagDokumentbestilling(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Behandling behandling, BrevData brevData)
        throws FunksjonellException, TekniskException {
        Kontaktopplysning kontaktopplysning = brevmottakerService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker);
        DokumentbestillingMetadata metadata = brevDataService.lagBestillingMetadata(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        Element brevinnhold = brevDataService.lagBrevXML(produserbartDokument, mottaker, kontaktopplysning, behandling, brevData);
        return new Dokumentbestilling(metadata, brevinnhold);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void produserDokumentISaksflyt(Produserbaredokumenter produserbartDokument, Aktoersroller mottaker, long behandlingID, BrevData brevdata)
        throws FunksjonellException {
        Assert.notNull(mottaker, "Dokument uten mottaker.");
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (produserbartDokument == MELDING_MANGLENDE_OPPLYSNINGER) {
            prosessinstansService.opprettProsessinstansMangelbrev(behandling, mottaker, brevdata);
        } else {
            throw new FunksjonellException("Produserbaredokumenter " + produserbartDokument + " er ikke støttet.");
        }
    }
}