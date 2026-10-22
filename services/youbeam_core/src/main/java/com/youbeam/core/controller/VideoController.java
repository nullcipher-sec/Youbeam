package com.youbeam.core.controller;

import com.youbeam.core.Interface.IPlaybackTicketService;
import com.youbeam.core.Interface.IVideoService;
import com.youbeam.core.Interface.IVideoUploadService;
import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.config.OpenApiConfig;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.CreateVideoRequest;
import com.youbeam.core.model.request.UpdateVideoRequest;
import com.youbeam.core.model.response.PlaybackTicketResponse;
import com.youbeam.core.model.response.VideoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD de videos. El catálogo y la consulta de un video son públicos; el resto exige sesión.
 * Solo el dueño edita o borra.
 */
@Tag(name = "Videos", description = "Gestión de los metadatos de los videos")
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final IVideoService videoService;
    private final IPlaybackTicketService playbackTicketService;
    private final IVideoUploadService videoUploadService;

    @Operation(summary = "Crear un video", description = "Los videos nuevos son privados y quedan pendientes de subida.",
            security = @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME))
    @PostMapping
    public ResponseEntity<VideoResponse> create(@Valid @RequestBody CreateVideoRequest request,
                                                AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoService.create(request, user));
    }

    @Operation(summary = "Listar mis videos", security = @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME))
    @GetMapping
    public ResponseEntity<List<VideoResponse>> listOwn(AuthenticatedUser user) {
        return ResponseEntity.ok(videoService.listOwn(user));
    }

    // El tamaño de página está acotado para que nadie pueda volcar todo el catálogo en una petición
    @Operation(summary = "Catálogo público", description = "Videos públicos y listos, del más reciente al más antiguo. No requiere sesión.")
    @GetMapping("/catalog")
    public ResponseEntity<List<VideoResponse>> catalog(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(videoService.catalog(page, size));
    }

    @Operation(summary = "Consultar un video",
            description = "Los videos públicos y ocultos los ve cualquiera que tenga el id; los privados, solo su dueño.")
    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> get(@PathVariable UUID id,
                                             @Parameter(hidden = true) Optional<AuthenticatedUser> user)
            throws ControllerException {
        return ResponseEntity.ok(videoService.get(id, user.orElse(null)));
    }

    // no-store: el ticket es una credencial y no debe quedar en cachés del navegador ni de proxies
    @Operation(summary = "Obtener un ticket de reproducción",
            description = "JWT ES256 de corta duración para el Streaming Server. Responde 403 si el usuario no puede ver el video.")
    @GetMapping("/{id}/play")
    public ResponseEntity<PlaybackTicketResponse> play(@PathVariable UUID id,
                                                       @Parameter(hidden = true) Optional<AuthenticatedUser> user)
            throws ControllerException {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(playbackTicketService.issue(id, user.orElse(null)));
    }

    // El usuario va antes que el archivo: los argumentos se resuelven en orden y, con
    // resolve-lazily, una petición sin sesión recibe 401 antes de que se lea el multipart
    @Operation(summary = "Subir el archivo de un video",
            description = "Solo el dueño, y solo mientras el video no esté listo. El servidor lo transcodifica, "
                    + "lo cifra y lo guarda en el bucket; al terminar el video queda listo para reproducir.",
            security = @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME))
    @PostMapping(path = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponse> upload(@PathVariable UUID id, AuthenticatedUser user,
                                                @RequestPart("file") MultipartFile file) throws ControllerException {
        return ResponseEntity.ok(videoUploadService.upload(id, file, user));
    }

    @Operation(summary = "Actualizar un video", description = "Solo el dueño puede actualizarlo. Los campos ausentes se conservan.",
            security = @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME))
    @PatchMapping("/{id}")
    public ResponseEntity<VideoResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateVideoRequest request,
                                                AuthenticatedUser user) throws ControllerException {
        return ResponseEntity.ok(videoService.update(id, request, user));
    }

    @Operation(summary = "Eliminar un video", description = "Solo el dueño puede eliminarlo. Es un borrado lógico (soft delete).",
            security = @SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, AuthenticatedUser user) throws ControllerException {
        videoService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
