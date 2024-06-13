package io.sn.aetherium.implementations.crystals

import kotlin.test.Test

class ArcpkgConvertTest {

    @Test
    fun `test arcpkg convertor`() {
        val target = "/home/freeze-dolphin/Documents/workspace/idea/Aetherium/result/ArcCreate.MCR5.arcpkg"

        ArcpkgConvert(
            arrayOf(target), "ArcCreateMCR5.", ExportConfiguration().copy(
                exportBgMode = ExportBgMode.AUTO_RENAME
            )
        ).exec()
    }

}