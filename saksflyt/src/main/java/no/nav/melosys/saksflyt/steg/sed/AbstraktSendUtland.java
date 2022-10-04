package no.nav.melosys.saksflyt.steg.sed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

public abstract class AbstraktSendUtland implements StegBehandler {
    protected final EessiService eessiService;
    protected final BehandlingsresultatService behandlingsresultatService;

    protected AbstraktSendUtland(EessiService eessiService,
                                 BehandlingsresultatService behandlingsresultatService) {
        this.eessiService = eessiService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    protected SendUtlandStatus sendUtland(BucType bucType, Prosessinstans prosessinstans) {
        return sendUtland(bucType, prosessinstans, Collections.emptySet());
    }

    protected SendUtlandStatus sendUtland(BucType bucType, Prosessinstans prosessinstans, Collection<Vedlegg> vedlegg) {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (skalSendesUtland(behandlingsresultat)) {
            Set<String> mottakerinstitusjoner = prosessinstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<Set<String>>() {});

            if (!CollectionUtils.isEmpty(mottakerinstitusjoner)) {
                eessiService.opprettOgSendSed(behandlingID, new ArrayList<>(mottakerinstitusjoner), bucType, vedlegg,
                    prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED));
                return SendUtlandStatus.SED_SENDT;
            } else {
                sendBrev(prosessinstans);
                return SendUtlandStatus.BREV_SENDT;
            }
        }
        return SendUtlandStatus.IKKE_SENDT;
    }

    protected abstract void sendBrev(Prosessinstans prosessinstans);

    protected abstract boolean skalSendesUtland(Behandlingsresultat behandlingsresultat);

    protected String hentBegrunnelsekodeTilForkortetPeriode(Prosessinstans prosessinstans) {
        Behandlingsresultat behandlingsresultat =
            behandlingsresultatService.hentBehandlingsresultatMedAvklartefakta(prosessinstans.getBehandling().getId());
        return behandlingsresultat.getAvklartefakta().stream()
            .filter(avklartfakta -> avklartfakta.getType() == Avklartefaktatyper.AARSAK_ENDRING_PERIODE)
            .map(Avklartefakta::getFakta)
            .map(Endretperiode::valueOf)
            .map(Endretperiode::getKode)
            .findFirst()
            .orElse(null);
    }

    protected String hentSaksbehandler(Prosessinstans prosessinstans) {
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
