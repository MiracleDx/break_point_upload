package org.dongx.uploader.service;

import lombok.extern.slf4j.Slf4j;
import org.dongx.uploader.dto.UploadDTO;
import org.dongx.uploader.model.ApiResponse;
import org.dongx.uploader.model.FileCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Upload Service
 *
 * @author <a href="mailto:dongxiang886@gmail.com">Dongx</a>
 * @since 1.0.0
 */
@Slf4j
@Service
public class UploadService {

    private Map<String, FileCache> uploadCache = new ConcurrentHashMap<>();

    @Value("${upload.chunkSize}")
    private Long chunkSize;

    @Value("${upload.dir}")
    private String dir;

    private static final byte IS_COMPLETE = 1;


    /**
     * 快速上传
     *
     * @param md5
     * @return
     */
    public ApiResponse fastUpload(String md5) {
        boolean isUploaded = uploadCache.containsKey(md5);

        // 未上传过
        if (!isUploaded) {
            return ApiResponse.success("The file didn't uploaded.", null);
        }

        FileCache fileCache = uploadCache.get(md5);
        Boolean completed = fileCache.getCompleted();
        String path = fileCache.getPath();

        // 上传完成返回路径
        if (completed) {
            log.info("The file already uploaded.");
            return ApiResponse.success(path);
        } else {
            // 没有上传完成，返回缺少的分片数
            List<Integer> missChunkList = new ArrayList<>();
            byte[] progress = fileCache.getProgress();
            for (int i = 0; i < progress.length; i++) {
                if (progress[i] != IS_COMPLETE) {
                    missChunkList.add(i);
                }
            }
            return ApiResponse.success("The file was uploaded partially.", missChunkList);
        }
    }

    /**
     * 断点续传
     *
     * @param uploadDTO
     * @return
     */
    public ApiResponse breakPointUpload(UploadDTO uploadDTO) {
        // 上传路径
        String md5 = uploadDTO.getMd5();
        Integer currentChunk = uploadDTO.getCurrentChunk();
        String uploadDir = dir + File.separator + md5;
        File tmpDir = new File(uploadDir);
        // 创建上传路径
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        MultipartFile file = uploadDTO.getFile();
        String filename = uploadDTO.getFilename();

        // 上传临时文件名
        String tmpFilename = filename + "_tmp";
        // 临时文件的路径
        String filepath = uploadDir + File.separator + tmpFilename;
        File tmpFile = new File(filepath);

        try {
            // fileChannel
            RandomAccessFile randomAccessFile = new RandomAccessFile(tmpFile, "rw");
            FileChannel channel = randomAccessFile.getChannel();

            // 当前分片偏移量
            long offset = chunkSize * currentChunk;
            byte[] fileData = file.getBytes();
            // 将偏移量写入文件
            MappedByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, offset, fileData.length);
            byteBuffer.put(fileData);
            freedMappedByteBuffer(byteBuffer);
            randomAccessFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 判断是否上传完成
        boolean isOk = checkProgress(uploadDTO, filepath);
        if (isOk) {
            if (!tmpFile.exists() || tmpFile.isDirectory()) {
                log.info("File does not exist: {}.", tmpFile.getName());
            } else {
                // 上传完成修改为文件名
                String realpath = uploadDir + File.separator + filename;
                File newFile = new File(realpath);
                tmpFile.renameTo(newFile);
                // 更新真实文件名称
                uploadCache.get(md5).setPath(realpath);
            }
            return ApiResponse.success("The file upload success.", filepath);
        }
        return ApiResponse.success("The file part " + currentChunk + " upload success.", currentChunk);
    }

    /**
     * 判断是否上传完成
     *
     * @param uploadDTO
     * @param filepath
     * @return
     */
    protected Boolean checkProgress(UploadDTO uploadDTO, String filepath) {
        String md5 = uploadDTO.getMd5();
        FileCache fileCache = uploadCache.get(md5);
        Integer totalChunk = uploadDTO.getTotalChunk();
        Integer currentChunk = uploadDTO.getCurrentChunk();

        // 各分片的完成进度
        byte[] progress;
        if (fileCache == null) {
            fileCache = new FileCache();
            progress = new byte[totalChunk];
            fileCache.setProgress(progress);
        } else {
            progress = fileCache.getProgress();
        }

        // 上传完成标志位
        progress[currentChunk] = IS_COMPLETE;

        List<Integer> unProgresses = new ArrayList<>();
        for (Integer i = 0; i < progress.length; i++) {
            if (progress[i] != IS_COMPLETE) {
                unProgresses.add(i);
            } else {
                log.info("check file part {} completed", i);
            }
        }

        boolean unCompleted = unProgresses.isEmpty();
        if (unCompleted) {
            fileCache.setCompleted(true);
            fileCache.setPath(filepath);
        } else {
            fileCache.setCompleted(false);
            fileCache.setProgress(progress);
        }
        uploadCache.put(md5, fileCache);
        return unCompleted;
    }

    /**
     * 在MappedByteBuffer释放后再对它进行读操作的话就会引发jvm crash，在并发情况下很容易发生
     * 正在释放时另一个线程正开始读取，于是crash就发生了。所以为了系统稳定性释放前一般需要检查是否还有线程在读或写
     * @param mappedByteBuffer
     */
    public static void freedMappedByteBuffer(final MappedByteBuffer mappedByteBuffer) {
        try {
            if (mappedByteBuffer == null) {
                return;
            }

            mappedByteBuffer.force();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner", new Class[0]);
                        getCleanerMethod.setAccessible(true);
                        sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(mappedByteBuffer,
                                new Object[0]);
                        cleaner.clean();
                    } catch (Exception e) {
                        log.error("clean MappedByteBuffer error", e);
                    }
                    log.info("clean MappedByteBuffer completed");
                    return null;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
