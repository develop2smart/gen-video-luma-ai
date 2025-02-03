package genai.luma.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class LumaAIService {

    @Value("${luma.ai.api-key}")
    private String apiKey;

    @Value("${luma.ai.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public LumaAIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] generateVideo(String prompt, String model, String resolution, String duration) {
        try {

            // Step 1: Request to generate a video and get the generation ID
            String generationId = requestGeneration(prompt, model, resolution, duration, "video");
            System.out.println("Generation ID: " + generationId);
        
            // Step 2: Poll the API to check the status
            Boolean completed = false;
            String videoUrl = null;
        
            while (!completed) {
                // Check the status of the generation
                Map<String, Object> statusResponse = checkGenerationStatus(generationId, "video");
                String status = (String) statusResponse.get("status");
                System.out.println("Status: " + status);
        
                if ("completed".equals(status)) {
                    completed = true;
                    videoUrl = (String) statusResponse.get("url");
                    System.out.println("Generated URL: " + videoUrl);
                } else if ("failed".equals(status)) {
                    throw new RuntimeException("Video generation failed.");
                } else {
                    // Wait for 3 seconds before polling again
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted while waiting for video generation.", e);
                    }
                }
            }

            // Step 3: Download the video as a byte array
            byte[] videoContent = download(videoUrl);
        
            String fileName = generateUniqueFileName("video");
    
            // Step 4: Save the video locally
            saveLocally(videoContent, fileName);
        
            // Step 5: Return the byte array
            return videoContent;
        } catch (Exception e) {
            throw new RuntimeException("Error while calling Luma AI API: " + e.getMessage(), e);
        }
    }

    public byte[] generateImage(String prompt) {
        try{
            // Step 1: Request to generate an image and get the generation ID
            String generationId = requestGeneration(prompt, "", "", "", "image");
            System.out.println("Generation ID: " + generationId);

            // Step 2: Poll the API to check the status
            Boolean completed = false;
            String imageUrl = null;
        
            while (!completed) {
                // Check the status of the generation
                Map<String, Object> statusResponse = checkGenerationStatus(generationId, "image");
                String status = (String) statusResponse.get("status");
                System.out.println("Status: " + status);
        
                if ("completed".equals(status)) {
                    completed = true;
                    imageUrl = (String) statusResponse.get("url");
                    System.out.println("Generated URL: " + imageUrl);
                } else if ("failed".equals(status)) {
                    throw new RuntimeException("Image generation failed.");
                } else {
                    // Wait for 3 seconds before polling again
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted while waiting for image generation.", e);
                    }
                }
            }
        
            // Step 3: Download the image as a byte array
            byte[] imageContent = download(imageUrl);
            String fileName = generateUniqueFileName("image");
        
            // Step 4: Save the image locally
            saveLocally(imageContent, fileName);
        
            // Step 5: Return the byte array
            return imageContent;
        } catch (Exception e) {
            throw new RuntimeException("Error while calling Luma AI API: " + e.getMessage(), e);
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
    
        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
    
        // Create the HTTP request
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
    
        // Send POST request to the appropriate endpoint based on type
        String url = baseUrl + (type.equals("video") ? "" : "/image"); // Use /image endpoint for images
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
    
        // Extract and return the generation ID
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

        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        // Create the HTTP request
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Send GET request to check the status
        String url = baseUrl + "/" + generationId;

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        // Parse the response
        Map<String, Object> responseBody = response.getBody();

        if (responseBody != null) {
            String state = (String) responseBody.get("state");
            Map<String, Object> result = new HashMap<>();
            result.put("status", state);

            if ("completed".equals(state)) {
                // Extract the URL of the generated content
                Map<String, Object> assets = (Map<String, Object>) responseBody.get("assets");
                String contentUrl = type.equalsIgnoreCase("video") ? (String) assets.get("video") : (String) assets.get("image");
                result.put("url", contentUrl);
            } else {
                result.put("url", null);
            }
            return result;
        } else {
            throw new RuntimeException("Failed to check generation status. Response is null.");
        }
    }

    private byte[] download(String videoUrl) {
        // Make a GET request to download the video
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
        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        // Format the current date and time
        String dateTime = now.format(dateFormatter);

        // Generate an 8-character random number string
        Random random = new Random();
        String randomString = String.format("%08d", random.nextInt(100000000));

        // Combine the parts to create the file name
        return "generated_" + type + "_" + dateTime + "_" + randomString + (type.equals("video") ? ".mp4" : ".jpg");
    }
}