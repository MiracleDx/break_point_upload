package org.dongx.uploader.controller;

import org.dongx.uploader.dto.UploadDTO;
import org.dongx.uploader.model.ApiResponse;
import org.dongx.uploader.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Upload Controller
 *
 * @author <a href="mailto:dongxiang886@gmail.com">Dongx</a>
 * @since 1.0.0
 */
@CrossOrigin
@RequestMapping
@RestController
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @GetMapping("/fastUpload")
    public ApiResponse fastUpload(@RequestParam String md5) {
        return uploadService.fastUpload(md5);
    }

    @PostMapping("/breakPointUpload")
    public ApiResponse breakPointUpload(UploadDTO uploadDTO) {
        return uploadService.breakPointUpload(uploadDTO);
    }
}
