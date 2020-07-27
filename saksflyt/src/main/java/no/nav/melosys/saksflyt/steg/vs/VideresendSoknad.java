package no.nav.melosys.saksflyt.steg.vs;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.Vedlegg;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.arkiv.DokumentVariant.lagArkivVariant;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

/**
 * Sender et brev med søknad som vedlegg til utenlandsk myndighet
 *
 * Transisjoner:
 * VS_SEND_SOKNAD -> VS_SEND_SOKNAD eller FEILET_MASKINELT hvis feil
 */
@Component
public class VideresendSoknad extends AbstraktSendUtland {
    private static final Logger log = LoggerFactory.getLogger(VideresendSoknad.class);

    private final JoarkFasade joarkFasade;
    private final FagsakService fagsakService;
    private final SedSomBrevService sedSomBrevService;

    @Autowired
    protected VideresendSoknad(@Qualifier("system") EessiService eessiService,
                               BehandlingsresultatService behandlingsresultatService,
                               @Qualifier("system") JoarkFasade joarkFasade,
                               FagsakService fagsakService,
                               SedSomBrevService sedSomBrevService) {
        super(eessiService, behandlingsresultatService);
        this.joarkFasade = joarkFasade;
        this.fagsakService = fagsakService;
        this.sedSomBrevService = sedSomBrevService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return VS_SEND_SOKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        SendUtlandStatus sendtStatus = sendUtland(BucType.LA_BUC_03, prosessinstans, hentSøknadSomVedlegg(prosessinstans.getBehandling()));

        if (sendtStatus == SendUtlandStatus.SED_SENDT) {
            prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
        } else if (sendtStatus == SendUtlandStatus.BREV_SENDT) {
            prosessinstans.setSteg(VS_DISTRIBUER_JOURNALPOST);
        } else {
            throw new TekniskException("Verken SED eller brev ble sendt for behandling " + prosessinstans.getBehandling().getId());
        }
    }

    private Vedlegg hentSøknadSomVedlegg(Behandling behandling) throws FunksjonellException, IntegrasjonException {
        final String journalpostID = behandling.getInitierendeJournalpostId();

        if (StringUtils.isEmpty(journalpostID)) {
            throw new FunksjonellException("JournalpostID til behandling " + behandling.getId() + " finnes ikke!");
        }

        Journalpost journalpost = joarkFasade.hentJournalpost(journalpostID);
        String tittel = journalpost.getHoveddokument().getTittel();
        byte[] pdf = joarkFasade.hentDokument(journalpostID, journalpost.getHoveddokument().getDokumentId());

        return new Vedlegg(pdf, tittel);
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        return true;
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();

        // Fagsak må hentes på nytt fra detabasen da den har blitt oppdatert i AvklarMyndighet
        Fagsak fagsak = fagsakService.hentFagsak(behandling.getFagsak().getSaksnummer());
        behandling.setFagsak(fagsak);

        Landkoder landkode = fagsak.hentMyndighetLandkode();
        String journalpostID = sedSomBrevService
            .lagJournalpostForSendingAvSedSomBrev(SedType.A008, landkode, behandling, lagSøknadVedlegg(behandling));

        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
    }

    private List<FysiskDokument> lagSøknadVedlegg(Behandling behandling) throws FunksjonellException, IntegrasjonException {
        byte[] vedleggData = hentSøknadSomVedlegg(behandling).getInnhold();

        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setBrevkode(SedType.A008.name());
        fysiskDokument.setDokumentKategori(DokumentKategoriKode.SOK.getKode());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagArkivVariant(vedleggData)));
        fysiskDokument.setTittel(hentSøknadTittel(behandling));
        return Collections.singletonList(fysiskDokument);
    }

    private String hentSøknadTittel(Behandling behandling) throws FunksjonellException, IntegrasjonException {
        String journalpostID = behandling.getInitierendeJournalpostId();
        if (StringUtils.isEmpty(journalpostID)) {
            throw new FunksjonellException("JournalpostID til behandling " + behandling.getId() + " finnes ikke!");
        }
        return joarkFasade.hentJournalpost(journalpostID).getHoveddokument().getTittel();
    }
}
