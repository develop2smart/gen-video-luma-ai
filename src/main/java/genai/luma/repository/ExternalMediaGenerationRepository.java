package genai.luma.repository;

import genai.luma.entity.ExternalMediaGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalMediaGenerationRepository extends JpaRepository<ExternalMediaGeneration, Long> {
    List<ExternalMediaGeneration> findByStatus(String status);
}