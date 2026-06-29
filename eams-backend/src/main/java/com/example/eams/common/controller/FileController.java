package com.example.eams.common.controller;

import com.example.eams.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传接口
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Value("${eams.upload.path:}")
    private String uploadPath;

    private String getUploadDir() {
        return uploadPath;
    }

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"};

    /**
     * 上传文件
     * POST /api/file/upload
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "请选择要上传的文件");
        }

        // 校验文件大小
        if (file.getSize() > MAX_SIZE) {
            return Result.fail(400, "文件大小不能超过10MB");
        }

        // 校验文件类型
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return Result.fail(400, "不支持的文件类型");
        }
        String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        boolean allowed = false;
        for (String ae : ALLOWED_EXTENSIONS) {
            if (ae.equals(ext)) { allowed = true; break; }
        }
        if (!allowed) {
            return Result.fail(400, "不支持的文件类型：" + originalName);
        }

        // 存储文件
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String dirPath = getUploadDir() + "/" + dateDir;
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();

            // 在文件名中嵌入原始名称（去除特殊字符），便于前端展示原始文件名
            String safeOriginalName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String newName = UUID.randomUUID().toString() + "_" + safeOriginalName;
            String fullPath = dirPath + "/" + newName;
            file.transferTo(new File(fullPath));

            // 返回相对路径
            String url = "/uploads/" + dateDir + "/" + newName;
            return Result.ok(url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.fail(500, "文件上传失败，请重试");
        }
    }
}
