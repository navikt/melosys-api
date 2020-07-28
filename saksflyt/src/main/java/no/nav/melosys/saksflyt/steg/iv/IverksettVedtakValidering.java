package no.nav.melosys.saksflyt.steg.iv;

import java.util.EnumSet;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_VALIDERING;

/**
 * Validerer opplysning bli brukt for iverksett vedtak.
 */
@Component
public class IverksettVedtakValidering implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakValidering.class);
    private static final EnumSet<ProsessType> AKSEPTERTE_PROSESSTYPER = EnumSet.of(
        ProsessType.IVERKSETT_VEDTAK,
        ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public IverksettVedtakValidering(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
        log.info("IverksetteVedtakValidering initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_VALIDERING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType prosessType = prosessinstans.getType();
        if (!AKSEPTERTE_PROSESSTYPER.contains(prosessType)) {
            throw new TekniskException("ProsessType " + prosessType + " er ikke støttet.");
        }

        Behandling behandling = prosessinstans.getBehandling();
        if (behandling == null) {
            throw new FunksjonellException("Prosessinstans " + prosessinstans.getId() + " er ikke knyttet til en behandling.");
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        validerBehandlingsresultat(behandlingsresultat);

        String saksbehandlerID = prosessinstans.getData(SAKSBEHANDLER);
        if (StringUtils.isEmpty(saksbehandlerID) && !behandlingsresultat.erAutomatisert()) {
            throw new FunksjonellException("SaksbehandlerID er ikke oppgitt.");
        }

        if (prosessinstans.getType() != ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE) {
            validerLovalgsperioder(behandlingsresultat);
        }

        prosessinstans.setSteg(IV_AVKLAR_MYNDIGHET);
    }

    private void validerBehandlingsresultat(Behandlingsresultat behandlingsresultat)
        throws FunksjonellException {
        if (behandlingsresultat.getType() == Behandlingsresultattyper.IKKE_FASTSATT) {
            throw new FunksjonellException("BehandlingsResultatType er ikke oppgitt.");
        }
    }

    private void validerLovalgsperioder(Behandlingsresultat behandlingsresultat) throws FunksjonellException {
        if (behandlingsresultat.getType() != Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();
            if (lovvalgsperiode.harUgyldigTilstand()) {
                throw new FunksjonellException("Lovvalgsperioden har en ugyldig kombinasjon av lovvalgsresultat og lovvalgsland");
            }
        }
    }
}