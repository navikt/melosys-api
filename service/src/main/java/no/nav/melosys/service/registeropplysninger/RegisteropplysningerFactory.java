package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.TekniskException;


// Setter saksopplysningtyper per behandlingstema,
// iht. https://confluence.adeo.no/display/TEESSI/Saksopplysninger+per+behandlingstema
public final class RegisteropplysningerFactory {

    private RegisteropplysningerFactory() {
    }

    public static RegisteropplysningerRequest.SaksopplysningTyper utledSaksopplysningTyper(Behandlingstema behandlingstema) {
        return switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG, ARBEID_FLERE_LAND, IKKE_YRKESAKTIV, ARBEID_ETT_LAND_ØVRIG, ARBEID_TJENESTEPERSON_ELLER_FLY, ARBEID_NORGE_BOSATT_ANNET_LAND, ARBEID_I_UTLANDET, YRKESAKTIV ->
                hentSaksopplysningTyperForBehandlingAvSøknad();
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE ->
                hentSaksopplysningTyperForRegistreringAvUnntak();
            case ANMODNING_OM_UNNTAK_HOVEDREGEL -> hentSaksopplysningTyperForAnmodningOmUnntak();
            case BESLUTNING_LOVVALG_NORGE, BESLUTNING_LOVVALG_ANNET_LAND ->
                hentSaksopplysningTyperForBeslutningOmLovvalg();
            case ØVRIGE_SED_MED, ØVRIGE_SED_UFM, TRYGDETID ->
                hentSaksopplysningTyperForBehandlingAvØvrigeSedOgTrygdetid();
            default -> throw new TekniskException(
                "Kan ikke utlede relevante saksopplysninger fra behandlingstema " + behandlingstema);
        };
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
