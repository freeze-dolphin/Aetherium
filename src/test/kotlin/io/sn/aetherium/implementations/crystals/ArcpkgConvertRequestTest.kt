package io.sn.aetherium.implementations.crystals

import kotlin.test.Test

class ArcpkgConvertRequestTest {

    @Test
    fun `test arcpkg convertor`() {
        val target = "./result/ArcCreate.MCR5.arcpkg"

        ArcpkgConvertRequest(
            arrayOf(target), "ArcCreateMCR5.", ExportConfiguration().copy(
                exportBgMode = ExportBgMode.AUTO_RENAME
            )
        ).exec()
    }

}