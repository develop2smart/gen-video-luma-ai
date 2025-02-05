package genai.luma.service;

import genai.luma.entity.ExternalMediaGeneration;
import genai.luma.repository.ExternalMediaGenerationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class LumaAIService {

    @Value("${luma.ai.api-key}")
    private String apiKey;

    @Value("${luma.ai.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ExternalMediaGenerationRepository repository;

    public LumaAIService(RestTemplate restTemplate, ExternalMediaGenerationRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    public String generateVideo(String prompt, String model, String resolution, String duration) {
        String generationId = requestGeneration(prompt, model, resolution, duration, "video");

        ExternalMediaGeneration media = new ExternalMediaGeneration();
        media.setGenerationId(generationId);
        media.setType("video");
        media.setStatus("N"); 
        repository.save(media);

        return generationId;
    }

    public String generateImage(String prompt) {
        String generationId = requestGeneration(prompt, "", "", "", "image");

        // Persist the request details into the database
        ExternalMediaGeneration media = new ExternalMediaGeneration();
        media.setGenerationId(generationId);
        media.setType("image");
        media.setStatus("N"); 
        repository.save(media);

        return generationId;
    }

    @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.SECONDS) 
    public void processPendingGenerations() {
        System.out.println("Processing");

        List<ExternalMediaGeneration> pendingGenerations = repository.findByStatus("N");
        System.out.println("Pending Generations: " + pendingGenerations.size());

        for (ExternalMediaGeneration media : pendingGenerations) {
            try {
                System.out.println("Checking: " + media.toString());
                // Check the status of the generation
                Map<String, Object> statusResponse = checkGenerationStatus(media.getGenerationId(), media.getType());
                String status = (String) statusResponse.get("status");
                System.out.println("status: " + status);

                if ("completed".equals(status)) {
                    String contentUrl = (String) statusResponse.get("url");
                    System.out.println("Completed for " + media.getGenerationId() + " , URL: " + contentUrl);
                    byte[] content = download(contentUrl);

                    media.setContent(content);
                    media.setStatus("Y"); 
                    String fileName = generateUniqueFileName(media.getType());
                    saveLocally(content, fileName);
                    repository.save(media);
                }
            } catch (Exception e) {
                System.err.println("Failed to process generation ID " + media.getGenerationId() + ": " + e.getMessage());
            }
        }
    }

    public String requestGeneration(String prompt, String model, String resolution, String duration, String type) {
        // Validate the type (video or image)
        if (!type.equalsIgnoreCase("video") && !type.equalsIgnoreCase("image")) {
            throw new IllegalArgumentException("Invalid type. Must be 'video' or 'image'.");
        }
    
        // Define the request payload
        Map<String, String> payload = new HashMap<>();
        
        if (prompt != null && prompt.length() > 0) {
            payload.put("prompt", prompt);
        } else {
            throw new RuntimeException("Prompt required");
        }

        System.out.println("Prompt: " + prompt);
        if (type.equals("video")) {
            System.out.println("Model: " + model);
            System.out.println("Resolution: " + resolution);
            System.out.println("Duration: " + duration);

            if (model != null && model.length() > 0) {
                payload.put("model", model);
            }
            if (resolution != null && resolution.length() > 0) {
                payload.put("resolution", resolution);
            }
            if (duration != null && duration.length() > 0) {
                int durationValue = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
                System.out.println("durationValue: " + durationValue);
                if (durationValue > 5) {
                    throw new RuntimeException("Video is available less than 5s");
                } else {
                    payload.put("duration", duration);
                }
            }
        }
    
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
    
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
        String url = baseUrl + (type.equals("video") ? "" : "/image"); 
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
    
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("id")) {
            String generationId = (String) responseBody.get("id");
            return generationId;
        } else {
            System.out.println("Failed to initiate generation. Response: " + responseBody);
            throw new RuntimeException("Failed to initiate generation. Response: " + responseBody);
        }
    }

    public Map<String, Object> checkGenerationStatus(String generationId, String type) {
        // Validate the type (video or image)
        if (!type.equalsIgnoreCase("video") && !type.equalsIgnoreCase("image")) {
            throw new IllegalArgumentException("Invalid type. Must be 'video' or 'image'.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = baseUrl + "/" + generationId;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null) {
            String state = (String) responseBody.get("state");
            Map<String, Object> result = new HashMap<>();
            result.put("status", state);

            if ("completed".equals(state)) {
                Map<String, Object> assets = (Map<String, Object>) responseBody.get("assets");
                String contentUrl = type.equalsIgnoreCase("video") ? (String) assets.get("video") : (String) assets.get("image");
                result.put("url", contentUrl);
            } else {
                if ("failed".equals(state)) {
                    System.out.println("** Reason: "+ responseBody.get("failure_reason"));
                }
                result.put("url", null);
            }
            return result;
        } else {
            throw new RuntimeException("Failed to check generation status. Response is null.");
        }
    }

    public byte[] download(String videoUrl) {
        ResponseEntity<byte[]> response = restTemplate.exchange(videoUrl, HttpMethod.GET, null, byte[].class);
        return response.getBody();
    }

    private void saveLocally(byte[] videoContent, String fileName) throws IOException {
        File file = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(videoContent);
        }
    }

    private String generateUniqueFileName(String type) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        String dateTime = now.format(dateFormatter);
        Random random = new Random();
        String randomString = String.format("%08d", random.nextInt(100000000));
        return "generated_" + type + "_" + dateTime + "_" + randomString + (type.equals("video") ? ".mp4" : ".jpg");
    }
}