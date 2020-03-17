package no.nav.melosys.saksflyt.steg;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

public abstract class AbstraktSendUtland extends AbstraktStegBehandler {
    protected final EessiService eessiService;
    protected final BehandlingsresultatService behandlingsresultatService;

    protected AbstraktSendUtland(EessiService eessiService,
                                 BehandlingsresultatService behandlingsresultatService) {
        this.eessiService = eessiService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    protected SendUtlandStatus sendUtland(BucType bucType, Prosessinstans prosessinstans) throws MelosysException {
        return sendUtland(bucType, prosessinstans, null);
    }

    protected SendUtlandStatus sendUtland(BucType bucType, Prosessinstans prosessinstans, byte[] vedlegg) throws MelosysException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        SendUtlandStatus sendUtlandStatus = SendUtlandStatus.IKKE_SENDT;

        if (skalSendesUtland(behandlingsresultat)) {
            List<String> mottakerinstitusjoner = prosessinstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>() {});

            if (!CollectionUtils.isEmpty(mottakerinstitusjoner)) {
                eessiService.opprettOgSendSed(behandlingID, mottakerinstitusjoner, bucType, vedlegg);
                sendUtlandStatus = SendUtlandStatus.SED_SENDT;
            } else {
                sendBrev(prosessinstans);
                sendUtlandStatus = SendUtlandStatus.BREV_SENDT;
            }
        }
        return sendUtlandStatus;
    }

    protected abstract void sendBrev(Prosessinstans prosessinstans) throws MelosysException;

    protected abstract boolean skalSendesUtland(Behandlingsresultat behandlingsresultat);

    protected String hentBegrunnelseKode(Prosessinstans prosessinstans) {
        Endretperiode endretPeriodeBegrunnelseKode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class);
        String begrunnelseKode = null;
        if (endretPeriodeBegrunnelseKode != null) {
            begrunnelseKode = endretPeriodeBegrunnelseKode.getKode();
        }
        return begrunnelseKode;
    }

    protected String hentSaksbehandler(Prosessinstans prosessinstans) throws IkkeFunnetException {
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        if (StringUtils.isEmpty(saksbehandler)) {
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(prosessinstans.getBehandling().getId());
            if (behandlingsresultat.erAutomatisert()) {
                saksbehandler = prosessinstans.getBehandling().getFagsak().getRegistrertAv();
            }
        }
        return saksbehandler;
    }

    public enum SendUtlandStatus {
        IKKE_SENDT,
        BREV_SENDT,
        SED_SENDT
    }
}
