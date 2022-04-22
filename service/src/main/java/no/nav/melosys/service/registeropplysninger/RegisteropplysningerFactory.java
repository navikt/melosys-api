package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.TekniskException;

/**
 * Setter saksopplysningtyper per behandlingstema, iht. https://confluence.adeo.no/display/TEESSI/Saksopplysninger+per+behandlingstema
 */
public final class RegisteropplysningerFactory {

    private RegisteropplysningerFactory() {}

    public static RegisteropplysningerRequest.SaksopplysningTyper utledSaksopplysningTyper(Behandlingstema behandlingstema) {
        switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER:
            case UTSENDT_SELVSTENDIG:
            case ARBEID_FLERE_LAND:
            case IKKE_YRKESAKTIV:
            case ARBEID_ETT_LAND_ØVRIG:
            case ARBEID_NORGE_BOSATT_ANNET_LAND:
            case ARBEID_I_UTLANDET:
            case YRKESAKTIV:
                return hentSaksopplysningTyperForBehandlingAvSøknad();
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING:
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE:
                return hentSaksopplysningTyperForRegistreringAvUnntak();
            case ANMODNING_OM_UNNTAK_HOVEDREGEL:
                return hentSaksopplysningTyperForAnmodningOmUnntak();
            case BESLUTNING_LOVVALG_NORGE:
            case BESLUTNING_LOVVALG_ANNET_LAND:
                return hentSaksopplysningTyperForBeslutningOmLovvalg();
            case ØVRIGE_SED_MED:
            case ØVRIGE_SED_UFM:
            case TRYGDETID:
                return hentSaksopplysningTyperForBehandlingAvØvrigeSedOgTrygdetid();
            default:
                throw new TekniskException("Kan ikke utlede relevante saksopplysninger fra behandlingstema " + behandlingstema);
        }
    }

    private static RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForBehandlingAvSøknad() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .arbeidsforholdopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .organisasjonsopplysninger()
            .sakOgBehandlingopplysninger()
            .build();
    }

    private static RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForRegistreringAvUnntak() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .utbetalingsopplysninger()
            .build();
    }

    private static RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForAnmodningOmUnntak() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .arbeidsforholdopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .organisasjonsopplysninger()
            .utbetalingsopplysninger()
            .build();
    }

    private static RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForBeslutningOmLovvalg() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .arbeidsforholdopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .organisasjonsopplysninger()
            .sakOgBehandlingopplysninger()
            .utbetalingsopplysninger()
            .build();
    }

    private static RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForBehandlingAvØvrigeSedOgTrygdetid() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .build();
    }
}
