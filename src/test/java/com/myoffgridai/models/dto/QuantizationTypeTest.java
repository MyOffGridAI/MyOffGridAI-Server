package com.myoffgridai.models.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QuantizationType}.
 *
 * <p>Verifies enum ordering, labels, and rank assignments.</p>
 */
class QuantizationTypeTest {

    @Test
    void allEnumValues_havePositiveRank() {
        for (QuantizationType qt : QuantizationType.values()) {
            assertTrue(qt.getRank() > 0,
                    qt.name() + " should have a positive rank");
        }
    }

    @Test
    void allEnumValues_haveNonEmptyLabel() {
        for (QuantizationType qt : QuantizationType.values()) {
            assertNotNull(qt.getLabel(), qt.name() + " label should not be null");
            assertFalse(qt.getLabel().isBlank(), qt.name() + " label should not be blank");
        }
    }

    @Test
    void rankOrdering_lowestToHighest() {
        assertTrue(QuantizationType.IQ1_S.getRank() < QuantizationType.IQ2_XS.getRank());
        assertTrue(QuantizationType.IQ2_XS.getRank() < QuantizationType.Q2_K.getRank());
        assertTrue(QuantizationType.Q2_K.getRank() < QuantizationType.Q3_K_XS.getRank());
        assertTrue(QuantizationType.Q3_K_XS.getRank() < QuantizationType.Q3_K_S.getRank());
        assertTrue(QuantizationType.Q3_K_S.getRank() < QuantizationType.Q3_K_M.getRank());
        assertTrue(QuantizationType.Q3_K_M.getRank() < QuantizationType.Q4_K_S.getRank());
        assertTrue(QuantizationType.Q4_K_S.getRank() < QuantizationType.Q4_K_M.getRank());
        assertTrue(QuantizationType.Q4_K_M.getRank() < QuantizationType.Q5_K_S.getRank());
        assertTrue(QuantizationType.Q5_K_S.getRank() < QuantizationType.Q5_K_M.getRank());
        assertTrue(QuantizationType.Q5_K_M.getRank() < QuantizationType.Q6_K.getRank());
        assertTrue(QuantizationType.Q6_K.getRank() < QuantizationType.Q8_0.getRank());
        assertTrue(QuantizationType.Q8_0.getRank() < QuantizationType.F16.getRank());
        assertTrue(QuantizationType.F16.getRank() < QuantizationType.F32.getRank());
    }

    @Test
    void f16AndBf16_haveSameRank() {
        assertEquals(QuantizationType.F16.getRank(), QuantizationType.BF16.getRank());
    }

    @Test
    void q4KmRank_isEight() {
        assertEquals(8, QuantizationType.Q4_K_M.getRank());
    }

    @Test
    void q4KmLabel_indicatesMostPopular() {
        assertTrue(QuantizationType.Q4_K_M.getLabel().contains("most popular"));
    }

    @ParameterizedTest
    @EnumSource(QuantizationType.class)
    void eachEnumConstant_hasValidRankRange(QuantizationType qt) {
        assertTrue(qt.getRank() >= 1 && qt.getRank() <= 14,
                qt.name() + " rank should be between 1 and 14, was " + qt.getRank());
    }

    @Test
    void enumCount_isCorrect() {
        assertEquals(15, QuantizationType.values().length);
    }

    @Test
    void valueOf_worksForAllKnownTypes() {
        assertDoesNotThrow(() -> QuantizationType.valueOf("Q4_K_M"));
        assertDoesNotThrow(() -> QuantizationType.valueOf("BF16"));
        assertDoesNotThrow(() -> QuantizationType.valueOf("IQ1_S"));
        assertDoesNotThrow(() -> QuantizationType.valueOf("F32"));
    }
}
