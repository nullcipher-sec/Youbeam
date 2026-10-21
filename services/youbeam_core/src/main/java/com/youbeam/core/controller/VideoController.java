package com.youbeam.core.controller;

import com.youbeam.core.Interface.IVideoService;
import com.youbeam.core.commons.AuthenticatedUser;
import com.youbeam.core.config.OpenApiConfig;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.CreateVideoRequest;
import com.youbeam.core.model.request.UpdateVideoRequest;
import com.youbeam.core.model.response.VideoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * CRUD de videos. Todos los endpoints exigen sesión; solo el dueño edita o borra.
 */
@Tag(name = "Videos", description = "Video metadata management")
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME)
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final IVideoService videoService;

    @Operation(summary = "Create a video", description = "New videos are private and pending upload.")
    @PostMapping
    public ResponseEntity<VideoResponse> create(@Valid @RequestBody CreateVideoRequest request,
                                                AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoService.create(request, user));
    }

    @Operation(summary = "List my videos")
    @GetMapping
    public ResponseEntity<List<VideoResponse>> listOwn(AuthenticatedUser user) {
        return ResponseEntity.ok(videoService.listOwn(user));
    }

    @Operation(summary = "Get a video")
    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> get(@PathVariable UUID id, AuthenticatedUser user)
            throws ControllerException {
        return ResponseEntity.ok(videoService.get(id, user));
    }

    @Operation(summary = "Update a video", description = "Only the owner can update. Absent fields are kept.")
    @PatchMapping("/{id}")
    public ResponseEntity<VideoResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateVideoRequest request,
                                                AuthenticatedUser user) throws ControllerException {
        return ResponseEntity.ok(videoService.update(id, request, user));
    }

    @Operation(summary = "Delete a video", description = "Only the owner can delete. Soft delete.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, AuthenticatedUser user) throws ControllerException {
        videoService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
