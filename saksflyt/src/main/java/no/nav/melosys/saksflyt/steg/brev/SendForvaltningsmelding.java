package no.nav.melosys.saksflyt.steg.brev;

import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto;
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.*;
import static no.nav.melosys.saksflytapi.domain.ProsessSteg.SEND_FORVALTNINGSMELDING;

@Component
public class SendForvaltningsmelding implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendForvaltningsmelding.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;

    public SendForvaltningsmelding(BrevBestiller brevBestiller, BehandlingService behandlingService) {
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return SEND_FORVALTNINGSMELDING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        var forvaltningsmeldingMottaker = hentForvaltningsmeldingMottaker(prosessinstans);
        long behandlingID = Objects.requireNonNull(prosessinstans.getBehandling()).getId();

        if (skalSendeForvaltningsmelding(forvaltningsmeldingMottaker)) {
            Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
            String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);

            if (forvaltningsmeldingMottaker.equals(BRUKER)) {
                brevBestiller.bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, List.of(Mottaker.medRolle(Mottakerroller.BRUKER)), null,
                    saksbehandler, null, behandling);
            } else {
                String avsenderID = prosessinstans.getData(AVSENDER_ID);
                boolean avsenderErOrganisasjon = avsenderID.length() == 9;
                Mottaker mottaker;

                if (avsenderErOrganisasjon) {
                    mottaker = Mottaker.medRolle(Mottakerroller.ANNEN_ORGANISASJON);
                    mottaker.setOrgnr(avsenderID);
                } else {
                    mottaker = Mottaker.medRolle(Mottakerroller.ANNEN_PERSON);
                    mottaker.setPersonIdent(avsenderID);
                }
                brevBestiller.bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, List.of(mottaker), null,
                    saksbehandler, null, behandling);
            }
            log.info("Sendt forvaltningsmelding for behandling {}", behandlingID);
        } else {
            log.info("Ikke sendt forvaltningsmelding for behandling {}", behandlingID);
        }
    }

    private boolean skalSendeForvaltningsmelding(ForvaltningsmeldingMottaker forvaltningsmeldingMottaker) {
        return BRUKER.equals(forvaltningsmeldingMottaker) || AVSENDER.equals(forvaltningsmeldingMottaker);
    }

    private ForvaltningsmeldingMottaker hentForvaltningsmeldingMottaker(Prosessinstans prosessinstans) {
        var forvaltningsmeldingMottaker = prosessinstans.getData(FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.class);

        if (forvaltningsmeldingMottaker == null &&
            (prosessinstans.getType() == ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
                || prosessinstans.getType() == ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD)) {
            return hentForvaltningsmeldingMottakerForDigitalSøknad(prosessinstans);
        }
        return forvaltningsmeldingMottaker;
    }

    private ForvaltningsmeldingMottaker hentForvaltningsmeldingMottakerForDigitalSøknad(Prosessinstans prosessinstans) {
        var søknadsdata = prosessinstans.getData(ProsessDataKey.DIGITAL_SØKNADSDATA, UtsendtArbeidstakerSkjemaM2MDto.class);
        if (søknadsdata == null) {
            throw new IllegalStateException(
                "Mangler digital søknadsdata på prosessinstans %s for behandling %d. "
                    .formatted(prosessinstans.getId(), prosessinstans.getBehandling().getId())
                    + "Klarte ikke utlede mottaker for forvaltningsmelding.");
        }

        Skjemadel mottattSkjemadel = søknadsdata.getSkjema().getMetadata().getSkjemadel();

        if (mottattSkjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL || mottattSkjemadel == Skjemadel.ARBEIDSTAKERS_DEL) {
            return BRUKER;
        }
        return INGEN;
    }
}
