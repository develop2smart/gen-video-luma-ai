package genai.luma.controller;

import genai.luma.service.LumaAIService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/luma")
public class LumaAIController {

    private final LumaAIService lumaAIService;

    public LumaAIController(LumaAIService lumaAIService) {
        this.lumaAIService = lumaAIService;
    }

    @PostMapping("/generate-video")
    public ResponseEntity<byte[]> generateVideo(
        @RequestParam(required = true) String prompt,
        @RequestParam(required = false, defaultValue = "") String model,
        @RequestParam(required = false, defaultValue = "") String resolution,
        @RequestParam(required = false, defaultValue = "") String duration) {
            try {
                byte[] videoContent = lumaAIService.generateVideo(prompt, model, resolution, duration);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Disposition", "attachment; filename=generated_video.mp4");
                return new ResponseEntity<>(videoContent, headers, HttpStatus.OK);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
    }

    @PostMapping("/generate-image")
    public ResponseEntity<byte[]> generateImage(@RequestParam(required = false, defaultValue = "a futuristic cityscape") String prompt) {
        try {
            byte[] imageContent = lumaAIService.generateImage(prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Disposition", "attachment; filename=generated_image.mp4");
            return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
