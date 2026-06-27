package com.example.eams.common.util;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import com.example.eams.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Excel 导入导出工具类
 * <p>
 * 基于 Apache POI + Hutool 封装。
 * 导入: 支持 .xls/.xlsx，校验文件格式和大小。
 * 导出: 支持自定义列头和字段映射，浏览器直接下载。
 */
@Slf4j
public class ExcelUtil {

    /** 单次导入最大条数 */
    private static final int MAX_IMPORT_ROWS = 1000;

    /** 允许的文件扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            new HashSet<>(Arrays.asList("xls", "xlsx"));

    // ==================== 导入 ====================

    /**
     * 读取 Excel 文件（Spring MultipartFile）
     */
    public static List<Map<String, Object>> read(MultipartFile file) {
        return read(file, MAX_IMPORT_ROWS);
    }

    /**
     * 读取 Excel 文件（Spring MultipartFile）
     */
    public static List<Map<String, Object>> read(MultipartFile file, int maxRows) {
        validateSpringFile(file);
        try (InputStream in = file.getInputStream();
             ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(in)) {
            List<Map<String, Object>> rows = reader.readAll();
            if (rows == null || rows.isEmpty()) {
                throw new BusinessException(400, "文件内容为空，请检查后重新上传");
            }
            if (rows.size() > maxRows) {
                throw new BusinessException(400,
                        "单次导入最多" + maxRows + "条，当前" + rows.size() + "条，请分批导入");
            }
            return rows;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excel读取失败", e);
            throw new BusinessException(400, "文件读取失败，请检查文件格式");
        }
    }

    /**
     * 读取 Excel 文件为 List<Map>（表头为 key）- 旧版Wrapper接口
     *
     * @param file   上传的Excel文件
     * @param maxRows 最大行数限制
     * @return 每行一个 Map（key=表头列名, value=单元格值）
     */
    public static List<Map<String, Object>> read(MultipartFileWrapper file, int maxRows) {
        validateFile(file);
        try (InputStream in = file.getInputStream();
             ExcelReader reader = cn.hutool.poi.excel.ExcelUtil.getReader(in)) {
            List<Map<String, Object>> rows = reader.readAll();
            if (rows == null || rows.isEmpty()) {
                throw new BusinessException(400, "文件内容为空，请检查后重新上传");
            }
            if (rows.size() > maxRows) {
                throw new BusinessException(400,
                        "单次导入最多" + maxRows + "条，当前" + rows.size() + "条，请分批导入");
            }
            return rows;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excel读取失败", e);
            throw new BusinessException(400, "文件读取失败，请检查文件格式");
        }
    }

    /**
     * 读取 Excel（默认最大1000条）
     */
    public static List<Map<String, Object>> read(MultipartFileWrapper file) {
        return read(file, MAX_IMPORT_ROWS);
    }

    // ==================== 导出 ====================

    /**
     * 导出 Excel 并写入 HTTP 响应（浏览器直接下载）
     *
     * @param response    HttpServletResponse
     * @param fileName    文件名（不含扩展名），如 "资产台账_20260624"
     * @param headerAlias 表头映射: LinkedHashMap<字段名, 中文列名>
     * @param dataList    数据列表: List<Map<String, Object>>
     */
    public static void export(HttpServletResponse response,
                              String fileName,
                              LinkedHashMap<String, String> headerAlias,
                              List<Map<String, Object>> dataList) {
        try (ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(true)) {
            // 设置表头别名
            writer.setHeaderAlias(headerAlias);
            // 写入数据（只写 headerAlias 中声明的字段）
            writer.write(dataList, true);

            // 设置响应头
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedFileName = URLEncoder.encode(fileName,
                    StandardCharsets.UTF_8.name()).replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + encodedFileName + ".xlsx");

            writer.flush(response.getOutputStream(), true);
        } catch (IOException e) {
            log.error("Excel导出失败", e);
            throw new BusinessException(500, "文件导出失败，请重试");
        }
    }

    // ==================== 内部方法 ====================

    private static void validateSpringFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(400, "请上传 .xlsx 或 .xls 格式文件");
        }
        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "请上传 .xlsx 或 .xls 格式文件");
        }
    }

    private static void validateFile(MultipartFileWrapper file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(400, "请上传 .xlsx 或 .xls 格式文件");
        }
        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "请上传 .xlsx 或 .xls 格式文件");
        }
    }

    /**
     * MultipartFile 包装接口
     * <p>
     * 解耦 Spring MultipartFile 依赖，方便单测 mock。
     */
    public interface MultipartFileWrapper {
        String getOriginalFilename();
        InputStream getInputStream() throws IOException;
        boolean isEmpty();
    }
}
