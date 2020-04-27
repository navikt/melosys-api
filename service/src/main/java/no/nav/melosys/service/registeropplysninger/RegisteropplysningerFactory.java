package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.TekniskException;
import org.springframework.stereotype.Service;

@Service
public class RegisteropplysningerFactory {

    public RegisteropplysningerRequest.SaksopplysningTyper utledSaksopplysningTyper(Behandlingstema behandlingstema) throws TekniskException {
        switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER:
            case UTSENDT_SELVSTENDIG:
            case ARBEID_FLERE_LAND:
            case IKKE_YRKESAKTIV:
            case ARBEID_ETT_LAND_ØVRIG:
            case ARBEID_NORGE_BOSATT_ANNET_LAND:
                return hentSaksopplysningTyperForBehandlingAvSøknad();
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING:
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE:
                return hentSaksopplysningTyperForRegistreringAvUnntak();
            case ANMODNING_OM_UNNTAK_HOVEDREGEL:
                return hentSaksopplysningTyperForAnmodningOmUnntak();
            case BESLUTNING_LOVVALG_NORGE:
            case BESLUTNING_LOVVALG_ANNET_LAND:
                return hentSaksopplysningTyperForBeslutningOmLovvalg();
            case ØVRIGE_SED:
            case TRYGDETID:
                return hentSaksopplysningTyperForBehandlingAvØvrigeSedOgTrygdetid();
            default:
                throw new TekniskException("Kan ikke utlede relevante saksopplysninger fra behandlingstema " + behandlingstema);
        }
    }

    private RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForBehandlingAvSøknad() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .personopplysninger()
            .personhistorikkopplysninger()
            .arbeidsforholdopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .organisasjonsopplysninger()
            .sakOgBehandlingopplysninger()
            .build();
    }

    public RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForRegistreringAvUnntak() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .personopplysninger()
            .personhistorikkopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .utbetalingsopplysninger()
            .build();
    }

    public RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForAnmodningOmUnntak() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .personopplysninger()
            .personhistorikkopplysninger()
            .arbeidsforholdopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .organisasjonsopplysninger()
            .utbetalingsopplysninger()
            .build();
    }

    public RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForBeslutningOmLovvalg() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .personopplysninger()
            .personhistorikkopplysninger()
            .arbeidsforholdopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .organisasjonsopplysninger()
            .sakOgBehandlingopplysninger()
            .utbetalingsopplysninger()
            .build();
    }

    private RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForBehandlingAvØvrigeSedOgTrygdetid() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .personopplysninger()
            .build();
    }
}
