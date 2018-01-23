package no.nav.melosys.regler.lovvalg.utled_fakta;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SØKNADEN_KVALIFISERER_FOR_EF_883_2004;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.*;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.*;
import static no.nav.melosys.regler.motor.voc.Predikat.*;
import static no.nav.melosys.regler.motor.voc.VerdielementSett.alle;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.regler.motor.Regelpakke;
import no.nav.melosys.regler.motor.voc.Predikat;

/**
 * Regelpakken slår fast om søknaden dekkes av forordning (EF) 883/2004
 */
public class SjekkOmSoeknadenDekkesAvEf_883_2004 implements Regelpakke {
    
    /**
     * Gir varsel hvis det er evt. flyktningstatus som avgjør om bruker kvalifiserer for forordningen.
     * 
     * Varsel gis hvis ingen av følgende slår til:
     *   1a) Brukeren er statsborger av et EU/EØS-land og perioden starter etter 01.06.2012
     *   1b) Brukeren er statsborger av Sveits og perioden starter etter 01.06.2016
     *   1c) Bruker er statsløs
     *   1d) Tilfellet dekkes av nordisk konvensjon om trygd (gjelder fom. 01.05.2014)
     *   
     * I tillegg må minst ett av følgende slå til: 
     *   2a) Bruker arbeider i annet EØS land og perioden starter etter 01.06.2012
     *   2b) Bruker arbeider i annet nordisk land og perioden starter etter 01.05.2014
     *   2c) Bruker arbeider i Sveits og perioden starter etter 01.06.2016
     */
    @Regel
    public static void giVarselHvisEvtFlyktningstatusErAvgjørende() {
        hvis(
            ingenAvFølgendeErSant(
                brukerErEøsBorger.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JUNI_2012)), // 1a
                brukerErSveitsiskStatsborger.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JANUAR_2016)), // 1b
                brukerErStatsløs, // 1c
                brukerSendesTilEtNordiskLand.og(søknadsperioden().starterPåEllerEtter(FØRSTE_MAI_2014)) // 1d
            ).og(minstEttAvFølgendeErSant(
                brukerSendesTilEØSLand.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JUNI_2012)), // 2a
                brukerSendesTilEtNordiskLand.og(søknadsperioden().starterPåEllerEtter(FØRSTE_MAI_2014)), // 2b
                brukerSendesTilSveits.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JANUAR_2016)) // 2c
            ))
        ).så(
            leggTilMelding(DELVIS_STOETTET, "Tilfellet dekkes av forordning 883/2004 bare hvis brukeren har flykningstatus. Dette kan ikke sjekkes automatisk. Ring UDI.")
        );
    }
    
    
    /**
     * Setter argumentet SØKNADEN_KVALIFISERER_FOR_EF_883_2004.
     * 
     * Tilfellet i søknaden er dekket av forordningen dersom minst ett av følgende slår til:
     *   1a) Brukeren er statsborger av et EU/EØS-land og perioden starter etter 01.06.2012
     *   1b) Brukeren er statsborger av Sveits og perioden starter etter 01.06.2016
     *   1c) Bruker er statsløs
     *   1d) Bruker er flyktning (kan ikke sjekkes maskinelt)
     *   1e) Tilfellet dekkes av nordisk konvensjon om trygd (gjelder fom. 01.05.2014)
     *   
     * I tillegg må minst ett av følgende slå til: 
     *   2a) Bruker arbeider i annet EØS land og perioden starter etter 01.06.2012
     *   2b) Bruker arbeider i annet nordisk land og perioden starter etter 01.05.2014
     *   2c) Bruker arbeider i Sveits og perioden starter etter 01.06.2016
     *
     * Nordisk konvensjon om trygd: tredjelandsborger som sendes til Sverige, Finland, Danmark, Island, Grønland eller Færøyene 
     */
    @Regel
    public static void sjekkOmSoeknadenDekkesAvEf_883_2004() {
        hvis(
            minstEttAvFølgendeErSant(
                brukerErEøsBorger.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JUNI_2012)), // 1a
                brukerErSveitsiskStatsborger.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JANUAR_2016)), // 1b
                brukerErStatsløs, // 1c
                brukerSendesTilEtNordiskLand.og(søknadsperioden().starterPåEllerEtter(FØRSTE_MAI_2014)) // 1e
            ).og(minstEttAvFølgendeErSant(
                brukerSendesTilEØSLand.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JUNI_2012)), // 2a
                brukerSendesTilEtNordiskLand.og(søknadsperioden().starterPåEllerEtter(FØRSTE_MAI_2014)), // 2b
                brukerSendesTilSveits.og(søknadsperioden().starterPåEllerEtter(FØRSTE_JANUAR_2016)) // 2c
            ))
        ).så(
            settArgument(SØKNADEN_KVALIFISERER_FOR_EF_883_2004, JA)
        ).ellers(
            // FIXME: Sett argumentet til NEI og gi feilmelding hvis alle tilfellene gjenkjennes
            settArgument(SØKNADEN_KVALIFISERER_FOR_EF_883_2004, JA),
            leggTilMelding(DELVIS_STOETTET, "Kan ikke fastslå om søknaden dekkes av forordning 883/2004. Må sjekkes manuelt.")
        );
    }
    
    private static final Predikat brukerErEøsBorger = () -> personopplysningDokumentet().statsborgerskap.erEØS();

    private static final Predikat brukerErSveitsiskStatsborger = () -> personopplysningDokumentet().statsborgerskap.erSveits();

    private static final Predikat brukerErStatsløs = () -> personopplysningDokumentet().statsborgerskap.erStatsløs();

    // FIXME: Uavklart hvis bruker sendes til mer enn ett land
    private static final Predikat brukerSendesTilEtNordiskLand
            = alle(søknadDokumentet().arbeidUtland.arbeidsland).inneholderMinstEn(Land::erNorden);

    private static final Predikat brukerSendesTilSveits
            = alle(søknadDokumentet().arbeidUtland.arbeidsland).inneholderMinstEn(Land::erSveits);

    private static final Predikat brukerSendesTilEØSLand
            = alle(søknadDokumentet().arbeidUtland.arbeidsland).inneholderMinstEn(Land::erEØS);

}
