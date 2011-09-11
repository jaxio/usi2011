package usi2011.util;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.util.SplitUtil.split;
import static usi2011.util.SplitUtil.splitKeyValue;

import org.junit.Test;

public class SplitUtilTest {
    @Test
    public void validSplitKeyValue() {
        assertThat(splitKeyValue("", ':')).isEqualTo(new String[] { "", "" });
        assertThat(splitKeyValue("a", ':')).isEqualTo(new String[] { "a", "" });
        assertThat(splitKeyValue("a:b", ':')).isEqualTo(new String[] { "a", "b" });
        assertThat(splitKeyValue(" a:b", ':')).isEqualTo(new String[] { " a", "b" });
        assertThat(splitKeyValue(" a: b", ':')).isEqualTo(new String[] { " a", " b" });
        assertThat(splitKeyValue(" a : b ", ':')).isEqualTo(new String[] { " a ", " b " });
    }

    @Test(expected = NullPointerException.class)
    public void splitNullKeyValueThrowsException() {
        splitKeyValue(null, ':');
    }

    @Test
    public void validSplit() {
        assertThat(split("", ':')).isEqualTo(new String[0]);
        assertThat(split(":", ':')).isEqualTo(new String[0]);
        assertThat(split("a", ':')).isEqualTo(new String[] { "a" });
        assertThat(split("a:b", ':')).isEqualTo(new String[] { "a", "b" });
        assertThat(split("a:b:c", ':')).isEqualTo(new String[] { "a", "b", "c" });
        assertThat(split("a:b:c:", ':')).isEqualTo(new String[] { "a", "b", "c" });
        assertThat(split(" a:b", ':')).isEqualTo(new String[] { " a", "b" });
        assertThat(split(" a: b", ':')).isEqualTo(new String[] { " a", " b" });
        assertThat(split(" a : b ", ':')).isEqualTo(new String[] { " a ", " b " });
    }

}
