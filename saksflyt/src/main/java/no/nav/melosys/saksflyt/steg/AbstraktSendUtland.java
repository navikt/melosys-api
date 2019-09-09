package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.dokument.sed.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;

public abstract class AbstraktSendUtland extends AbstraktStegBehandler {
    private final EessiService eessiService;
    private final BrevBestiller brevBestiller;
    protected final BehandlingsresultatService behandlingsresultatService;
    private final LandvelgerService landvelgerService;

    protected AbstraktSendUtland(EessiService eessiService,
                                 BrevBestiller brevBestiller,
                                 BehandlingsresultatService behandlingsresultatService,
                                 LandvelgerService landvelgerService) {
        this.eessiService = eessiService;
        this.brevBestiller = brevBestiller;
        this.behandlingsresultatService = behandlingsresultatService;
        this.landvelgerService = landvelgerService;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (noeSkalSendes(behandlingsresultat)) {
            if (erEessiKlar(behandlingsresultat)) {
                eessiService.opprettOgSendSed(behandlingID);
            } else {
                brevBestiller.bestill(lagBrevBestilling(prosessinstans));
            }
        }
    }

    private boolean erEessiKlar(Behandlingsresultat behandlingsresultat) throws MelosysException {
        final String landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingsresultat.getBehandling()).stream().findFirst().map(Landkoder::getKode)
            .orElseThrow(() -> new FunksjonellException("Fant ikke trygdemyndighetsland for behandling " + behandlingsresultat.getBehandling().getId()));
        BucType bucType = LovvalgBestemmelseUtils.hentBucTypeFraBestemmelse(behandlingsresultat.hentBestemmelse());
        return eessiService.hentEessiMottakerinstitusjoner(bucType.toString()).stream().anyMatch(i -> i.getLandkode().equals(landkode));
    }

    protected abstract Brevbestilling lagBrevBestilling(Prosessinstans prosessinstans) throws IkkeFunnetException;

    protected abstract boolean noeSkalSendes(Behandlingsresultat behandlingsresultat);

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
}
