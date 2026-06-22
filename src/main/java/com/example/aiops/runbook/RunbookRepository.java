package com.example.aiops.runbook;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RunbookRepository {

    List<RunbookDocument> findAllDocuments();

    List<RunbookSection> findAllSections();

    Optional<RunbookDocument> findDocument(String runbookId);

    List<RunbookSection> findSections(String runbookId, Set<RunbookSectionType> sectionTypes);
}
