package no.nav.melosys.saksflyt.steg.sed;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.TRYGDEMYNDIGHET;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.YTTERLIGERE_INFO_SED;

@Component
public class SendAnmodningOmUnntak extends AbstraktSendUtland {
    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;
    private final AnmodningsperiodeService anmodningsperiodeService;

    private static final ZoneId TIME_ZONE_ID = ZoneId.systemDefault();
    private static final int SVARFRIST_MÅNEDER = 2;

    public SendAnmodningOmUnntak(@Qualifier("system") EessiService eessiService,
                                 BrevBestiller brevBestiller,
                                 BehandlingService behandlingService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 AnmodningsperiodeService anmodningsperiodeService) {
        super(eessiService, behandlingsresultatService);
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_ANMODNING_OM_UNNTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        Behandling behandling = prosessinstans.getBehandling();
        LocalDateTime svarFristDato = LocalDateTime.now().plusMonths(SVARFRIST_MÅNEDER);
        behandling.setDokumentasjonSvarfristDato(svarFristDato.atZone(TIME_ZONE_ID).toInstant());
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        behandlingService.lagre(behandling);
        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(behandling.getId());

        sendUtland(BucType.LA_BUC_01, prosessinstans, hentVedlegg(prosessinstans));
    }

    private Collection<Vedlegg> hentVedlegg(Prosessinstans prosessinstans) {
        final Set<DokumentReferanse> vedleggReferanser = prosessinstans.getData(ProsessDataKey.VEDLEGG_SED,
            new TypeReference<>() {
            }, Collections.emptySet());
        return eessiService.lagEessiVedlegg(prosessinstans.getBehandling().getFagsak(),
            vedleggReferanser);
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) {
        brevBestiller.bestill(lagBrevBestilling(prosessinstans));
    }

    private DoksysBrevbestilling lagBrevBestilling(Prosessinstans prosessinstans) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(prosessinstans.getBehandling().getId());
        return new DoksysBrevbestilling.Builder().medProduserbartDokument(Produserbaredokumenter.ANMODNING_UNNTAK)
            .medAvsenderID(hentSaksbehandler(prosessinstans))
            .medMottakere(Mottaker.av(TRYGDEMYNDIGHET))
            .medBehandling(behandling)
            .medYtterligereInformasjon(prosessinstans.getData(YTTERLIGERE_INFO_SED))
            .build();
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        Anmodningsperiode anmodningsperiode = behandlingsresultat.hentValidertAnmodningsperiode();
        return behandlingsresultat.erAnmodningOmUnntak()
            && (anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
            || anmodningsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2);
    }
}
