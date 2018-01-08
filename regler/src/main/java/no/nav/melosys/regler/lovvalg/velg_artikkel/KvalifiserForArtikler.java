package no.nav.melosys.regler.lovvalg.velg_artikkel;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.*;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.IKKE_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMeldingOgAvbryt;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.JA;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.NEI;
import static no.nav.melosys.regler.motor.voc.Verdielement.argumentet;

import no.nav.melosys.regler.motor.Regelpakke;

public class KvalifiserForArtikler implements Regelpakke {
    
    @Regel
    public static void kvalifiserFor11_2() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_2, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3a() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_3A, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_3A, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3b() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_3B, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_3B, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3c() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_3C, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_3C, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3d() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_3D, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_3D, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3e() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_3E, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_3E, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_4_1() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_4_1, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_4_1, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_4_2() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_4_2, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_4_2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_5() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_11_5, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_11_5, NEI)
        );
    }

    /**
     * Kriteriene for at 12.1 skal vurderes:
     * 1) Brukeren (tilfellet) er omfattet av forordning (EF) 883/2004
     * 3) Bruker utfører lønnet arbeid (inkl.frilansere)
     * 4) Bruker er ikke næringsdrivende
     * 5) Arbeidsgiver må ha vesentlig aktivitet i Norge
     * 6) Bruker arbeider i kun 1 land, ikke Norge
     * 
     * FIXME: Revider kommentar og implementasjon.
     */
    @Regel
    public static void kvalifiserFor12_1() {
        hvis(
            // Vi forusetter at 1 alltid er oppfylt her (skal ha feilmelding hvis ikke)
            argumentet(BRUKER_ER_ARBEIDSTAKER).erSann() // 2
            .og(argumentet(BRUKER_ER_NÆRINGSDRIVENDE).erIkkeSann()) // 3
            .og(argumentet(ARBEIDSGIVER_HAR_VESENTLIG_VIRKSOMHET_I_NORGE).erSann()) // 4
            .og(argumentet(ANTALL_UTLAND_BRUKER_ARBEIDER_I).erLik(1)) // 5
            .og(argumentet(BRUKER_ARBEIDER_I_NORGE).erIkkeSann()) // 6
        ).så(
            settArgument(SKAL_VURDERE_ART_12_1, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_12_1, NEI)
        );
        // FIXME: Fjern midlertidig kode under
        hvis(
            argumentet(SKAL_VURDERE_ART_12_1).erIkkeSann()
        ).så(
            leggTilMeldingOgAvbryt(IKKE_STOETTET, "Denne versjonen av Melosys støtter kun artikkel 12.1. Søknaden kvalifiserer ikke for denne artikkelen.")
        );
    }

    @Regel
    public static void kvalifiserFor12_2() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_12_2, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_12_2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1A() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_1A, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_1A, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B1() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_1B1, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_1B1, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B2() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_1B2, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_1B2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B3() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_1B3, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_1B3, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B4() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_1B4, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_1B4, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_2A() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_2A, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_2A, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_2B() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_2B, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_2B, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_3() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_3, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_3, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_4() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_13_4, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_13_4, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor16_M() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_16_M, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_16_M, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor16_U() {
        hvis(
            () -> false // FIXME
        ).så(
            settArgument(SKAL_VURDERE_ART_16_U, JA)
        ).ellers(
            settArgument(SKAL_VURDERE_ART_16_U, NEI)
        );
    }

}
