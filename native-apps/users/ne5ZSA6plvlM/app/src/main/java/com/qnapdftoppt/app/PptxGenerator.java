package com.qnapdftoppt.app;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Native Pure-Java PowerPoint (.pptx) Generator
 * Creates valid OpenXML PowerPoint presentation packages using standard ZipOutputStream.
 * Zero external libraries required. Fully compatible with MS PowerPoint & Google Slides.
 */
public class PptxGenerator {

    public static File createPresentation(Context context, String presentationTitle, List<QnAItem> qnaList, String watermarkText) throws Exception {
        String cleanName = presentationTitle.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (cleanName.trim().isEmpty()) cleanName = "QnA_Presentation";
        String fileName = cleanName + "_" + System.currentTimeMillis() + ".pptx";

        File exportDir = context.getExternalFilesDir(null);
        if (exportDir == null) exportDir = context.getFilesDir();
        File pptxFile = new File(exportDir, fileName);

        FileOutputStream fos = new FileOutputStream(pptxFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        int slideCount = qnaList.size() + 1; // Slide 1 = Title Slide, Slide 2..N = Q&A Slides

        // 1. [Content_Types].xml
        addZipEntry(zos, "[Content_Types].xml", getContentTypesXml(slideCount));

        // 2. _rels/.rels
        addZipEntry(zos, "_rels/.rels", getRelsXml());

        // 3. ppt/presentation.xml
        addZipEntry(zos, "ppt/presentation.xml", getPresentationXml(slideCount));

        // 4. ppt/_rels/presentation.xml.rels
        addZipEntry(zos, "ppt/_rels/presentation.xml.rels", getPresentationRelsXml(slideCount));

        // 5. ppt/slideMasters/slideMaster1.xml & rels
        addZipEntry(zos, "ppt/slideMasters/slideMaster1.xml", getSlideMasterXml());
        addZipEntry(zos, "ppt/slideMasters/_rels/slideMaster1.xml.rels", getSlideMasterRelsXml());

        // 6. ppt/slideLayouts/slideLayout1.xml & rels
        addZipEntry(zos, "ppt/slideLayouts/slideLayout1.xml", getSlideLayoutXml());
        addZipEntry(zos, "ppt/slideLayouts/_rels/slideLayout1.xml.rels", getSlideLayoutRelsXml());

        // 7. Generate Slide 1 (Title Slide)
        addZipEntry(zos, "ppt/slides/slide1.xml", getTitleSlideXml(presentationTitle, qnaList.size(), watermarkText));
        addZipEntry(zos, "ppt/slides/_rels/slide1.xml.rels", getSlideRelsXml());

        // 8. Generate Slide 2..N (Q&A Slides)
        for (int i = 0; i < qnaList.size(); i++) {
            int slideIndex = i + 2;
            QnAItem item = qnaList.get(i);
            String slideXml = getQnASlideXml(i + 1, item.getQuestion(), item.getAnswer(), watermarkText);
            addZipEntry(zos, "ppt/slides/slide" + slideIndex + ".xml", slideXml);
            addZipEntry(zos, "ppt/slides/_rels/slide" + slideIndex + ".xml.rels", getSlideRelsXml());
        }

        zos.close();
        fos.close();

        return pptxFile;
    }

    private static void addZipEntry(ZipOutputStream zos, String entryName, String content) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        OutputStreamWriter writer = new OutputStreamWriter(zos, StandardCharsets.UTF_8);
        writer.write(content);
        writer.flush();
        zos.closeEntry();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    private static String getContentTypesXml(int slideCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n");
        sb.append("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n");
        sb.append("  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n");
        sb.append("  <Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>\n");
        sb.append("  <Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/>\n");
        sb.append("  <Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/>\n");
        for (int i = 1; i <= slideCount; i++) {
            sb.append("  <Override PartName=\"/ppt/slides/slide").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>\n");
        }
        sb.append("</Types>");
        return sb.toString();
    }

    private static String getRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"ppt/presentation.xml\"/>\n" +
                "</Relationships>";
    }

    private static String getPresentationXml(int slideCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<p:presentation xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n");
        sb.append("  <p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rIdMaster\"/></p:sldMasterIdLst>\n");
        sb.append("  <p:sldIdLst>\n");
        for (int i = 1; i <= slideCount; i++) {
            sb.append("    <p:sldId id=\"").append(255 + i).append("\" r:id=\"rId").append(i).append("\"/>\n");
        }
        sb.append("  </p:sldIdLst>\n");
        sb.append("  <p:sldSz cx=\"9144000\" cy=\"6858000\"/>\n"); // Standard 4:3 / 16:9 widescreen box
        sb.append("</p:presentation>");
        return sb.toString();
    }

    private static String getPresentationRelsXml(int slideCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n");
        sb.append("  <Relationship Id=\"rIdMaster\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>\n");
        for (int i = 1; i <= slideCount; i++) {
            sb.append("  <Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide").append(i).append(".xml\"/>\n");
        }
        sb.append("</Relationships>");
        return sb.toString();
    }

    private static String getSlideMasterXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<p:sldMaster xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n" +
                "  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>\n" +
                "  <p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rIdLayout\"/></p:sldLayoutIdLst>\n" +
                "</p:sldMaster>";
    }

    private static String getSlideMasterRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rIdLayout\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>\n" +
                "</Relationships>";
    }

    private static String getSlideLayoutXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<p:sldLayout xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" type=\"title\">\n" +
                "  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>\n" +
                "</p:sldLayout>";
    }

    private static String getSlideLayoutRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"../slideMasters/slideMaster1.xml\"/>\n" +
                "</Relationships>";
    }

    private static String getSlideRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>\n" +
                "</Relationships>";
    }

    private static String getTitleSlideXml(String title, int totalQuestions, String watermark) {
        String safeTitle = escapeXml(title);
        String safeSub = escapeXml("Questions & Answers Deck (" + totalQuestions + " Questions)");
        String safeWm = escapeXml(watermark);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n" +
                "  <p:cSld>\n" +
                "    <p:spTree>\n" +
                "      <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n" +
                "      <p:grpSpPr/>\n" +
                "      <!-- Title Shape -->\n" +
                "      <p:sp>\n" +
                "        <p:nvSpPr><p:cNvPr id=\"2\" name=\"Title Box\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n" +
                "        <p:spPr><a:xfrm><a:off x=\"914400\" y=\"1828800\"/><a:ext cx=\"7315200\" cy=\"1828800\"/></a:xfrm></p:spPr>\n" +
                "        <p:txBody><a:bodyPr align=\"ctr\"/><a:lstStyle/><a:p><a:r><a:rPr sz=\"3600\" b=\"1\"><a:solidFill><a:srgbClr val=\"1E88E5\"/></a:solidFill></a:rPr><a:t>" + safeTitle + "</a:t></a:r></a:p></p:txBody>\n" +
                "      </p:sp>\n" +
                "      <!-- Subtitle Shape -->\n" +
                "      <p:sp>\n" +
                "        <p:nvSpPr><p:cNvPr id=\"3\" name=\"Subtitle Box\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n" +
                "        <p:spPr><a:xfrm><a:off x=\"914400\" y=\"3800000\"/><a:ext cx=\"7315200\" cy=\"900000\"/></a:xfrm></p:spPr>\n" +
                "        <p:txBody><a:bodyPr align=\"ctr\"/><a:lstStyle/><a:p><a:r><a:rPr sz=\"2000\"><a:solidFill><a:srgbClr val=\"555555\"/></a:solidFill></a:rPr><a:t>" + safeSub + "</a:t></a:r></a:p></p:txBody>\n" +
                "      </p:sp>\n" +
                "      <!-- Footer Watermark -->\n" +
                "      <p:sp>\n" +
                "        <p:nvSpPr><p:cNvPr id=\"4\" name=\"Footer Box\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n" +
                "        <p:spPr><a:xfrm><a:off x=\"914400\" y=\"6000000\"/><a:ext cx=\"7315200\" cy=\"400000\"/></a:xfrm></p:spPr>\n" +
                "        <p:txBody><a:bodyPr align=\"ctr\"/><a:lstStyle/><a:p><a:r><a:rPr sz=\"1200\" i=\"1\"><a:solidFill><a:srgbClr val=\"888888\"/></a:solidFill></a:rPr><a:t>" + safeWm + "</a:t></a:r></a:p></p:txBody>\n" +
                "      </p:sp>\n" +
                "    </p:spTree>\n" +
                "  </p:cSld>\n" +
                "</p:sld>";
    }

    private static String getQnASlideXml(int qNum, String question, String answer, String watermark) {
        String safeQ = escapeXml("Q" + qNum + ": " + question);
        String safeA = escapeXml("Answer:\n" + answer);
        String safeWm = escapeXml(watermark);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n" +
                "  <p:cSld>\n" +
                "    <p:spTree>\n" +
                "      <p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>\n" +
                "      <p:grpSpPr/>\n" +
                "      <!-- Question Card Box -->\n" +
                "      <p:sp>\n" +
                "        <p:nvSpPr><p:cNvPr id=\"2\" name=\"Question Box\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n" +
                "        <p:spPr><a:xfrm><a:off x=\"600000\" y=\"600000\"/><a:ext cx=\"7944000\" cy=\"2000000\"/></a:xfrm><a:solidFill><a:srgbClr val=\"E3F2FD\"/></a:solidFill></p:spPr>\n" +
                "        <p:txBody><a:bodyPr lIns=\"144000\" tIns=\"144000\"/><a:lstStyle/><a:p><a:r><a:rPr sz=\"2200\" b=\"1\"><a:solidFill><a:srgbClr val=\"1565C0\"/></a:solidFill></a:rPr><a:t>" + safeQ + "</a:t></a:r></a:p></p:txBody>\n" +
                "      </p:sp>\n" +
                "      <!-- Answer Card Box -->\n" +
                "      <p:sp>\n" +
                "        <p:nvSpPr><p:cNvPr id=\"3\" name=\"Answer Box\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n" +
                "        <p:spPr><a:xfrm><a:off x=\"600000\" y=\"2800000\"/><a:ext cx=\"7944000\" cy=\"3200000\"/></a:xfrm><a:solidFill><a:srgbClr val=\"F5F7FA\"/></a:solidFill></p:spPr>\n" +
                "        <p:txBody><a:bodyPr lIns=\"144000\" tIns=\"144000\"/><a:lstStyle/><a:p><a:r><a:rPr sz=\"1800\"><a:solidFill><a:srgbClr val=\"212121\"/></a:solidFill></a:rPr><a:t>" + safeA + "</a:t></a:r></a:p></p:txBody>\n" +
                "      </p:sp>\n" +
                "      <!-- Footer Watermark -->\n" +
                "      <p:sp>\n" +
                "        <p:nvSpPr><p:cNvPr id=\"4\" name=\"Footer Box\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>\n" +
                "        <p:spPr><a:xfrm><a:off x=\"600000\" y=\"6200000\"/><a:ext cx=\"7944000\" cy=\"300000\"/></a:xfrm></p:spPr>\n" +
                "        <p:txBody><a:bodyPr align=\"r\"/><a:lstStyle/><a:p><a:r><a:rPr sz=\"1000\" i=\"1\"><a:solidFill><a:srgbClr val=\"999999\"/></a:solidFill></a:rPr><a:t>" + safeWm + "</a:t></a:r></a:p></p:txBody>\n" +
                "      </p:sp>\n" +
                "    </p:spTree>\n" +
                "  </p:cSld>\n" +
                "</p:sld>";
    }
}