package com.miduo.cloud.ticket.application.ticket;

import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 内部富文本白名单及内嵌图片边界，避免私密图片进入公开工单附件。 */
public final class InternalCommentHtml {
    private static final Pattern IMAGE = Pattern.compile("^data:image/(png|jpeg|gif);base64,([A-Za-z0-9+/=]+)$");
    private static final Safelist SAFE = Safelist.relaxed()
            .addTags("span", "s", "hr", "input")
            .addAttributes(":all", "style")
            .addAttributes("span", "data-user-id", "data-comment-mention")
            .addAttributes("input", "checked")
            .addEnforcedAttribute("input", "type", "checkbox")
            .addEnforcedAttribute("input", "disabled", "disabled")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer")
            .removeProtocols("img", "src", "http", "https")
            .addProtocols("img", "src", "data");

    private InternalCommentHtml() { }

    public static String clean(String html) {
        return clean(html, true);
    }

    public static String cleanPublic(String html) {
        return clean(html, false);
    }

    private static String clean(String html, boolean internal) {
        if (html.length() > 3000000) fail("评论正文和图片总量过大");
        Document dirty = Jsoup.parseBodyFragment(html);
        int imageBytes = 0;
        if (internal && dirty.select("img").size() > 5) fail("每条内部备注最多5张图片");
        for (Element image : internal ? dirty.select("img") : new org.jsoup.select.Elements()) {
            imageBytes += validateImage(image.attr("src"));
            if (imageBytes > 2 * 1024 * 1024) fail("内部备注图片总量不能超过2MB");
        }
        Safelist rules = internal ? SAFE : new Safelist(SAFE)
                .removeProtocols("img", "src", "data").addProtocols("img", "src", "https", "http")
                .addTags("video", "source")
                .addAttributes("video", "src", "poster", "controls", "preload")
                .addAttributes("source", "src", "type")
                .addProtocols("video", "src", "http", "https")
                .addProtocols("video", "poster", "http", "https")
                .addProtocols("source", "src", "http", "https")
                .addEnforcedAttribute("video", "controls", "controls")
                .addEnforcedAttribute("video", "preload", "metadata");
        Document clean = new Cleaner(rules).clean(dirty);
        clean.select("img:not([src])").remove();
        clean.outputSettings().prettyPrint(false);
        for (Element element : clean.getAllElements()) {
            if (element.hasAttr("style")) {
                String style = safeStyle(element.attr("style"));
                if (style.isEmpty()) element.removeAttr("style");
                else element.attr("style", style);
            }
            if (element.hasAttr("data-user-id") && !element.attr("data-user-id").matches("[0-9]{1,19}")) {
                element.removeAttr("data-user-id");
            }
        }
        Document textOnly = clean.clone();
        textOnly.select("img").remove();
        if (textOnly.body().html().length() > 20000) fail("评论正文不能超过20000个字符（图片除外）");
        if (clean.body().text().trim().isEmpty() && clean.select("img").isEmpty()) fail("评论内容不能为空");
        return clean.body().html();
    }

    private static int validateImage(String src) {
        Matcher matcher = IMAGE.matcher(src);
        if (!matcher.matches()) fail("内部图片请使用上传或粘贴的 PNG、JPEG、GIF，不支持外链或SVG");
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "内部图片数据无效");
        }
        if (bytes.length > 512 * 1024) fail("内部备注单张图片不能超过512KB");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) fail("内部图片数据无效");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                if (!reader.getFormatName().equalsIgnoreCase(matcher.group(1))) fail("内部图片格式与内容不符");
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > 16000000) fail("内部图片尺寸过大");
            } finally {
                reader.dispose();
            }
        } catch (java.io.IOException ex) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "内部图片数据无效");
        }
        return bytes.length;
    }

    private static String safeStyle(String input) {
        StringBuilder result = new StringBuilder();
        for (String declaration : input.split(";")) {
            String[] parts = declaration.split(":", 2);
            if (parts.length != 2) continue;
            String key = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = parts[1].trim();
            boolean allowed = false;
            if (key.equals("color") || key.equals("background-color")) {
                allowed = value.matches("#[0-9a-fA-F]{3,8}|[a-zA-Z]{1,20}|rgba?\\([0-9., %]+\\)");
            } else if (key.equals("text-align")) {
                allowed = value.matches("left|right|center|justify");
            } else if (key.equals("font-size") || key.equals("line-height") || key.equals("text-indent")) {
                allowed = value.matches("[0-9]{1,3}(\\.[0-9]{1,2})?(px|pt|em|%)?");
            } else if (key.equals("font-family")) {
                allowed = value.matches("[\\p{L}0-9 ,\"'-]{1,100}");
            } else if (key.equals("font-weight")) {
                allowed = value.matches("normal|bold|[1-9]00");
            } else if (key.equals("font-style")) {
                allowed = value.matches("normal|italic");
            } else if (key.equals("text-decoration")) {
                allowed = value.matches("none|underline|line-through");
            }
            if (allowed) result.append(key).append(':').append(value).append(';');
        }
        return result.toString();
    }

    private static void fail(String message) {
        throw BusinessException.of(ErrorCode.PARAM_ERROR, message);
    }
}
