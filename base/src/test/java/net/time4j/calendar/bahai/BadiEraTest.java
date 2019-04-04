package net.time4j.calendar.bahai;

import net.time4j.calendar.frenchrev.FrenchRepublicanEra;
import net.time4j.format.TextWidth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(JUnit4.class)
public class BadiEraTest {

    @Test
    public void values() {
        assertThat(
            BadiEra.values().length,
            is(1));
    }

    @Test
    public void eraNames() {
        assertThat(
            BadiEra.BAHAI.getDisplayName(Locale.FRENCH, TextWidth.WIDE),
            is("Ère Bahá'íe"));
        assertThat(
            BadiEra.BAHAI.getDisplayName(Locale.FRENCH, TextWidth.ABBREVIATED),
            is("E.B."));
        assertThat(
            BadiEra.BAHAI.getDisplayName(Locale.FRENCH, TextWidth.SHORT),
            is("E.B."));
        assertThat(
            BadiEra.BAHAI.getDisplayName(Locale.FRENCH, TextWidth.NARROW),
            is("EB"));
    }

}
