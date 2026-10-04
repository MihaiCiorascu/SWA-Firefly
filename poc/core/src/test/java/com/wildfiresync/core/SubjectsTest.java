package com.wildfiresync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SubjectsTest {

	@Test
	void buildsTheSubjectForAReport() {
		assertEquals("events.operation.report-1", Subjects.forReport(new ReportId("report-1")));
	}

	@Test
	void theWildcardCoversReportSubjects() {
		assertEquals("events.operation.>", Subjects.ALL);
	}

	@Test
	void rejectsReportIdsThatAreNotSubjectTokens() {
		assertThrows(IllegalArgumentException.class, () -> Subjects.forReport(new ReportId("report.1")));
		assertThrows(IllegalArgumentException.class, () -> Subjects.forReport(new ReportId("report *")));
		assertThrows(IllegalArgumentException.class, () -> Subjects.forReport(new ReportId("report>1")));
	}

}
