package com.example.aiops.runbook;

import java.util.List;

public interface RunbookRetriever {

    List<RunbookSearchResult> search(RunbookSearchRequest request);
}
