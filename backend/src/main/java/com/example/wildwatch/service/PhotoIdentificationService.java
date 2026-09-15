package com.example.wildwatch.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import com.example.wildwatch.repository.SpeciesRepository;

@Service
public class PhotoIdentificationService {

    private static final Logger log = LoggerFactory.getLogger(PhotoIdentificationService.class);

    private final ChatClient.Builder chatClientBuilder;
    private final SpeciesRepository speciesRepository;

    public PhotoIdentificationService(ChatClient.Builder chatClientBuilder, SpeciesRepository speciesRepository) {
        this.chatClientBuilder = chatClientBuilder;
        this.speciesRepository = speciesRepository;
    }

    public String identifySpecies(MultipartFile image, String region) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }

        try {
            byte[] bytes = image.getBytes();
            String contentType = image.getContentType() == null ? "image/jpeg" : image.getContentType();

            String systemPrompt = "You are a wildlife expert assistant. Identify the species in the provided image. "
                    + "Respond with the most likely scientific name, common name, and your confidence level (high/medium/low). "
                    + "If uncertain between a few options, list up to 3 possibilities ordered by likelihood. "
                    + "Be honest if the image is unclear or doesn't show identifiable wildlife.";

            // Build a chat client and send the image. Use Spring AI ChatClient; attach the
            // image as a media object
            // Note: Spring AI multimodal APIs have evolved; we attempt to construct a media
            // object if available on the classpath.
            ChatClient client = chatClientBuilder.build();

            // Attach the image directly using Spring AI Media and call the chat API.
            MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);
            Media media = new Media(mimeType, new ByteArrayResource(bytes));

            String content = client.prompt()
                    .system(systemPrompt)
                    .user(userSpec -> userSpec
                            .text("Please examine the attached image and identify the species.")
                            .media(media))
                    .call()
                    .content();

            // Try to extract a best-guess scientific name (simple regex for two-word Latin
            // binomial)
            Pattern latin = Pattern.compile("([A-Z][a-z]+)\\s([a-z]{2,})");
            Matcher m = latin.matcher(content);
            String regionNote = "";
            if (m.find()) {
                String scientific = m.group(1) + " " + m.group(2);
                try {
                    Optional<?> maybe = speciesRepository.findByScientificName(scientific);
                    if (region != null && !region.isBlank()) {
                        List<?> list = speciesRepository.findDistinctByRegionContaining(region);
                        boolean recordedInRegion = list.stream().anyMatch(s -> s.toString().contains(scientific));
                        if (recordedInRegion) {
                            regionNote = "This species has been recorded in our " + region + " dataset";
                        } else {
                            regionNote = "Not yet recorded in our " + region + " dataset for this project";
                        }
                    } else if (maybe.isPresent()) {
                        regionNote = "This species exists in our project dataset.";
                    } else {
                        regionNote = "This species is not present in our project dataset.";
                    }
                } catch (Exception ex) {
                    log.error("Failed checking species repository", ex);
                    regionNote = "Could not check project species data.";
                }
            } else if (region != null && !region.isBlank()) {
                regionNote = "Could not confidently detect a scientific name from the model response; region lookup skipped.";
            }

            return content + "\n\n" + (regionNote == null ? "" : regionNote);

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to identify species from photo", ex);
            throw new RuntimeException("I couldn’t identify the species from that image right now.");
        }
    }
}
