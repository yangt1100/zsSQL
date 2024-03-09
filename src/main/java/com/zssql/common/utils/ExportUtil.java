package com.zssql.common.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.zssql.common.handler.ExportCountHandler;
import com.zssql.common.handler.ExportDataHandler;
import com.zssql.domain.dto.ErrorDto;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


/**
 * 导出工具类
 */
public class ExportUtil {
    // 导出超时时间（1小时）
    private static final long TIMEOUT_MILL = 60 * 60 * 1000L;
    // 最大导出记录数(50W)
    private static final int MAX_EXPORT_RECS = 50 * 10000;


    private ExportUtil() {
    }

    /**
     * 基于注解方式导出Excel文件
     * @param fileName 文件名
     * @param sheetName sheet名称
     * @param clazz     类（根据类字段注解生成标题）
     * @param writeHandler 导出Excel样式
     * @param dataHandler 获取导出数据处理器(循环获取，直至不返回数据)
     * @return
     * @throws IOException
     */
    public static File exportExcel(String fileName, String sheetName, Class clazz, WriteHandler writeHandler,
                                   ErrorDto errorDto, ExportDataHandler dataHandler) {
        return exportExcel(fileName, sheetName, clazz, writeHandler, errorDto, null, dataHandler);
    }

    /**
     * 基于注解方式导出Excel文件
     * @param fileName 文件名
     * @param sheetName sheet名称
     * @param clazz     类（根据类字段注解生成标题）
     * @param writeHandler 导出Excel样式
     * @param countHandler 导出数据量统计处理器
     * @param dataHandler 获取导出数据处理器(循环获取，直至不返回数据)
     * @return
     * @throws IOException
     */
    public static File exportExcel(String fileName, String sheetName, Class clazz, WriteHandler writeHandler, ErrorDto errorDto,
                                   ExportCountHandler countHandler, ExportDataHandler dataHandler) {
        File excelFile = null;
        try {
            if (null != countHandler && MAX_EXPORT_RECS < countHandler.countData()) {
                ErrorDto.setErrorInfo(errorDto, "导出数据量不能超过50万条");
                return null;
            }
            // 样式为空则使用默认样式
            writeHandler = CommonUtil.getValue(writeHandler, getDefaultWriterHandler());
            // 创建文件
            excelFile = new File(fileName);
            // 构建ExcelWriter
            ExcelWriter excelWriter = EasyExcel.write(new FileOutputStream(excelFile), clazz)
                    .excelType(ExcelTypeEnum.XLSX).registerWriteHandler(writeHandler).build();
            WriteSheet writeSheet = new WriteSheet();
            writeSheet.setSheetNo(1);
            writeSheet.setSheetName(sheetName);

            int count = 0;
            long startTime = System.currentTimeMillis();
            while (true) {
                // 循环读取并写入数据
                List datas = dataHandler.getExportData();
                if (CollectionUtils.isEmpty(datas)) {
                    break;
                }
                // 写入数据
                excelWriter.write(datas, writeSheet);
                // 超时判断
                if ((System.currentTimeMillis() - startTime) > TIMEOUT_MILL) {
                    excelWriter.finish();
                    ErrorDto.setErrorInfo(errorDto, "导出数据处理超时");
                    deleteFile(excelFile);
                    return null;
                }
                // 数据量判断
                count += datas.size();
                if (count > MAX_EXPORT_RECS) {
                    break;
                }
            }
            excelWriter.finish();
            return excelFile;
        }catch (Exception e) {
            e.printStackTrace();
            ErrorDto.setErrorInfo(errorDto, "导出文件发生异常");
            deleteFile(excelFile);
            return null;
        }
    }

    /**
     * 基于自定义表头导出Excel文件
     * @param fileName  文件名
     * @param sheetName  sheet名称
     * @param heads       表头
     * @param writeHandler 样式
     * @param errorDto      错误信息
     * @param dataHandler   导出数据获取处理器
     * @return
     */
    public static File exportExcel(String fileName, String sheetName, List<List<String>> heads, WriteHandler writeHandler,
                                  ErrorDto errorDto, ExportDataHandler dataHandler) {
        return exportExcel(fileName, sheetName, heads, writeHandler, errorDto, null, dataHandler);
    }

    /**
     * 基于自定义表头导出Excel文件
     * @param fileName  文件名
     * @param sheetName  sheet名称
     * @param heads       表头
     * @param writeHandler 样式
     * @param errorDto      错误信息
     * @param countHandler  导出数据量统计处理器
     * @param dataHandler   导出数据获取处理器
     * @return
     */
    public static File exportExcel(String fileName, String sheetName, List<List<String>> heads, WriteHandler writeHandler,
                                  ErrorDto errorDto, ExportCountHandler countHandler,ExportDataHandler dataHandler) {
        File excelFile = null;
        try {
            if (null != countHandler && MAX_EXPORT_RECS < countHandler.countData()) {
                ErrorDto.setErrorInfo(errorDto, "导出数据量不能超过50万条");
                return null;
            }
            // 样式为空则使用默认样式
            writeHandler = CommonUtil.getValue(writeHandler, getDefaultWriterHandler());
            // 创建文件
            excelFile = new File(fileName);
            // 构建ExcelWriter
            ExcelWriter excelWriter = EasyExcel.write(new FileOutputStream(excelFile)).head(heads)
                    .excelType(ExcelTypeEnum.XLSX).registerWriteHandler(writeHandler).build();
            WriteSheet writeSheet = new WriteSheet();
            writeSheet.setSheetNo(1);
            writeSheet.setSheetName(sheetName);

            int count = 0;
            long startTime = System.currentTimeMillis();
            while (true) {
                // 循环读取并写入数据
                List datas = dataHandler.getExportData();
                if (CollectionUtils.isEmpty(datas)) {
                    break;
                }
                // 写入数据
                excelWriter.write(datas, writeSheet);
                // 超时判断
                if ((System.currentTimeMillis() - startTime) > TIMEOUT_MILL) {
                    excelWriter.finish();
                    ErrorDto.setErrorInfo(errorDto, "导出数据处理超时");
                    deleteFile(excelFile);
                    return null;
                }
                // 数据量判断
                count += datas.size();
                if (count > MAX_EXPORT_RECS) {
                    break;
                }
            }
            excelWriter.finish();
            return excelFile;
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDto.setErrorInfo(errorDto, "导出文件发生异常");
            deleteFile(excelFile);
            return null;
        }
    }

    /**
     * 基于自定义表头导出Excel文件
     * @param file  文件
     * @param sheetName  sheet名称
     * @param heads       表头
     * @param writeHandler 样式
     * @param errorDto      错误信息
     * @param dataHandler   导出数据获取处理器
     * @return
     */
    public static boolean exportExcel(File file, String sheetName, List<List<String>> heads, WriteHandler writeHandler,
                                   ErrorDto errorDto, ExportDataHandler dataHandler) {
        return exportExcel(file, sheetName, heads, writeHandler, errorDto, null, dataHandler);
    }

    /**
     * 基于自定义表头导出Excel文件
     * @param file  文件
     * @param sheetName  sheet名称
     * @param heads       表头
     * @param writeHandler 样式
     * @param errorDto      错误信息
     * @param countHandler  导出数据量统计处理器
     * @param dataHandler   导出数据获取处理器
     * @return
     */
    public static boolean exportExcel(File file, String sheetName, List<List<String>> heads, WriteHandler writeHandler,
                                   ErrorDto errorDto, ExportCountHandler countHandler,ExportDataHandler dataHandler) {
        try {
            if (null != countHandler && MAX_EXPORT_RECS < countHandler.countData()) {
                ErrorDto.setErrorInfo(errorDto, "导出数据量不能超过50万条");
                return false;
            }
            // 样式为空则使用默认样式
            writeHandler = CommonUtil.getValue(writeHandler, getDefaultWriterHandler());
            // 构建ExcelWriter
            ExcelWriter excelWriter = EasyExcel.write(new FileOutputStream(file)).head(heads)
                    .excelType(ExcelTypeEnum.XLSX).registerWriteHandler(writeHandler).build();
            WriteSheet writeSheet = new WriteSheet();
            writeSheet.setSheetNo(1);
            writeSheet.setSheetName(sheetName);

            int count = 0;
            long startTime = System.currentTimeMillis();
            while (true) {
                // 循环读取并写入数据
                List datas = dataHandler.getExportData();
                if (CollectionUtils.isEmpty(datas)) {
                    break;
                }
                // 写入数据
                excelWriter.write(datas, writeSheet);
                // 超时判断
                if ((System.currentTimeMillis() - startTime) > TIMEOUT_MILL) {
                    excelWriter.finish();
                    ErrorDto.setErrorInfo(errorDto, "导出数据处理超时");
                    deleteFile(file);
                    return false;
                }
                // 数据量判断
                count += datas.size();
                if (count > MAX_EXPORT_RECS) {
                    break;
                }
            }
            excelWriter.finish();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDto.setErrorInfo(errorDto, "导出文件发生异常");
            deleteFile(file);
            return false;
        }
    }


    /**
     * 删除文件
     * @param file
     */
    private static void deleteFile(File file) {
        if (null == file || !file.exists()) {
            return;
        }

        file.delete();
    }

    /**
     * 导出默认样式
     *
     * @return
     */
    private static WriteHandler getDefaultWriterHandler() {
        //头样式
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontName("微软雅黑");
        headWriteFont.setFontHeightInPoints((short) 9);
        headWriteFont.setColor(IndexedColors.WHITE.getIndex());
        headWriteCellStyle.setWriteFont(headWriteFont);
        // 内容样式
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        WriteFont contentWriteFont = new WriteFont();
        contentWriteFont.setFontName("微软雅黑");
        contentWriteFont.setFontHeightInPoints((short) 9);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);

        return new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
    }
}