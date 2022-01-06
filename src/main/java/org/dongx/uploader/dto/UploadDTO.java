package org.dongx.uploader.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * UploadDTO
 *
 * @author <a href="mailto:dongxiang886@gmail.com">Dongx</a>
 * @since 1.0.0
 */
@Data
public class UploadDTO {

    /**
     * 文件
     */
    private MultipartFile file;

    /**
     * 当前分片数
     */
    private Integer currentChunk;

    /**
     * 总分片数
     */
    private Integer totalChunk;

    /**
     * 原文件 md5
     */
    private String md5;

    /**
     * 源文件名称
     */
    private String filename;

}
