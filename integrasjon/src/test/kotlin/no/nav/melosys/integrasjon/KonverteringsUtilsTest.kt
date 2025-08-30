package no.nav.melosys.integrasjon

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.xml.datatype.XMLGregorianCalendar

class KonverteringsUtilsTest {

    @Test
    fun localDateToXMLGregorianCalendar() {
        val date = LocalDate.now()


        val xmlDate: XMLGregorianCalendar = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(date.atStartOfDay())


        xmlDate.shouldNotBeNull().run {
            year shouldBe date.year
            month shouldBe date.monthValue
            day shouldBe date.dayOfMonth
        }
    }
}
