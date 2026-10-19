package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SubjectsTests {

    private static final String HEXAGON = "8a1fb46622dffff";

    private static final UUID REPORT = new UUID(4, 1);

    @Test
    void aReportIsPublishedUnderItsHexagonAndId() {
        assertEquals("report.8a1fb46622dffff.00000000-0000-0004-0000-000000000001",
                Subjects.forReport(HEXAGON, REPORT));
    }

    @Test
    void aHexagonFilterMatchesEveryReportInIt() {
        assertEquals("report.8a1fb46622dffff.*", Subjects.reportsIn(HEXAGON));
    }

    @Test
    void reportsDoNotFallUnderTheOperationEvents() {
        assertFalse(Subjects.forReport(HEXAGON, REPORT).startsWith(Subjects.PREFIX + "."));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "8a1f.b466", "*", ">", "8a1f b466" })
    void anInvalidHexagonIsRejected(String hexagon) {
        assertThrows(IllegalArgumentException.class, () -> Subjects.forReport(hexagon, REPORT));
        assertThrows(IllegalArgumentException.class, () -> Subjects.reportsIn(hexagon));
    }

}
