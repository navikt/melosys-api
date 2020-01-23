package no.nav.melosys.saksflyt.steg;

import java.util.Optional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

public abstract class AbstraktSendUtland extends AbstraktStegBehandler {
    private final EessiService eessiService;
    protected final BehandlingsresultatService behandlingsresultatService;
    private final LandvelgerService landvelgerService;

    protected AbstraktSendUtland(EessiService eessiService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 LandvelgerService landvelgerService) {
        this.eessiService = eessiService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.landvelgerService = landvelgerService;
    }

    protected SendUtlandStatus sendUtland(BucType bucType, Prosessinstans prosessinstans) throws MelosysException {
        return sendUtland(bucType, prosessinstans, null);
    }

    protected SendUtlandStatus sendUtland(BucType bucType, Prosessinstans prosessinstans, byte[] vedlegg) throws MelosysException {
        return sendUtland(bucType, prosessinstans, null, null, vedlegg);
    }

    protected SendUtlandStatus sendUtland(BucType bucType, Prosessinstans prosessinstans, String land, String mottakerInstitusjon, byte[] vedlegg) throws MelosysException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (skalSendesUtland(behandlingsresultat)) {
            Optional<String> landkode = land != null
                ? Optional.of(land)
                : landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream().findFirst().map(Landkoder::getKode);
            if (landkode.isPresent()) {
                if (eessiService.landErEessiReady(bucType.name(), landkode.get())) {
                    if (mottakerInstitusjon == null) {
                        mottakerInstitusjon = prosessinstans.getData(ProsessDataKey.EESSI_MOTTAKER);
                    }
                    if (StringUtils.isEmpty(mottakerInstitusjon)) {
                        mottakerInstitusjon = eessiService.hentMottakerinstitusjonFraBuc(prosessinstans.getBehandling().getFagsak(), bucType);
                    }

                    eessiService.opprettOgSendSed(behandlingID, mottakerInstitusjon, bucType, vedlegg);
                    return SendUtlandStatus.SED_SENDT;
                } else {
                    sendBrev(prosessinstans);
                    return SendUtlandStatus.BREV_SENDT;
                }
            }
        }

        return SendUtlandStatus.IKKE_SENDT;
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
