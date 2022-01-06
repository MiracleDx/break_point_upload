package org.dongx.uploader.model;

import lombok.Data;

/**
 * File Cache
 *
 * @author <a href="mailto:dongxiang886@gmail.com">Dongx</a>
 * @since 1.0.0
 */
@Data
public class FileCache {

    private Boolean completed;

    private String path;

    private byte[] progress;
}
