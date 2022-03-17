package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
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
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.domain.arkiv.DokumentVariant.lagDokumentVariant;

/**
 * Sender et brev med søknad som vedlegg til utenlandsk myndighet
 * <p>
 * Transisjoner:
 * VS_SEND_SOKNAD -> VS_SEND_SOKNAD eller FEILET_MASKINELT hvis feil
 */
@Component
public class VideresendSoknad extends AbstraktSendUtland {
    private static final Logger log = LoggerFactory.getLogger(VideresendSoknad.class);

    private final JoarkFasade joarkFasade;
    private final FagsakService fagsakService;
    private final SedSomBrevService sedSomBrevService;

    protected VideresendSoknad(@Qualifier("system") EessiService eessiService,
                               BehandlingsresultatService behandlingsresultatService,
                               @Qualifier("system") JoarkFasade joarkFasade, FagsakService fagsakService,
                               SedSomBrevService sedSomBrevService) {
        super(eessiService, behandlingsresultatService);
        this.joarkFasade = joarkFasade;
        this.fagsakService = fagsakService;
        this.sedSomBrevService = sedSomBrevService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.VIDERESEND_SØKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        SendUtlandStatus sendtStatus = sendUtland(
            BucType.LA_BUC_03,
            prosessinstans,
            hentVedlegg(prosessinstans)
        );

        log.info("Status på sending av søknad til utenlandsk myndighet for behandling {}: {}",
            prosessinstans.getBehandling().getId(), sendtStatus);
    }

    private Collection<Vedlegg> hentVedlegg(Prosessinstans prosessinstans) {
        final Set<DokumentReferanse> vedleggReferanser = prosessinstans.getData(ProsessDataKey.VEDLEGG_SED,
            new TypeReference<Set<DokumentReferanse>>() {
            });
        if (CollectionUtils.isEmpty(vedleggReferanser)) {
            throw new FunksjonellException("Kan ikke videresende søknad uten vedlegg!");
        }

        return eessiService.lagEessiVedlegg(prosessinstans.getBehandling().getFagsak(), vedleggReferanser);
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        return true;
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) {
        Behandling behandling = prosessinstans.getBehandling();

        // Fagsak må hentes på nytt fra db da den har blitt oppdatert i AvklarMyndighet
        Fagsak fagsak = fagsakService.hentFagsak(behandling.getFagsak().getSaksnummer());
        behandling.setFagsak(fagsak);

        Landkoder mottakerLandkode = fagsak.hentMyndighetLandkode();
        String journalpostID = sedSomBrevService
            .lagJournalpostForSendingAvSedSomBrev(SedType.A008, mottakerLandkode, behandling, lagSøknadVedlegg(behandling));

        prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, mottakerLandkode);
    }

    private List<FysiskDokument> lagSøknadVedlegg(Behandling behandling) {
        final String journalpostID = behandling.getInitierendeJournalpostId();
        if (StringUtils.isEmpty(journalpostID)) {
            throw new FunksjonellException("JournalpostID til behandling " + behandling.getId() + " finnes ikke!");
        }
        Journalpost journalpost = joarkFasade.hentJournalpost(journalpostID);
        byte[] vedleggData = joarkFasade.hentDokument(journalpostID, journalpost.getHoveddokument().getDokumentId());

        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setBrevkode(SedType.A008.name());
        fysiskDokument.setDokumentKategori(DokumentKategoriKode.SOK.getKode());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagDokumentVariant(vedleggData)));
        fysiskDokument.setTittel(journalpost.getHoveddokument().getTittel());
        return Collections.singletonList(fysiskDokument);
    }
}
