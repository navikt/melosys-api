package no.nav.melosys.integrasjon.test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.tjeneste.virksomhet.person.v2.informasjon.Person;

public class TpsTestData {

    private static final Logger LOG = LoggerFactory.getLogger(TpsTestData.class);

    public static LocalDate sistOppdatert = null;

    public static final long STD_MANN_AKTØR_ID = FJERNET19L;
    public static final String STD_MANN_FNR = "FJERNET";
    public static final String STD_MANN_FORNAVN = "AL-HAMIDI";
    public static final String STD_MANN_ETTERNAVN = "KHADIM MUJULY H";
    private static TpsTestData instance;
    // Simulering av Tps sin datamodell
    private static Map<Long, String> FNR_VED_AKTØR_ID = new HashMap<>();
    private static Map<String, Long> AKTØR_ID_VED_FNR = new HashMap<>();
    private static Map<String, Person> PERSON_VED_FNR = new HashMap<>();

    // Konstanter for standardbrukere (kan refereres eksternt)
    public static final long STD_KVINNE_AKTØR_ID = FJERNET67L;
    public static final String STD_KVINNE_FNR = "88888888888";
    public static final String STD_KVINNE_FORNAVN = "FORNAVN L NOE";
    public static final String STD_KVINNE_ETTERNAVN = "NAVNESEN";
    public static final int STD_KVINNE_FØDT_DAG = 06;
    public static final int STD_KVINNE_FØDT_DAG_MND = 01;
    public static final int STD_KVINNE_FØDT_DAG_ÅR = 1969;

    public static final String STD_BARN_FNR = "FJERNET";
    public static final long STD_BARN_AKTØR_ID = 666L;
    public static final String STD_BARN_FORNAVN = "EMIL";
    public static final String STD_BARN_ETTERNAVN = "MALVIK";

    // Svarteliste
    public static final String FNR_TRIGGER_SERVICE_UNAVAILABLE = "1111111111l";
    public static final long AKTOER_ID_SERVICE_UNAVAILABLE = 2L;

}
