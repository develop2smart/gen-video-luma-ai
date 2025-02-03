package genai.luma;

import genai.luma.service.LumaAIService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import java.util.Base64;

@SpringBootApplication
public class LumaAiIntegrationApplication {

    public static void main(String[] args) {
        // Start the Spring Boot application and get the application context
        ApplicationContext context = SpringApplication.run(LumaAiIntegrationApplication.class, args);

        // Get the LumaAIService bean from the context
        LumaAIService lumaAIService = context.getBean(LumaAIService.class);

        try {
            // Generate an image
            String imagePrompt = "A professional business meeting in a modern office, featuring diverse, realistic individuals with detailed, lifelike faces, natural expressions, and perfect fashion outfits. Bright lighting, high-quality facial details, and modern aesthetics ensure a professional, realistic video output";
            byte[] image = lumaAIService.generateImage(imagePrompt);
            String imageBase64 = Base64.getEncoder().encodeToString(image);
            System.out.println("Generated Image as Base64: " + imageBase64);
            System.out.println("Image generation successful.");
            
            // Generate a video
            String videoPrompt = "A professional business meeting in a modern office, featuring diverse, realistic individuals with detailed, lifelike faces, natural expressions, and perfect fashion outfits. Bright lighting, high-quality facial details, and modern aesthetics ensure a professional, photo realistic output";
            String model = "ray-2"; 
            String resolution = "720p"; 
            String duration = "5s"; 
            byte[] video = lumaAIService.generateVideo(videoPrompt, model, resolution, duration);
            String videoBase64 = Base64.getEncoder().encodeToString(video);
            System.out.println("Generated Video as Base64: " + videoBase64);
            System.out.println("Video generation successful.");
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}