package com.edinsonamaya.control_deudas.folder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping("/api/users/me/folders")
    public ResponseEntity<List<FolderResponse>> listMyFolders() {
        return ResponseEntity.ok(folderService.listForCurrentUser());
    }

    @PostMapping("/api/users/me/folders")
    public ResponseEntity<FolderResponse> createMyFolder(@RequestBody @Valid FolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(folderService.create(request));
    }

    @GetMapping("/api/folders/{folderId}")
    public ResponseEntity<FolderDetailResponse> getFolderDetail(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.getDetail(folderId));
    }
}
