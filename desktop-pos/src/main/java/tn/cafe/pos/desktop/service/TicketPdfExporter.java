package tn.cafe.pos.desktop.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;

public final class TicketPdfExporter {
    private TicketPdfExporter() {}

    public static void write(Path path, String service, String client) throws IOException {
        String text = (service == null ? "" : service) + "\n\n" + (client == null ? "" : client);
        writeText(path, text);
    }

    public static void writeSingle(Path path, String ticket) throws IOException {
        writeText(path, ticket == null ? "" : ticket);
    }

    private static void writeText(Path path, String text) throws IOException {
        List<String> lines = wrapLines(text.replace('\r', '\n').lines().toList(), 30);
        int pageHeight = Math.max(360, 30 + lines.size() * 12);
        StringBuilder stream = new StringBuilder("BT\n/F1 8 Tf\n12 ")
            .append(pageHeight - 18).append(" Td\n");
        for (String line : lines) {
            stream.append('(').append(escape(line)).append(") Tj\n0 -12 Td\n");
        }
        stream.append("ET\n");
        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
        String[] objects = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 164 " + pageHeight + "] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>",
                "<< /Length " + streamBytes.length + " >>\nstream\n" + stream + "endstream"
        };
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
            pdf.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) pdf.append(String.format("%010d 00000 n %n", offset));
        pdf.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        Files.writeString(path, pdf.toString(), StandardCharsets.ISO_8859_1);
    }

    private static List<String> wrapLines(List<String> source, int width) {
        var result = new java.util.ArrayList<String>();
        for (String line : source) {
            String value = line == null ? "" : line;
            if (value.isEmpty()) { result.add(""); continue; }
            String remaining = value.strip();
            while (remaining.length() > width) {
                int split = remaining.lastIndexOf(' ', width);
                if (split <= 0) split = width;
                result.add(remaining.substring(0, split).stripTrailing());
                remaining = remaining.substring(split).stripLeading();
            }
            result.add(remaining);
        }
        return result;
    }

    private static String escape(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('–', '-')
            .replace('—', '-')
            .replace('•', '-')
            .replace('’', '\'');
        return ascii.replaceAll("[^\\x20-\\x7E]", " ")
            .replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
