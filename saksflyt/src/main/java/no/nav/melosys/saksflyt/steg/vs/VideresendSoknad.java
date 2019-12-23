package no.nav.melosys.saksflyt.steg.vs;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
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
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.LandvelgerService;
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

    private final EessiService eessiService;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final JoarkFasade joarkFasade;
    private final FagsakService fagsakService;

    @Autowired
    protected VideresendSoknad(EessiService eessiService,
                               BehandlingsresultatService behandlingsresultatService,
                               LandvelgerService landvelgerService,
                               TpsFasade tpsFasade,
                               UtenlandskMyndighetService utenlandskMyndighetService,
                               @Qualifier("system") JoarkFasade joarkFasade,
                               FagsakService fagsakService) {
        super(eessiService, behandlingsresultatService, landvelgerService);
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.joarkFasade = joarkFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return VS_SEND_SOKNAD;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        SendUtlandStatus sendtStatus = sendUtland(BucType.LA_BUC_03, prosessinstans, hentSøknadDokument(prosessinstans.getBehandling()));

        if (sendtStatus == SendUtlandStatus.SED_SENDT) {
            prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
        } else if (sendtStatus == SendUtlandStatus.BREV_SENDT) {
            prosessinstans.setSteg(VS_DISTRIBUER_JOURNALPOST);
        } else {
            throw new TekniskException("Verken SED eller brev ble sendt for behandling " + prosessinstans.getBehandling().getId());
        }
    }

    private byte[] hentSøknadDokument(Behandling behandling) throws FunksjonellException {
        String journalpostID = behandling.getInitierendeJournalpostId();
        String dokumentID = behandling.getInitierendeDokumentId();

        if (StringUtils.isEmpty(journalpostID)) {
            throw new FunksjonellException("JournalpostID til behandling " + behandling.getId() + " finnes ikke!");
        } else if (StringUtils.isEmpty(dokumentID)) {
            throw new FunksjonellException("DokumentID til behandling " + behandling.getId() + " finnes ikke!");
        }

        return joarkFasade.hentDokument(journalpostID, dokumentID);
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
        UtenlandskMyndighet utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);
        String institusjonID = utenlandskMyndighetService.lagInstitusjonsId(utenlandskMyndighet);
        String institusjonNavn = utenlandskMyndighet.navn;

        String fnr = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), fnr, SedType.A008, eessiService.genererSedForhåndsvisning(behandling.getId(), SedType.A008),
            institusjonID, institusjonNavn, landkode.getKode(), lagSøknadVedlegg(behandling)
        );

        String journalpostID = joarkFasade.opprettJournalpost(opprettJournalpost, true);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
    }

    private List<FysiskDokument> lagSøknadVedlegg(Behandling behandling) throws FunksjonellException, IntegrasjonException {
        byte[] vedleggData = hentSøknadDokument(behandling);

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
