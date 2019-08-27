package no.nav.melosys.saksflyt.steg.sob;

import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingService;

import static no.nav.melosys.domain.Tema.MED;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

public abstract class SakOgBehandlingStegBehander extends AbstraktStegBehandler {

    private final SakOgBehandlingFasade sakOgBehandlingFasade;
    private final TpsService tpsService;
    private final BehandlingService behandlingService;

    protected SakOgBehandlingStegBehander(SakOgBehandlingFasade sakOgBehandlingFasade, TpsService tpsService, BehandlingService behandlingService) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        this.tpsService = tpsService;
        this.behandlingService = behandlingService;
    }

    private static BehandlingStatusMapper lagBehandlingStatusMapper(String saksnummer, Long behandlingID, String aktørID) {
        // BehandlingsId i SOB skal være unik i NAV, så vi prefikser med applikasjonsID.
        String behandlingsId = String.format("%s-%d", Fagsystem.MELOSYS.getKode(), behandlingID);

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(Fagsystem.MELOSYS.getKode());
        builder.medHendelsestidspunkt(LocalDateTime.now());
        builder.medArkivtema(MED.getKode());
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        return builder.build();
    }

    protected void sakOgBehandlingOpprettet(String saksnummer, Long behandlingId, String aktørID) throws IntegrasjonException {
        sakOgBehandlingFasade.sendBehandlingOpprettet(lagBehandlingStatusMapper(saksnummer, behandlingId, aktørID));
    }

    protected void sakOgBehandlingAvsluttet(String saksnummer, Long behandlingId, String aktørID) throws TekniskException, IkkeFunnetException {
        if (aktørID == null) {
            aktørID = hentAktørIdFraTps(behandlingId);
        }

        sakOgBehandlingFasade.sendBehandlingAvsluttet(lagBehandlingStatusMapper(saksnummer, behandlingId, aktørID));
    }

    private String hentAktørIdFraTps(long behandlingId) throws TekniskException, IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        PersonDokument personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        return tpsService.hentAktørIdForIdent(personDokument.fnr);
    }
}
