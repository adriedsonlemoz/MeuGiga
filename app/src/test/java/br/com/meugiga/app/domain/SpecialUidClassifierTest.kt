package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.AppKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpecialUidClassifierTest {
    @Test
    fun `classifica uids especiais do NetworkStats`() {
        assertEquals(AppKind.AGGREGATE, SpecialUidClassifier.classify(-1)?.kind)
        assertEquals(AppKind.REMOVED, SpecialUidClassifier.classify(-4)?.kind)
        assertEquals("Aplicativos removidos", SpecialUidClassifier.classify(-4)?.label)
        assertEquals(AppKind.TETHERING, SpecialUidClassifier.classify(-5)?.kind)
        assertEquals(AppKind.SYSTEM, SpecialUidClassifier.classify(1000)?.kind)
    }

    @Test
    fun `uid de aplicativo fica para o PackageManager`() {
        assertNull(SpecialUidClassifier.classify(10123))
    }
}
