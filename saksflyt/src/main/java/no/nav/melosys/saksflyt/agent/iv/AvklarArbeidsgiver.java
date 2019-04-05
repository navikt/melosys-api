package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktAvklarArbeidsgiveraktoer;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_AVKLAR_ARBEIDSGIVER;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_MEDL;

/**
 * Oppdaterer aktør med avklart arbeidsgiver i saken.
 *
 * Transisjoner:
 *  IV_AVKLAR_ARBEIDSGIVER -> IV_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component("IverksettVedtakAvklarArbeidsgiver")
public class AvklarArbeidsgiver extends AbstraktAvklarArbeidsgiveraktoer {

    private static final Logger log = LoggerFactory.getLogger(AvklarArbeidsgiver.class);

    @Autowired
    public AvklarArbeidsgiver(AktoerService aktoerService,
                              AvklarteVirksomheterSystemService avklarteVirksomheterService,
                              BehandlingRepository behandlingRepository) {
        super(aktoerService, avklarteVirksomheterService, behandlingRepository);

        log.info("AvklarArbeidsgiver initialisert");
    }

    public ProsessSteg inngangsSteg() {
        return IV_AVKLAR_ARBEIDSGIVER;
    }

    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        // IVERKSETT_VEDTAK omfatter både Innvilgelse og Avslag
        if (prosessinstans.getType() == ProsessType.IVERKSETT_VEDTAK) {
            super.utfør(prosessinstans);
        }

        prosessinstans.setSteg(IV_OPPDATER_MEDL);
    }
}