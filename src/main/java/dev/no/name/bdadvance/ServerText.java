package dev.no.name.bdadvance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Разбор системных сообщений Bloodstone: блоки, обрамлённые строками из чёрточек. */
public final class ServerText {
    private static final Pattern DELIMITER = Pattern.compile("^[^\\p{L}\\p{N}]*$");

    private ServerText() {
    }

    /** Строки сообщения без разделителей и пустых строк. */
    public static List<String> lines(String plain) {
        List<String> result = new ArrayList<>();
        for (String line : plain.split("\n")) {
            String trimmed = line.strip();
            if (!DELIMITER.matcher(trimmed).matches()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /** Сообщение начинается со строки-разделителя — это блок от сервера, а не реплика игрока. */
    public static boolean isBlock(String plain) {
        int newline = plain.indexOf('\n');
        return newline > 0 && DELIMITER.matcher(plain.substring(0, newline).strip()).matches();
    }
}
