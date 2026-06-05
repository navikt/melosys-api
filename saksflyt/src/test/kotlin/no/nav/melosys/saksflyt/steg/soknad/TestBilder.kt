package no.nav.melosys.saksflyt.steg.soknad

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun lagBildeBytes(format: String, bredde: Int = 40, hoyde: Int = 30): ByteArray {
    val bilde = BufferedImage(bredde, hoyde, BufferedImage.TYPE_INT_RGB)
    val grafikk = bilde.createGraphics()
    grafikk.color = Color.BLUE
    grafikk.fillRect(0, 0, bredde, hoyde)
    grafikk.dispose()
    return ByteArrayOutputStream().use { ut ->
        ImageIO.write(bilde, format, ut)
        ut.toByteArray()
    }
}

