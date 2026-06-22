package com.example.aiops.runbook;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class KeywordRunbookRetriever implements RunbookRetriever {

    private final RunbookRepository repository;

    public KeywordRunbookRetriever(RunbookRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RunbookSearchResult> search(RunbookSearchRequest request) {
        return repository.findAllSections().stream()
                .filter(section -> section.sectionType().participatesInRetrieval())
                .map(section -> toResult(section, score(section, request)))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(RunbookSearchResult::score).reversed()
                        .thenComparing(RunbookSearchResult::runbookId)
                        .thenComparing(result -> result.sectionType().ordinal()))
                .limit(request.topK())
                .toList();
    }

    private int score(RunbookSection section, RunbookSearchRequest request) {
        int score = 0;
        String symptom = normalize(request.symptom());
        if (equalsNormalized(section.component(), request.component())) {
            score += 5;
        }
        if (containsIgnoreCase(section.rootCauses(), request.rootCauseHypothesis())) {
            score += 4;
        }
        if (containsIgnoreCase(section.alertTypes(), request.alertType())) {
            score += 3;
        }
        for (String tag : section.tags()) {
            if (!tag.isBlank() && symptom.contains(normalize(tag))) {
                score += 2;
            }
        }
        for (String keyword : keywords(symptom)) {
            if (normalize(section.content()).contains(keyword)) {
                score++;
            }
            if (normalize(section.sectionTitle()).contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private RunbookSearchResult toResult(RunbookSection section, int score) {
        return new RunbookSearchResult(section.runbookId(), section.title(), section.sectionType(),
                section.sectionTitle(), section.content(), score);
    }

    private Set<String> keywords(String symptom) {
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(symptom.split("[^\\p{L}\\p{N}_-]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .forEach(tokens::add);
        return tokens;
    }

    private boolean equalsNormalized(String left, String right) {
        return !normalize(left).isBlank() && normalize(left).equals(normalize(right));
    }

    private boolean containsIgnoreCase(List<String> values, String expected) {
        String normalized = normalize(expected);
        return !normalized.isBlank()
                && values.stream().map(this::normalize).anyMatch(normalized::equals);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
