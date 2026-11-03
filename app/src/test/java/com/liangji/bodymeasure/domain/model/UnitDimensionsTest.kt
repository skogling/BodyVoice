package com.liangji.bodymeasure.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitDimensionsTest {

    @Test
    fun infers_known_units() {
        assertEquals(UnitDimension.MASS, UnitDimensions.infer("kg"))
        assertEquals(UnitDimension.MASS, UnitDimensions.infer(" 斤 "))
        assertEquals(UnitDimension.LENGTH, UnitDimensions.infer("CM"))
        assertEquals(UnitDimension.LENGTH, UnitDimensions.infer("cm"))
        assertEquals(UnitDimension.RATIO, UnitDimensions.infer("%"))
        assertEquals(UnitDimension.CUSTOM, UnitDimensions.infer("次"))
        assertEquals(UnitDimension.CUSTOM, UnitDimensions.infer(""))
    }
}
