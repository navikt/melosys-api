package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpostUtils;
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
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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
    private final LandvelgerService landvelgerService;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final JoarkFasade joarkFasade;

    @Autowired
    protected VideresendSoknad(EessiService eessiService,
                               BehandlingsresultatService behandlingsresultatService,
                               LandvelgerService landvelgerService,
                               TpsFasade tpsFasade,
                               UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                               @Qualifier("system") JoarkFasade joarkFasade) {
        super(eessiService, behandlingsresultatService, landvelgerService);
        this.eessiService = eessiService;
        this.landvelgerService = landvelgerService;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.joarkFasade = joarkFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return VS_SEND_SOKNAD;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        sendUtland(BucType.LA_BUC_03, prosessinstans, hentSøknadDokument(prosessinstans.getBehandling()));

        if (prosessinstans.getSteg() != VS_DISTRIBUER_JOURNALPOST) {
            prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
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
        Fagsak fagsak = behandling.getFagsak();

        String fnr = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        Landkoder landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId()).stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Fant ikke trygdemyndighetsland for behandling " + behandling.getId()));
        String institusjonID = fagsak.hentMyndigheter().stream().findFirst()
            .orElseThrow(() -> new TekniskException("Finner ingen myndighet for fagsak " + fagsak.getSaksnummer())).getInstitusjonId();
        String institusjonNavn = utenlandskMyndighetRepository.findByLandkode(landkode).map(u -> u.navn).orElse("");

        OpprettJournalpost opprettJournalpost = OpprettJournalpostUtils.lagJournalpostForSendingAvSedSomBrev(
            fagsak.getGsakSaksnummer(), fnr, SedType.A008, eessiService.genererSedForhåndsvisning(behandling.getId(), SedType.A008),
            institusjonID, institusjonNavn, lagSøknadVedlegg(behandling)
        );

        String journalpostID = joarkFasade.opprettJournalpost(opprettJournalpost, true);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setSteg(VS_DISTRIBUER_JOURNALPOST);
    }

    private List<FysiskDokument> lagSøknadVedlegg(Behandling behandling) throws FunksjonellException, IntegrasjonException {
        byte[] vedleggData = hentSøknadDokument(behandling);

        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setBrevkode(SedType.A008.name());
        fysiskDokument.setDokumentKategori(DokumentKategoriKode.SOK.getKode());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(OpprettJournalpostUtils.lagArkivVariant(vedleggData)));
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
