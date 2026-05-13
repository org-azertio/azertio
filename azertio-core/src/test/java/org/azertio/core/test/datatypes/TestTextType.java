package org.azertio.core.test.datatypes;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.azertio.core.datatypes.CoreDataTypes.*;

class TestTextType {

    @Test
    void testWord() {
        assertTrue(WORD.matcher("hello").matches());
        assertTrue(WORD.matcher("my-word").matches());
        assertEquals("hello", WORD.parse("hello"));
        assertFalse(WORD.matcher("hello world").matches());
    }

    @Test
    void testFile_singleQuotes() {
        assertTrue(FILE.matcher("'/path/to/file.txt'").matches());
        assertEquals(Path.of("/path/to/file.txt"), FILE.parse("'/path/to/file.txt'"));
    }

    @Test
    void testFile_doubleQuotes() {
        assertTrue(FILE.matcher("\"/path/to/file.txt\"").matches());
        assertEquals(Path.of("/path/to/file.txt"), FILE.parse("\"/path/to/file.txt\""));
    }

    @Test
    void testFile_noQuotes_doesNotMatch() {
        assertFalse(FILE.matcher("/path/to/file.txt").matches());
    }
}