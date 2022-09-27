package no.nav.melosys.domain;

import java.util.EnumSet;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;

public class TemaFactory {

    private TemaFactory() {
    }

    private static final EnumSet<Behandlingstema> GYLDIGE_BEHANDLINGSTEMA_MEDLEMSKAP = EnumSet.of(
        UTSENDT_ARBEIDSTAKER,
        UTSENDT_SELVSTENDIG,
        ARBEID_ETT_LAND_ØVRIG,
        ARBEID_TJENESTEPERSON_ELLER_FLY,
        IKKE_YRKESAKTIV,
        ARBEID_FLERE_LAND,
        ARBEID_NORGE_BOSATT_ANNET_LAND,
        BESLUTNING_LOVVALG_NORGE,
        TRYGDETID,
        ØVRIGE_SED_MED,
        ARBEID_I_UTLANDET,
        ARBEID_KUN_NORGE,
        YRKESAKTIV,
        PENSJONIST,
        FORESPØRSEL_TRYGDEMYNDIGHET,
        UNNTAK_MEDLEMSKAP,
        VIRKSOMHET
    );

    private static final EnumSet<Behandlingstema> GYLDIGE_BEHANDLINGSTEMA_UNNTAK_FRA_MEDLEMSKAP = EnumSet.of(
        REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
        REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
        ANMODNING_OM_UNNTAK_HOVEDREGEL,
        BESLUTNING_LOVVALG_ANNET_LAND,
        ØVRIGE_SED_UFM,
        REGISTRERING_UNNTAK
    );

    public static Tema fraBehandlingstema(Behandlingstema behandlingstema) {
        if (GYLDIGE_BEHANDLINGSTEMA_MEDLEMSKAP.contains(behandlingstema)) {
            return Tema.MED;
        } else if (GYLDIGE_BEHANDLINGSTEMA_UNNTAK_FRA_MEDLEMSKAP.contains(behandlingstema)) {
            return Tema.UFM;
        } else {
            throw new FunksjonellException("Behandlingstema " + behandlingstema.getBeskrivelse() + " er ikke støttet.");
        }
    }
}
