package genai.luma;

import genai.luma.service.LumaAIService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class LumaAiIntegrationApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(LumaAiIntegrationApplication.class, args);

        LumaAIService lumaAIService = context.getBean(LumaAIService.class);

        // Example for generating a video
        String videoPrompt = "A professional business meeting in a modern office, featuring diverse, realistic individuals with detailed, lifelike faces, natural expressions, and perfect fashion outfits with various modern styles. Yellow, red, light-pink, white and blue colored clothes. Outdoor standed people's photo, Bright lighting, high-quality facial details, and modern aesthetics ensure a professional, realistic video output";
        String model = "ray-2";
        String resolution = "720p";
        String duration = "5s";
        String videoGenerationId = lumaAIService.generateVideo(videoPrompt, model, resolution, duration);
        System.out.println("Video Generation ID: " + videoGenerationId);

        // Example for generating an image
        String imagePrompt = "A professional business meeting in a modern office, featuring diverse, realistic individuals with detailed, lifelike faces, natural expressions, and perfect fashion outfits with various modern styles. Yellow, red, light-pink, white and blue colored clothes. Outdoor standed people's photo, Bright lighting, high-quality facial details, and modern aesthetics ensure a professional, photo realistic output";
        String imageGenerationId = lumaAIService.generateImage(imagePrompt);
        System.out.println("Image Generation ID: " + imageGenerationId);

        System.out.println("Requests have been submitted. The scheduler will handle processing.");
    }
}