package ca.corbett.movienight.controller;
import ca.corbett.movienight.service.RuntimeConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/runtime-config")
@CrossOrigin(origins = "http://localhost:5173")
public class RuntimeConfigController {
    private final RuntimeConfigService runtimeConfigService;
    public RuntimeConfigController(RuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }
    @GetMapping("/fully-local")
    public FullyLocalResponse getFullyLocal() {
        return new FullyLocalResponse(runtimeConfigService.isFullyLocal());
    }
    @PutMapping("/fully-local")
    public ResponseEntity<FullyLocalResponse> setFullyLocal(@RequestBody FullyLocalUpdateRequest request) {
        if (request == null || request.fullyLocal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fullyLocal is required");
        }
        boolean currentValue = runtimeConfigService.setFullyLocal(request.fullyLocal());
        return ResponseEntity.ok(new FullyLocalResponse(currentValue));
    }
    public record FullyLocalResponse(boolean fullyLocal) {
    }
    public record FullyLocalUpdateRequest(Boolean fullyLocal) {
    }
}
