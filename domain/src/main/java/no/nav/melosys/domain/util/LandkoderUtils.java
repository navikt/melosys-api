package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.exception.TekniskException;

public final class LandkoderUtils {

    private LandkoderUtils() {
        throw new IllegalArgumentException("Utility");
    }

    public static String tilIso3(String l) throws TekniskException {

        Landkoder iso2Kode = Landkoder.valueOf(l);

        switch (iso2Kode) {
            case BE: return Land.BELGIA;
            case NO: return Land.NORGE;
            case BG: return Land.BULGARIA;
            case CZ: return Land.TSJEKKIA;
            case DK: return Land.DANMARK;
            case EE: return Land.ESTLAND;
            case FI: return Land.FINLAND;
            case FR: return Land.FRANKRIKE;
            case GR: return Land.HELLAS;
            case IE: return Land.IRLAND;
            case IS: return Land.ISLAND;
            case IT: return Land.ITALIA;
            case HR: return Land.KROATIA;
            case CY: return Land.KYPROS;
            case LV: return Land.LATVIA;
            case LI: return Land.LIECHTENSTEIN;
            case LT: return Land.LITAUEN;
            case LU: return Land.LUXEMBOURG;
            case MT: return Land.MALTA;
            case NL: return Land.NEDERLAND;
            case PL: return Land.POLEN;
            case PT: return Land.PORTUGAL;
            case RO: return Land.ROMANIA;
            case SK: return Land.SLOVAKIA;
            case SI: return Land.SLOVENIA;
            case ES: return Land.SPANIA;
            case GB: return Land.STORBRITANNIA;
            case CH: return Land.SVEITS;
            case SE: return Land.SVERIGE;
            case DE: return Land.TYSKLAND;
            case HU: return Land.UNGARN;
            case AT: return Land.ØSTERRIKE;
            case FO: return Land.FÆRØYENE;
            case GL: return Land.GRØNLAND;
            case AX: return Land.ÅLAND;
            case SJ: return Land.SVALBARD_OG_JAN_MAYEN;

            default: throw new TekniskException("Støtter ikke land " + iso2Kode.getKode());
        }
    }

    public static Landkoder tilIso2(String l) throws TekniskException {

        switch (l) {
            case Land.BELGIA: return Landkoder.BE;
            case Land.NORGE: return Landkoder.NO;
            case Land.BULGARIA: return Landkoder.BG;
            case Land.TSJEKKIA: return Landkoder.CZ;
            case Land.DANMARK: return Landkoder.DK;
            case Land.ESTLAND: return Landkoder.EE;
            case Land.FINLAND: return Landkoder.FI;
            case Land.FRANKRIKE: return Landkoder.FR;
            case Land.HELLAS: return Landkoder.GR;
            case Land.IRLAND: return Landkoder.IE;
            case Land.ISLAND: return Landkoder.IS;
            case Land.ITALIA: return Landkoder.IT;
            case Land.UNGARN: return Landkoder.HU;
            case Land.KYPROS: return Landkoder.CY;
            case Land.LATVIA: return Landkoder.LV;
            case Land.LIECHTENSTEIN: return Landkoder.LI;
            case Land.LITAUEN: return Landkoder.LT;
            case Land.LUXEMBOURG: return Landkoder.LU;
            case Land.MALTA: return Landkoder.MT;
            case Land.NEDERLAND: return Landkoder.NL;
            case Land.POLEN: return Landkoder.PL;
            case Land.PORTUGAL: return Landkoder.PT;
            case Land.ROMANIA: return Landkoder.RO;
            case Land.SLOVAKIA: return Landkoder.SK;
            case Land.SLOVENIA: return Landkoder.SI;
            case Land.SPANIA: return Landkoder.ES;
            case Land.STORBRITANNIA: return Landkoder.GB;
            case Land.SVEITS: return Landkoder.CH;
            case Land.SVERIGE: return Landkoder.SE;
            case Land.TYSKLAND: return Landkoder.DE;
            case Land.ØSTERRIKE: return Landkoder.AT;
            case Land.FÆRØYENE: return Landkoder.FO;
            case Land.GRØNLAND: return Landkoder.GL;
            case Land.KROATIA: return Landkoder.HR;
            case Land.ÅLAND: return Landkoder.AX;
            case Land.SVALBARD_OG_JAN_MAYEN: return Landkoder.SJ;

            default: throw new TekniskException("Støtter ikke land " + l);
        }
    }
}
