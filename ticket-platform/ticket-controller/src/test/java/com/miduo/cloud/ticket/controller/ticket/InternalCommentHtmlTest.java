package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.InternalCommentHtml;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class InternalCommentHtmlTest {
    @Test
    void publicSanitizingKeepsPluginImagesAndVideoButRemovesActiveMarkup() {
        String cleaned = InternalCommentHtml.cleanPublic("<p>客户补充</p><img src='https://example.test/image.png' onerror='bad()'><video src='https://example.test/video.mp4' onloadstart='bad()' controls></video><script>bad()</script>");
        assertTrue(cleaned.contains("https://example.test/image.png"));
        assertTrue(cleaned.contains("<video"));
        assertTrue(cleaned.contains("https://example.test/video.mp4"));
        assertFalse(cleaned.contains("onerror"));
        assertFalse(cleaned.contains("onloadstart"));
        assertFalse(cleaned.contains("script"));
    }
    @Test
    void retainsFormattingTableAndBoundedPrivateImage() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String src = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        String html = InternalCommentHtml.clean("<p style='font-size:20px;color:rgb(255, 0, 0)'><b>标题</b><u>下划线</u></p><table><tr><td>单元格</td></tr></table><img src='" + src + "' onerror='alert(1)'>");
        assertTrue(html.contains(src));
        assertTrue(html.contains("<b>标题</b>"));
        assertTrue(html.contains("<td>单元格</td>"));
        assertTrue(html.contains("font-size:20px"));
        assertFalse(html.contains("onerror"));
        assertEquals(1, org.jsoup.Jsoup.parseBodyFragment(html).select("img").size());
    }

    @Test
    void refusesExternalSvgInvalidAndOversizeImages() {
        for (String src : new String[]{"https://cdn.test/private.png", "data:image/svg+xml;base64,PHN2Zz4=", "data:image/png;base64,eA==",
                "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[512 * 1024 + 1])}) {
            assertThrows(BusinessException.class, () -> InternalCommentHtml.clean("<p>备注</p><img src='" + src + "'>"));
        }
    }

    @Test
    void stripsActiveMarkupAndRejectsEmptyMarkup() {
        String cleaned = InternalCommentHtml.clean("<p onclick='bad()' style='background-image:url(https://bad.test)'>内容<a href='javascript:alert(1)'>链接</a></p><iframe src='https://bad.test'></iframe><svg onload='bad()'></svg>");
        assertFalse(cleaned.contains("javascript"));
        assertFalse(cleaned.contains("onclick"));
        assertFalse(cleaned.contains("iframe"));
        assertFalse(cleaned.contains("bad.test"));
        assertThrows(BusinessException.class, () -> InternalCommentHtml.clean("<p><br></p>"));
        assertThrows(BusinessException.class, () -> InternalCommentHtml.clean("<script>alert(1)</script>"));
    }
}
