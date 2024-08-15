package no.nav.melosys.service.registeropplysninger;

import io.getunleash.Unleash;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.springframework.stereotype.Component;


// Setter saksopplysningtyper per behandlingstema,
// iht. https://confluence.adeo.no/display/TEESSI/Saksopplysninger+per+behandlingstema
@Component
public class RegisteropplysningerFactory {

    private final SaksbehandlingRegler saksbehandlingRegler;

    private final Unleash unleash;

    public RegisteropplysningerFactory(SaksbehandlingRegler saksbehandlingRegler, Unleash unleash) {
        this.saksbehandlingRegler = saksbehandlingRegler;
        this.unleash = unleash;
    }

    public RegisteropplysningerRequest.SaksopplysningTyper utledSaksopplysningTyper(
        Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {

        if (saksbehandlingRegler.harIngenFlyt(sakstype, sakstema, behandlingstype, behandlingstema)) {
            return ingenSaksopplysningTyper();
        }
        if (saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(sakstype, sakstema, behandlingstema)) {
            return hentSaksopplysningTyperForRegistreringUnntakFraMedlemskap();
        }

        if(behandlingstema == Behandlingstema.ARBEID_KUN_NORGE && unleash.isEnabled(ToggleName.MELOSYS_ARBEID_KUN_NORGE)) { //legg til i switch case når vi fjerner toggle
            return hentSaksopplysningTyperForBehandlingAvSøknad();
        }

        return switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER,
                UTSENDT_SELVSTENDIG,
                ARBEID_FLERE_LAND,
                ARBEID_TJENESTEPERSON_ELLER_FLY,
                ARBEID_NORGE_BOSATT_ANNET_LAND,
                IKKE_YRKESAKTIV,
                YRKESAKTIV -> hentSaksopplysningTyperForBehandlingAvSøknad();
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE ->
                hentSaksopplysningTyperForRegistreringAvUnntak();
            case ANMODNING_OM_UNNTAK_HOVEDREGEL -> hentSaksopplysningTyperForAnmodningOmUnntak();
            case BESLUTNING_LOVVALG_NORGE, BESLUTNING_LOVVALG_ANNET_LAND ->
                hentSaksopplysningTyperForBeslutningOmLovvalg();
            default -> throw new TekniskException(
                "Kan ikke utlede relevante saksopplysninger fra behandlingstema " + behandlingstema);
        };
    }

    private RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForBehandlingAvSøknad() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .arbeidsforholdopplysninger()
            .inntektsopplysninger()
            .medlemskapsopplysninger()
            .organisasjonsopplysninger()
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

    private static RegisteropplysningerRequest.SaksopplysningTyper hentSaksopplysningTyperForRegistreringUnntakFraMedlemskap() {
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
            .utbetalingsopplysninger()
            .build();
    }

    private static RegisteropplysningerRequest.SaksopplysningTyper ingenSaksopplysningTyper() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder()
            .build();
    }
}
