package com.ai.manager.system.service;

import com.ai.manager.common.time.DisplayTime;
import com.ai.manager.system.domain.vo.DeployVersionVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DeployHistoryService {

    private static final int DEFAULT_LIMIT = 50;

    private final ObjectMapper objectMapper;
    private final Path historyFile;
    private final int maxRecords;

    public DeployHistoryService(
            ObjectMapper objectMapper,
            @Value("${ai-manager.deploy.backend-dir:/opt/ai-manager/backend}") String backendDir,
            @Value("${ai-manager.deploy.history-max-records:100}") int maxRecords) {
        this.objectMapper = objectMapper;
        this.historyFile = Path.of(backendDir).resolve("deploy-history.json");
        this.maxRecords = Math.max(10, maxRecords);
    }

    public List<DeployVersionVO> list(int limit) {
        int size = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, maxRecords);
        List<DeployVersionVO> records = loadAll();
        records.sort(Comparator.comparingLong(DeployVersionVO::getFinishedAt).reversed());
        if (records.size() <= size) {
            return records;
        }
        return new ArrayList<>(records.subList(0, size));
    }

    public void record(
            String target,
            boolean success,
            int exitCode,
            String deployMode,
            long startedAtEpochMs,
            long finishedAtEpochMs,
            Path projectRoot) {
        DeployVersionVO record = new DeployVersionVO();
        record.setId(UUID.randomUUID().toString());
        record.setTarget(target);
        record.setSuccess(success);
        record.setExitCode(exitCode);
        record.setDeployMode(deployMode);
        record.setStartedAt(startedAtEpochMs);
        record.setFinishedAt(finishedAtEpochMs);
        record.setDurationMs(Math.max(0, finishedAtEpochMs - startedAtEpochMs));
        record.setDeployedAt(DisplayTime.formatMinute(Instant.ofEpochMilli(finishedAtEpochMs)));
        if (projectRoot != null) {
            record.setProjectRoot(projectRoot.toString());
            fillGitInfo(record, projectRoot);
        }
        append(record);
    }

    private void fillGitInfo(DeployVersionVO record, Path projectRoot) {
        if (!Files.isDirectory(projectRoot.resolve(".git"))) {
            return;
        }
        try {
            String format = runGit(projectRoot, "log", "-1", "--pretty=format:%h|%s|%D|%an");
            if (format == null || format.isBlank()) {
                return;
            }
            String[] parts = format.split("\\|", 4);
            record.setGitCommit(parts.length > 0 ? parts[0].trim() : "");
            record.setGitMessage(parts.length > 1 ? parts[1].trim() : "");
            record.setGitAuthor(parts.length > 3 ? parts[3].trim() : "");
            String refs = parts.length > 2 ? parts[2].trim() : "";
            record.setGitBranch(parseBranch(refs, projectRoot));
        } catch (Exception ex) {
            log.debug("Read git info failed: {}", ex.getMessage());
        }
    }

    private String parseBranch(String refs, Path projectRoot) throws Exception {
        if (refs != null && refs.contains("HEAD -> ")) {
            int index = refs.indexOf("HEAD -> ");
            String branchPart = refs.substring(index + 8).trim();
            int comma = branchPart.indexOf(',');
            return comma >= 0 ? branchPart.substring(0, comma).trim() : branchPart;
        }
        String branch = runGit(projectRoot, "rev-parse", "--abbrev-ref", "HEAD");
        return branch == null ? "" : branch.trim();
    }

    private String runGit(Path projectRoot, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(projectRoot.toString());
        Collections.addAll(command, args);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return "";
        }
        return process.exitValue() == 0 ? output : "";
    }

    private synchronized void append(DeployVersionVO record) {
        try {
            List<DeployVersionVO> records = loadAll();
            records.add(record);
            records.sort(Comparator.comparingLong(DeployVersionVO::getFinishedAt));
            if (records.size() > maxRecords) {
                records = new ArrayList<>(records.subList(records.size() - maxRecords, records.size()));
            }
            Files.createDirectories(historyFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(historyFile.toFile(), records);
        } catch (Exception ex) {
            log.warn("Failed to persist deploy history: {}", ex.getMessage());
        }
    }

    private synchronized List<DeployVersionVO> loadAll() {
        try {
            if (!Files.isRegularFile(historyFile)) {
                return new ArrayList<>();
            }
            List<DeployVersionVO> records =
                    objectMapper.readValue(historyFile.toFile(), new TypeReference<List<DeployVersionVO>>() {});
            return records == null ? new ArrayList<>() : new ArrayList<>(records);
        } catch (Exception ex) {
            log.warn("Failed to read deploy history: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }
}
