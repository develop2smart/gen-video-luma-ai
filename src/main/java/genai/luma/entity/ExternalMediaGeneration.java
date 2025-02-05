package genai.luma.entity;

import jakarta.persistence.*;
import java.util.Arrays;

@Entity
@Table(name = "external_media_generation")
public class ExternalMediaGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generation_id", nullable = false, unique = true)
    private String generationId;

    @Column(name = "type", nullable = false)
    private String type; 

    @Column(name = "status", nullable = false)
    private String status; 

    @Lob
    @Column(name = "content")
    private byte[] content; 

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGenerationId() {
        return generationId;
    }

    public void setGenerationId(String generationId) {
        this.generationId = generationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ExternalMediaGeneration{" +
                "id=" + id +
                ", generationId='" + generationId + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", content=" + Arrays.toString(content) +
                '}';
    }
}