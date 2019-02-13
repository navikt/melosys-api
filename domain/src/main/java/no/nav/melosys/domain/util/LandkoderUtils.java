package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.exception.TekniskException;

public final class LandkoderUtils {
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
            default: throw new TekniskException("Støtter ikke land " + iso2Kode.getKode());
        }
    }
}
