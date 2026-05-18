package org.example.cloudapp.service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.List;
import org.example.cloudapp.entity.FileScanStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BouncerVirusScanClient {
    private static final String PROVIDER = "Bouncer";

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public BouncerVirusScanClient(RestClient.Builder builder,
                                  @Value("${cloudapp.virus-scan.base-url}") String baseUrl,
                                  @Value("${cloudapp.virus-scan.api-key:}") String apiKey,
                                  @Value("${cloudapp.virus-scan.enabled:false}") boolean enabled) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.enabled = enabled;
    }

    public ScanResponse scan(MultipartFile file) {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            return new ScanResponse(FileScanStatus.SKIPPED, PROVIDER, "Антивирусная проверка не настроена", null);
        }

        try {
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new NamedByteArrayResource(file.getBytes(), filename(file)));

            BouncerResponse response = restClient.post()
                    .uri("/v1/scan")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(BouncerResponse.class);

            return toScanResponse(response);
        } catch (IOException ex) {
            return new ScanResponse(FileScanStatus.FAILED, PROVIDER, "Не удалось подготовить файл для антивирусной проверки", null);
        } catch (RuntimeException ex) {
            return new ScanResponse(FileScanStatus.FAILED, PROVIDER, "Антивирусный сервис временно недоступен", null);
        }
    }

    private ScanResponse toScanResponse(BouncerResponse response) {
        if (response == null || response.data() == null) {
            return new ScanResponse(FileScanStatus.FAILED, PROVIDER, "Антивирусный сервис не вернул результат", null);
        }

        BouncerScanData data = response.data();
        String verdict = data.verdict();
        if ("clean".equalsIgnoreCase(verdict)) {
            return new ScanResponse(FileScanStatus.CLEAN, PROVIDER, "Файл прошел антивирусную проверку", null);
        }
        if ("malicious".equalsIgnoreCase(verdict) || "suspicious".equalsIgnoreCase(verdict)) {
            return new ScanResponse(FileScanStatus.INFECTED, PROVIDER, "Файл заблокирован: обнаружена угроза", threats(data.threats()));
        }
        if ("pending".equalsIgnoreCase(data.status())) {
            return new ScanResponse(FileScanStatus.PENDING, PROVIDER, "Файл принят на антивирусную проверку", null);
        }
        return new ScanResponse(FileScanStatus.FAILED, PROVIDER, "Антивирусная проверка завершилась без понятного verdict", null);
    }

    private String threats(List<BouncerThreat> threats) {
        if (threats == null || threats.isEmpty()) {
            return "Обнаружена угроза";
        }
        return String.join(", ", threats.stream().map(BouncerThreat::name).toList());
    }

    private String filename(MultipartFile file) {
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
    }

    public record ScanResponse(FileScanStatus status, String provider, String message, String threats) {
    }

    private record BouncerResponse(
            String status,
            BouncerScanData data
    ) {
    }

    private record BouncerScanData(
            @JsonProperty("scan_id") String scanId,
            String status,
            String verdict,
            List<BouncerThreat> threats
    ) {
    }

    private record BouncerThreat(String name) {
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
