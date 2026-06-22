package com.example.aiops.runbook;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class MarkdownRunbookRepository implements RunbookRepository {

    public static final String DEFAULT_LOCATION = "classpath*:runbooks/*.md";
    private static final Map<String, RunbookSectionType> SECTION_TYPES = Map.of(
            "检索描述", RunbookSectionType.RETRIEVAL_PROFILE,
            "典型现象", RunbookSectionType.SYMPTOM,
            "信号模式", RunbookSectionType.SIGNAL_PATTERN,
            "常见原因", RunbookSectionType.COMMON_CAUSE,
            "排查步骤", RunbookSectionType.DIAGNOSIS_STEP,
            "临时处理", RunbookSectionType.MITIGATION,
            "风险操作", RunbookSectionType.RISK,
            "人工接管条件", RunbookSectionType.HANDOFF,
            "最终建议模板", RunbookSectionType.TEMPLATE
    );

    private final Map<String, RunbookDocument> documents = new LinkedHashMap<>();
    private final List<RunbookSection> sections = new ArrayList<>();

    public MarkdownRunbookRepository() {
        this(new PathMatchingResourcePatternResolver(), DEFAULT_LOCATION);
    }

    public MarkdownRunbookRepository(ResourcePatternResolver resolver, String locationPattern) {
        load(resolver, locationPattern);
    }

    @Override
    public List<RunbookDocument> findAllDocuments() {
        return List.copyOf(documents.values());
    }

    @Override
    public List<RunbookSection> findAllSections() {
        return List.copyOf(sections);
    }

    @Override
    public Optional<RunbookDocument> findDocument(String runbookId) {
        return Optional.ofNullable(documents.get(runbookId));
    }

    @Override
    public List<RunbookSection> findSections(String runbookId,
                                             Set<RunbookSectionType> sectionTypes) {
        return sections.stream()
                .filter(section -> section.runbookId().equals(runbookId))
                .filter(section -> sectionTypes.contains(section.sectionType()))
                .toList();
    }

    private void load(ResourcePatternResolver resolver, String locationPattern) {
        try {
            Resource[] resources = resolver.getResources(locationPattern);
            if (resources.length == 0) {
                throw new IllegalStateException("No Markdown runbooks found at " + locationPattern);
            }
            for (Resource resource : resources) {
                parse(resource.getContentAsString(StandardCharsets.UTF_8), resource.getDescription());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Markdown runbooks from " + locationPattern,
                    exception);
        }
    }

    private void parse(String markdown, String source) {
        String normalized = markdown.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            throw invalid(source, "front matter must start with ---");
        }
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) {
            throw invalid(source, "front matter closing --- is missing");
        }
        Map<String, Object> metadata = parseFrontMatter(normalized.substring(4, end), source);
        String id = required(metadata, "id", source);
        String title = required(metadata, "title", source);
        String component = required(metadata, "component", source);
        List<String> alertTypes = list(metadata, "alertTypes");
        List<String> rootCauses = list(metadata, "rootCauses");
        List<String> tags = list(metadata, "tags");
        String body = normalized.substring(end + 5).trim();

        if (documents.containsKey(id)) {
            throw invalid(source, "duplicate runbook id " + id);
        }
        RunbookDocument document = new RunbookDocument(id, title, component,
                alertTypes, rootCauses, tags, body);
        documents.put(id, document);
        parseSections(document, source);
    }

    private Map<String, Object> parseFrontMatter(String frontMatter, String source) {
        Map<String, Object> values = new LinkedHashMap<>();
        String currentList = null;
        for (String rawLine : frontMatter.split("\n")) {
            String line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("- ")) {
                if (currentList == null) {
                    throw invalid(source, "list item has no property: " + line);
                }
                @SuppressWarnings("unchecked")
                List<String> items = (List<String>) values.get(currentList);
                items.add(unquote(line.substring(2).trim()));
                continue;
            }
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw invalid(source, "invalid front matter line: " + line);
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (value.isEmpty()) {
                values.put(key, new ArrayList<String>());
                currentList = key;
            } else {
                values.put(key, unquote(value));
                currentList = null;
            }
        }
        return values;
    }

    private void parseSections(RunbookDocument document, String source) {
        String currentTitle = null;
        StringBuilder content = new StringBuilder();
        for (String line : document.content().split("\n")) {
            if (line.startsWith("## ")) {
                addSection(document, currentTitle, content.toString(), source);
                currentTitle = line.substring(3).trim();
                content.setLength(0);
            } else if (currentTitle != null) {
                content.append(line).append('\n');
            }
        }
        addSection(document, currentTitle, content.toString(), source);
    }

    private void addSection(RunbookDocument document, String sectionTitle, String content,
                            String source) {
        if (sectionTitle == null) {
            return;
        }
        RunbookSectionType sectionType = SECTION_TYPES.get(sectionTitle);
        if (sectionType == null) {
            throw invalid(source, "unsupported section title: " + sectionTitle);
        }
        sections.add(new RunbookSection(document.runbookId(), document.title(),
                document.component(), document.alertTypes(), document.rootCauses(), document.tags(),
                sectionType, sectionTitle, content.trim()));
    }

    private String required(Map<String, Object> metadata, String key, String source) {
        Object value = metadata.get(key);
        if (!(value instanceof String text) || !StringUtils.hasText(text)) {
            throw invalid(source, "front matter is missing required field '" + key + "'");
        }
        return text.trim();
    }

    private List<String> list(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private IllegalStateException invalid(String source, String detail) {
        return new IllegalStateException("Invalid Markdown runbook " + source + ": " + detail);
    }
}
