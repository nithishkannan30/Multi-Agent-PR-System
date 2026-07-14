package com.example.MultiAgentsForPR.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IndexedFileRepository extends JpaRepository<IndexedFileEntity, Long> {
    List<IndexedFileEntity> findByOwnerAndRepo(String owner, String repo);
    Optional<IndexedFileEntity> findByOwnerAndRepoAndFilePath(String owner, String repo, String filePath);
}