package com.ssafy.nyamnyam.global.upload;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

/**
 * 허용 이미지 형식. 확장자나 Content-Type은 바꿔 보낼 수 있어 믿지 않고, 파일 앞부분(매직 바이트)으로 판별한다.
 */
public enum ImageFormat {
    JPEG("jpg"),
    PNG("png"),
    WEBP("webp");

    private final String extension;

    ImageFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    /** @return 판별한 형식, 허용 형식이 아니면 null */
    public static ImageFormat detect(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return PNG;
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return WEBP;
        }
        return null;
    }

    /** @return [width, height], 읽지 못하면 null */
    public int[] readSize(byte[] b) {
        try {
            return switch (this) {
                case PNG -> pngSize(b);
                case WEBP -> webpSize(b);
                case JPEG -> imageIoSize(b);
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** IHDR: 16~19 width, 20~23 height (빅엔디언) */
    private static int[] pngSize(byte[] b) {
        if (b.length < 24) {
            return null;
        }
        return new int[]{int32BE(b, 16), int32BE(b, 20)};
    }

    /** WebP 청크(VP8 손실 · VP8L 무손실 · VP8X 확장) 헤더에서 크기를 읽는다 */
    private static int[] webpSize(byte[] b) {
        if (b.length < 30) {
            return null;
        }
        String chunk = new String(b, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        switch (chunk) {
            case "VP8X" -> {
                int w = 1 + ((b[24] & 0xFF) | (b[25] & 0xFF) << 8 | (b[26] & 0xFF) << 16);
                int h = 1 + ((b[27] & 0xFF) | (b[28] & 0xFF) << 8 | (b[29] & 0xFF) << 16);
                return new int[]{w, h};
            }
            case "VP8 " -> {
                int w = ((b[26] & 0xFF) | (b[27] & 0xFF) << 8) & 0x3FFF;
                int h = ((b[28] & 0xFF) | (b[29] & 0xFF) << 8) & 0x3FFF;
                return new int[]{w, h};
            }
            case "VP8L" -> {
                int b0 = b[21] & 0xFF, b1 = b[22] & 0xFF, b2 = b[23] & 0xFF, b3 = b[24] & 0xFF;
                int w = 1 + (b0 | (b1 & 0x3F) << 8);
                int h = 1 + ((b1 >> 6) | b2 << 2 | (b3 & 0x0F) << 10);
                return new int[]{w, h};
            }
            default -> {
                return null;
            }
        }
    }

    private static int[] imageIoSize(byte[] b) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(b))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static int int32BE(byte[] b, int offset) {
        return (b[offset] & 0xFF) << 24 | (b[offset + 1] & 0xFF) << 16 | (b[offset + 2] & 0xFF) << 8 | (b[offset + 3] & 0xFF);
    }
}
