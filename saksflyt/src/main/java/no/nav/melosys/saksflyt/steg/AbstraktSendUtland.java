package no.nav.melosys.saksflyt.steg;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
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
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.kodeverk.Landkode;
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
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (skalSendesUtland(behandlingsresultat)) {
            Optional<String> landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream().findFirst().map(Landkoder::getKode);
            if (landkode.isPresent()) {
                if (eessiService.landErEessiReady(bucType.name(), landkode.get())) {
                    String mottakerInstitusjon = prosessinstans.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>() {}).get(0);
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

    private void sendUtland(BucType bucType, Prosessinstans prosessinstans, byte[] vedlegg, List<String> mottakerinstitusjoner) {


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

    class EessiMottaker {
        Landkode landkode;
        String institusjonId;
    }

    private void hentPåkobledeEssiMottakere(BucType bucType, Prosessinstans prosessinstans) throws IkkeFunnetException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Optional<String> landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream().findFirst().map(Landkoder::getKode);
    }
}
