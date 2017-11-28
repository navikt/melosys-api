package no.nav.melosys.regler.lovvalg.velg_artikkel;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ARBEIDER_I_FLERE_LAND;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ARBEIDER_I_FLY;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ARBEIDER_PÅ_SKIP;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_ARBEIDSTAKER;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_NÆRINGSDRIVENDE;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_TJENESTEMANN;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_2;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_3A;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_3B;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_3C;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_3D;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_3E;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_4_1;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_4_2;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_11_5;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_12_1;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_12_2;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_1A;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_1B1;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_1B2;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_1B3;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_1B4;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_2A;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_2B;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_3;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_13_4;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_16_M;
import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SKAL_VURDERE_ART_16_U;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settVariabel;
import static no.nav.melosys.regler.motor.dekl.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.dekl.FellesVokabular.JA;
import static no.nav.melosys.regler.motor.dekl.FellesVokabular.NEI;
import static no.nav.melosys.regler.motor.dekl.Verdielement.variabelen;

import no.nav.melosys.regler.motor.Regel;
import no.nav.melosys.regler.motor.dekl.FellesVokabular;

public class KvalifiserForArtikler {
    
    @Regel
    public static void kvalifiserFor11_2() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_2, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3a() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_3A, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_3A, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3b() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_3B, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_3B, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3c() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_3C, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_3C, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3d() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_3D, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_3D, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_3e() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_3E, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_3E, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_4_1() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_4_1, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_4_1, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_4_2() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_4_2, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_4_2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor11_5() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_11_5, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_11_5, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor12_1() {
        hvis(
            variabelen(BRUKER_ER_ARBEIDSTAKER).erSann()
            .og(variabelen(BRUKER_ER_NÆRINGSDRIVENDE).erIkkeSann())
            .og(variabelen(BRUKER_ARBEIDER_I_FLY).erIkkeSann())
            .og(variabelen(BRUKER_ARBEIDER_PÅ_SKIP).erIkkeSann())
            .og(variabelen(BRUKER_ER_TJENESTEMANN).erIkkeSann())
            .og(variabelen(BRUKER_ARBEIDER_I_FLERE_LAND).erIkkeSann())
        ).så(
            settVariabel(SKAL_VURDERE_ART_12_1, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_12_1, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor12_2() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_12_2, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_12_2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1A() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_1A, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_1A, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B1() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_1B1, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_1B1, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B2() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_1B2, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_1B2, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B3() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_1B3, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_1B3, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_1B4() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_1B4, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_1B4, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_2A() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_2A, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_2A, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_2B() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_2B, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_2B, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_3() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_3, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_3, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor13_4() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_13_4, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_13_4, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor16_M() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_16_M, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_16_M, NEI)
        );
    }

    @Regel
    public static void kvalifiserFor16_U() {
        hvis(
            () -> false // FIXME
        ).så(
            settVariabel(SKAL_VURDERE_ART_16_U, JA)
        ).ellers(
            settVariabel(SKAL_VURDERE_ART_16_U, NEI)
        );
    }

}
