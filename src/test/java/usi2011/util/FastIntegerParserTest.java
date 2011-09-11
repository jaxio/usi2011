package usi2011.util;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class FastIntegerParserTest {
    @Test
    public void valid() {
        for (int i = 0; i < 1000; i++) {
            assertThat(FastIntegerParser.parseInt(i + "")).isEqualTo(i);
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullThrowsNPIE() {
        FastIntegerParser.parseInt(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyThrowsIllegalArgumentException() {
        FastIntegerParser.parseInt("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCharThrowsIllegalArgumentException() {
        FastIntegerParser.parseInt("a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void twoInvalidCharsThrowsIllegalArgumentException() {
        FastIntegerParser.parseInt("ab");
    }

    @Test(expected = IllegalArgumentException.class)
    public void plusSignThrowsIllegalArgumentException() {
        FastIntegerParser.parseInt("+");
    }

    @Test(expected = IllegalArgumentException.class)
    public void minusSignThrowsIllegalArgumentException() {
        FastIntegerParser.parseInt("-");
    }

    @Test
    public void minus() {
        assertThat(FastIntegerParser.parseInt("-1")).isEqualTo(-1);
    }
    @Test
    public void isNumber() {
        assertThat(FastIntegerParser.isNumber(null)).isEqualTo(false);
        assertThat(FastIntegerParser.isNumber("")).isEqualTo(false);
        assertThat(FastIntegerParser.isNumber("1")).isEqualTo(true);
        assertThat(FastIntegerParser.isNumber("10")).isEqualTo(true);
        assertThat(FastIntegerParser.isNumber("100")).isEqualTo(true);
        assertThat(FastIntegerParser.isNumber("-1")).isEqualTo(true);
        assertThat(FastIntegerParser.isNumber("a")).isEqualTo(false);
        assertThat(FastIntegerParser.isNumber("aa")).isEqualTo(false);
    }
}